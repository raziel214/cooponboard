# Dependencies Manifesto - cooponboard

This document lists the third-party libraries and frameworks used by `cooponboard`, along with their versions and licensing info.

## Internal Dependencies (Quarkus Platform)
These dependencies inherit their versions from `quarkus-bom:3.36.0` (Apache License 2.0).

- **io.quarkus:quarkus-rest**: JAX-RS implementation for REST endpoints.
- **io.quarkus:quarkus-rest-jackson**: JSON serialization support.
- **io.quarkus:quarkus-hibernate-orm-panache**: Active Record and Repository pattern implementation for Hibernate.
- **io.quarkus:quarkus-jdbc-postgresql**: PostgreSQL database driver.
- **io.quarkus:quarkus-arc**: Dependency injection container.
- **io.quarkus:quarkus-config-yaml**: Config reading via YAML (`application.yml`).
- **io.quarkus:quarkus-hibernate-orm**: JPA entity manager integration.
- **io.quarkus:quarkus-smallrye-openapi**: Swagger / OpenAPI visual specification interface.
- **io.quarkus:quarkus-oidc**: OpenID Connect adapter (Keycloak integration).
- **io.quarkus:quarkus-smallrye-health**: Health probes (`/q/health`).
- **io.quarkus:quarkus-micrometer-registry-prometheus**: Metrics endpoint for Prometheus.
- **io.quarkus:quarkus-opentelemetry**: Distributed tracing and observability.

## Third-Party & Quarkiverse Dependencies
- **io.quarkiverse.vault:quarkus-vault**: Integration with HashiCorp Vault. (Apache License 2.0)
- **io.quarkiverse.minio:quarkus-minio:3.8.6**: Integration with MinIO S3 object storage. (Apache License 2.0)

## Test Dependencies
- **io.quarkus:quarkus-junit**: Testing framework runner.
- **io.quarkus:quarkus-junit5-mockito**: Mockito integration for Quarkus tests.
- **io.quarkus:quarkus-test-security**: Security/OIDC mocking framework for tests.
- **io.rest-assured:rest-assured**: HTTP integration testing tool.
