# cooponboard

API REST construida con **Quarkus** (Java 25, Quarkus 3.36.0) que forma parte del
ecosistema de microservicios **itilsupport**. Se apoya en una infraestructura
compartida de **PostgreSQL**, **Keycloak**, **HashiCorp Vault** y **MinIO**,
todos orquestados con Docker Compose sobre una red común.

---

## Arquitectura

```
                         red docker: itilsupport_network_dev
   ┌───────────────────────────────────────────────────────────────────────┐
   │                                                                         │
   │   ┌──────────────┐        ┌──────────────────┐      ┌───────────────┐  │
   │   │  cooponboard │  OIDC  │  itilsupport_     │      │ vault-server  │  │
   │   │  (Quarkus)   ├───────►│  keycloak         │      │ (secretos)    │  │
   │   │              │        └────────┬─────────┘      └───────┬───────┘  │
   │   │   :8080      │                 │ JDBC                   │           │
   │   │              │   JDBC          ▼                        │ secretos  │
   │   │              ├──────►┌──────────────────┐               │           │
   │   │              │       │ itilsupport_     │◄──────────────┘           │
   │   │              │       │ postgres  :5432  │                           │
   │   │              │       │  · itilsupport   │                           │
   │   │              │       │  · keycloak      │                           │
   │   │              │  S3   └──────────────────┘                           │
   │   │              ├──────►┌──────────────────┐                           │
   │   └──────────────┘       │ minio-server     │                           │
   │                          │  :9000 / :9001   │                           │
   │                          └──────────────────┘                           │
   └───────────────────────────────────────────────────────────────────────┘
        (también en la red: itilsupport_backend_dev, itilsupport_frontend_dev)
```

### Componentes

| Servicio | Contenedor | Imagen | Puerto host | Rol | Compose |
|---|---|---|---|---|---|
| **cooponboard** | `cooponboard_dev` | `cooponboard:dev` (build local) | `8082` (+ `5006` debug) | API REST de negocio | `docker-compose.dev.yml` (este repo) |
| **PostgreSQL** | `itilsupport_postgres` | `postgres:16` | `5432` | BD de la app + BD de Keycloak | `itilsupport/Postgres/` |
| **Keycloak** | `itilsupport_keycloak` | `keycloak:26.0` | `8081` | Identidades / OIDC | `itilsupport/Keycloak/` |
| **Vault** | `vault-server` | `hashicorp/vault` | `8200` | Gestión de secretos | `itilsupport/Vault/` |
| **MinIO** | `minio-server` | `minio/minio` | `9000` / `9001` | Almacenamiento de objetos (S3) | `itilsupport/Minio/` |

> El backend Spring `itilsupport_backend_dev` ocupa **8080** y **5005**; por eso
> `cooponboard` usa **8082** y **5006**.

### Modelo de red

Todos los servicios comparten la red Docker externa **`itilsupport_network_dev`**.
Gracias a esto, `cooponboard` resuelve a sus dependencias **por nombre de
contenedor** (DNS interno de Docker), sin depender de `host.docker.internal`:

| Dependencia | URL interna (desde cooponboard) |
|---|---|
| PostgreSQL | `jdbc:postgresql://itilsupport_postgres:5432/itilsupport` |
| Keycloak (OIDC) | `http://itilsupport_keycloak:8080/realms/<realm>` |
| Vault | `http://vault-server:8200` |
| MinIO | `http://minio-server:9000` |

---

## Cómo levantar el stack completo (DEV)

```bash
# 0. (una sola vez) crear la red compartida si no existe
docker network create itilsupport_network_dev

# 1. Infraestructura (en el repo itilsupport)
cd itilsupport/Postgres && cp .env.example .env && docker compose up -d
cd ../Keycloak         && cp .env.example .env && docker compose up -d
cd ../Vault            && docker compose up -d   # luego: unseal con 3 de 5 keys
cd ../Minio            && docker compose up -d

# 2. Construir y levantar cooponboard (este repo)
./mvnw package                                   # genera target/quarkus-app/
docker compose -f docker-compose.dev.yml up -d --build
```

Verificación:

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
curl http://localhost:8082/q/health              # health de la API (si se añade smallrye-health)
```

URLs útiles:

- API cooponboard → <http://localhost:8082>
- Consola Keycloak → <http://localhost:8081> (admin / admin)
- Consola MinIO → <http://localhost:9001> (minioadmin / minioadmin123)
- UI de Vault → <http://localhost:8200>

### Variables de entorno

Copiar `.env.example` a `.env` y ajustar. Las claves relevantes:

- `APP_PORT` / `DEBUG_PORT` — puertos publicados al host.
- `DB_NAME` / `DB_USER` / `DB_PASSWORD` — deben coincidir con `itilsupport/Postgres/.env`.
- `DB_DDL` — estrategia de Hibernate (`update`, `validate`, `none`, …).
- `OIDC_*`, `VAULT_*`, `MINIO_*` — listas para cuando se agreguen sus extensiones (ver roadmap).

---

## Roadmap de integración

Extensiones presentes hoy: `quarkus-rest`, `quarkus-rest-jackson`,
`quarkus-hibernate-orm-panache`, `quarkus-jdbc-postgresql`.

Pendientes de agregar para completar la arquitectura (las variables ya están
preparadas y comentadas en `docker-compose.dev.yml`):

```bash
./mvnw quarkus:add-extension -Dextensions="oidc"     # Keycloak / seguridad
./mvnw quarkus:add-extension -Dextensions="vault"    # secretos
./mvnw quarkus:add-extension -Dextensions="minio"    # almacenamiento de objetos
./mvnw quarkus:add-extension -Dextensions="smallrye-health"  # /q/health
```

Al agregar cada extensión, descomentar el bloque correspondiente en
`docker-compose.dev.yml`.

---

## Desarrollo local (sin Docker)

### Modo dev (live coding)

```bash
./mvnw quarkus:dev
```

> Quarkus incluye una Dev UI disponible solo en modo dev en <http://localhost:8080/q/dev/>.

En modo dev local, configura el datasource en `src/main/resources/application.properties`
apuntando a `localhost:5432` (en vez del nombre de contenedor).

### Empaquetado

```bash
./mvnw package
```

Genera `quarkus-run.jar` en `target/quarkus-app/` (no es un _über-jar_; las
dependencias quedan en `target/quarkus-app/lib/`). Ejecutable con
`java -jar target/quarkus-app/quarkus-run.jar`.

Über-jar:

```bash
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

### Ejecutable nativo

```bash
./mvnw package -Dnative
# sin GraalVM instalado, compilando en contenedor:
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Ejecución: `./target/cooponboard-1.0.0-SNAPSHOT-runner`.

Más info: <https://quarkus.io/guides/maven-tooling>.

---

## Imágenes Docker del proyecto

Quarkus genera plantillas en `src/main/docker/`:

| Dockerfile | Uso |
|---|---|
| `Dockerfile.jvm` | JAR sobre JVM (default, el que usa `docker-compose.dev.yml`). |
| `Dockerfile.native` | Binario nativo GraalVM (arranque en ms). |
| `Dockerfile.native-micro` | Nativo sobre imagen base mínima. |
| `Dockerfile.legacy-jar` | Fat-jar clásico (compatibilidad). |

---

## Guías relacionadas

- REST: <https://quarkus.io/guides/rest>
- Hibernate ORM con Panache: <https://quarkus.io/guides/hibernate-orm-panache>
- JDBC PostgreSQL / Datasource: <https://quarkus.io/guides/datasource>
- OIDC (Keycloak): <https://quarkus.io/guides/security-oidc-bearer-token-authentication>
- Vault: <https://docs.quarkiverse.io/quarkus-vault/dev/>
- MinIO: <https://docs.quarkiverse.io/quarkus-minio/dev/>
