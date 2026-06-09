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
   │   │   :8090      │                 │ JDBC                   │           │
   │   │              │   JDBC          ▼                        │ secretos  │
   │   │              ├──────►┌──────────────────┐               │           │
   │   │              │       │ shared_postgres  │◄──────────────┘           │
   │   │              │       │           :5432  │                           │
   │   │              │       │  · cooponboard   │                           │
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
| **cooponboard** | `cooponboard_dev` | `cooponboard:dev` (build local) | `8090` (+ `5006` debug) | API REST de negocio | `docker-compose.dev.yml` (este repo) |
| **PostgreSQL** | `shared_postgres` | `postgres:16` | `5432` | Servidor compartido: BD `cooponboard` + BD `keycloak` | `itilsupport/Postgres/` |
| **Keycloak** | `itilsupport_keycloak` | `keycloak:26.0` | `8081` | Identidades / OIDC | `itilsupport/Keycloak/` |
| **Vault** | `vault-server` | `hashicorp/vault` | `8200` | Gestión de secretos | `itilsupport/Vault/` |
| **MinIO** | `minio-server` | `minio/minio` | `9000` / `9001` | Almacenamiento de objetos (S3) | `itilsupport/Minio/` |

> El backend Spring ocupa **8080**/**5005** y Keycloak **8081**; por eso
> `cooponboard` usa **8090** y **5006**.

### Componentes externos (de terceros)

`cooponboard` **no reimplementa** infraestructura: se **integra** con software de
terceros que corre como servicios independientes (contenedores propios). Este
proyecto solo aporta el código de la API; cada componente externo conserva su
**propia licencia** y no queda cubierto por la licencia MIT de este repositorio.

| Componente | Para qué lo usamos | Proveedor | Licencia del componente |
|---|---|---|---|
| **PostgreSQL** | Persistencia relacional de la API (y BD de Keycloak) | PostgreSQL Global Development Group | PostgreSQL License (tipo BSD) |
| **Keycloak** | Gestión de identidades, autenticación y autorización (OIDC/OAuth2) | Red Hat / CNCF | Apache License 2.0 |
| **HashiCorp Vault** | Gestión y entrega segura de secretos (credenciales, claves) | HashiCorp | Business Source License 1.1 |
| **MinIO** | Almacenamiento de objetos compatible con S3 | MinIO Inc. | GNU AGPL v3 |

> **Importante para uso público:** la licencia MIT de este repo aplica **solo al
> código de `cooponboard`**. Si distribuyes o despliegas el stack completo, revisa
> y respeta las licencias de cada componente externo (en particular MinIO bajo
> **AGPL v3** y Vault bajo **BSL 1.1**, que imponen condiciones específicas).

### Modelo de red

Todos los servicios comparten la red Docker externa **`itilsupport_network_dev`**.
Gracias a esto, `cooponboard` resuelve a sus dependencias **por nombre de
contenedor** (DNS interno de Docker), sin depender de `host.docker.internal`:

| Dependencia | URL interna (desde cooponboard) |
|---|---|
| PostgreSQL | `jdbc:postgresql://shared_postgres:5432/cooponboard` |
| Keycloak (OIDC) | `http://itilsupport_keycloak:8080/realms/internos-dev` |
| Vault | `http://vault-server:8200` |
| MinIO | `http://minio-server:9000` |

---

## Estructura del proyecto

```
cooponboard/
├── pom.xml                     # Maven + BOM de Quarkus, dependencias y plugins
├── mvnw / mvnw.cmd             # Maven Wrapper (no requiere Maven instalado)
├── .mvn/                       # Configuración del wrapper
│
├── docker-compose.dev.yml      # Orquestación DEV (servicio cooponboard + red compartida)
├── .env.example                # Plantilla de variables de entorno (copiar a .env)
├── .dockerignore               # Acota el contexto de build de la imagen
├── .gitignore                  # Exclusiones de control de versiones
├── LICENSE                     # Licencia MIT
├── README.md                   # Este documento
│
└── src/
    ├── main/
    │   ├── java/com/quimbaya/cooponboard/
    │   │   ├── GreetingResource.java   # Endpoint REST de ejemplo (JAX-RS)
    │   │   └── MyEntity.java           # Entidad JPA/Panache de ejemplo
    │   │
    │   ├── resources/
    │   │   ├── application.yml          # Configuración de la app (datasource, perfiles)
    │   │   └── import.sql               # SQL de carga inicial (modo dev/test)
    │   │
    │   └── docker/                      # Plantillas de imagen generadas por Quarkus
    │       ├── Dockerfile.jvm           # JAR sobre JVM (default, usado por el compose)
    │       ├── Dockerfile.native        # Binario nativo GraalVM
    │       ├── Dockerfile.native-micro  # Nativo sobre imagen base mínima
    │       └── Dockerfile.legacy-jar    # Fat-jar clásico (compatibilidad)
    │
    └── test/
        └── java/com/quimbaya/cooponboard/
            ├── GreetingResourceTest.java   # Test unitario/funcional (@QuarkusTest)
            └── GreetingResourceIT.java     # Test de integración (@QuarkusIntegrationTest)
```

### Convenciones

- **Paquete base:** `com.quimbaya.cooponboard`. Organiza por capas o por feature dentro de él
  (p. ej. `.../domain`, `.../rest`, `.../service`, `.../repository`).
- **Recursos REST** terminan en `Resource` (estilo Quarkus/JAX-RS).
- **Entidades** usan Hibernate ORM con Panache.
- **Tests:** `*Test` corren en JVM (`@QuarkusTest`); `*IT` son de integración
  (`@QuarkusIntegrationTest`) y se ejecutan en la fase `verify`.
- **Configuración** centralizada en `application.yml` (extensión `quarkus-config-yaml`); los valores sensibles
  y específicos de entorno se inyectan por variables de entorno (ver `.env.example`).

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
curl http://localhost:8090/q/health              # health de la API (si se añade smallrye-health)
```

URLs útiles:

- API cooponboard → <http://localhost:8090> (Swagger UI: <http://localhost:8090/q/swagger-ui>)
- Consola Keycloak → <http://localhost:8081> (admin / admin)
- Consola MinIO → <http://localhost:9001> (minioadmin / minioadmin123)
- UI de Vault → <http://localhost:8200>

### Variables de entorno

Copiar `.env.example` a `.env` y ajustar. Las claves relevantes:

- `APP_PORT` / `DEBUG_PORT` — puertos publicados al host.
- `DB_NAME` / `DB_DDL` — nombre de la BD y estrategia de Hibernate. **Las credenciales de la BD NO van aquí: viven en Vault.**
- `VAULT_USERNAME` / `VAULT_PASSWORD` — credencial del usuario `cooponboard` (solo-lectura) con que la app se autentica en Vault. `VAULT_PASSWORD` es obligatorio y **no se versiona**.
- `OIDC_REALM` / `OIDC_CLIENT_ID` — realm y client de Keycloak.

---

## Gestión de secretos (Vault)

Las **credenciales de PostgreSQL y MinIO no se guardan** en `application.yml`,
`docker-compose.dev.yml` ni `.env`. Se obtienen en runtime desde **Vault**:

| Ítem | Valor |
|---|---|
| Path KV (v2) | `secret/dev/cooponboard/app` |
| Claves | `quarkus.datasource.username/password`, `quarkus.minio.access-key/secret-key` |
| Política | `cooponboard-reader` (solo lectura sobre `dev/cooponboard/*`) |
| Usuario app | `cooponboard` (auth `userpass`) |

La app se autentica en Vault con `VAULT_USERNAME`/`VAULT_PASSWORD` y Quarkus
expone esas claves del KV como configuración (extensión `quarkus-vault`,
`quarkus.vault.secret-config-kv-path`), resolviendo `quarkus.datasource.*` y
`quarkus.minio.*` directamente desde Vault.

> Para rotar credenciales basta con actualizar el secreto en Vault; no hay que
> tocar el código ni reconstruir la imagen.

## Seguridad (Keycloak / OIDC)

La API valida tokens **JWT Bearer** emitidos por Keycloak (`quarkus-oidc` en modo
`service`).

**Modelo de realms (enterprise):** el realm es una **frontera de seguridad por
ambiente + población**, no por aplicación. Aquí:

- **Realm `internos-dev`** = población *internos* (empleados) en el ambiente *dev*.
  Se replica como `internos-qa` / `internos-prod` (idealmente en instancias de
  Keycloak separadas).
- `cooponboard` aporta **clients** dentro del realm, no un realm propio:
  - `cooponboard-api` → resource server / audience (`aud=cooponboard-api`).
  - `cooponboard-spa` → front (público + PKCE).
- Si en el futuro hay clientes **B2C**, van en un realm aparte (`customers-*`),
  nunca mezclados con los internos.

Pasos pendientes en Keycloak (realm `internos-dev`):

1. Crear el client `cooponboard-api` (resource server) y `cooponboard-spa` (público+PKCE).
2. Definir client roles (`customer:read/write/admin`) y scopes (`customers:read`, …).
3. Agregar un **Audience mapper** que incluya `cooponboard-api` en el `aud`.
4. Proteger endpoints con `@RolesAllowed` / `@Authenticated` / `@ScopesAllowed`.

Mientras los clients no existan, la API arranca pero las rutas protegidas
rechazarán las peticiones.

---

## Desarrollo local (sin Docker)

### Modo dev (live coding)

```bash
./mvnw quarkus:dev
```

> Quarkus incluye una Dev UI disponible solo en modo dev en <http://localhost:8090/q/dev/>.

En modo dev local, el datasource ya apunta a `localhost:5432` mediante el perfil
`%dev` en `src/main/resources/application.yml` (en el contenedor, el compose lo
sobreescribe con el nombre de contenedor `shared_postgres`).

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

---

## Autor

**John Fredy Quimbaya Orozco**
Magíster en Gerencia de Tecnologías de Información · Arquitecto de Soluciones · Líder Técnico

Más de 9 años diseñando e implementando soluciones tecnológicas en los sectores
financiero y de salud, con foco en arquitectura de microservicios, seguridad
(OAuth2/OIDC), nube (AWS) y modernización de plataformas.

- LinkedIn: [John Fredy Quimbaya Orozco](https://www.linkedin.com/in/jfqo/)
- Email: 94041671@u.icesi.edu.co

## Licencia

Distribuido bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE) para el texto completo.

Copyright (c) 2026 John Fredy Quimbaya Orozco

> La licencia MIT cubre únicamente el código fuente de `cooponboard`. Los
> componentes externos (PostgreSQL, Keycloak, Vault, MinIO) se rigen por sus
> respectivas licencias — ver [Componentes externos](#componentes-externos-de-terceros).
