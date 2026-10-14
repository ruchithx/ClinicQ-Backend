# DMS Backend

Spring Boot 4.1 (Java 21) backend for the Dispensary Management System. Implements the contract in
`../docs/openapi.yaml` and the schema in `../docs/sql/V1__initial_schema.sql`.

## Build & test

```bash
# compile
./mvnw -B -DskipTests compile

# unit tests only (no Docker required)
./mvnw test

# full build incl. integration tests (requires a running Docker daemon for Testcontainers)
./mvnw clean verify

# bootable jar
./mvnw -DskipTests package   # -> target/dms-backend.jar
```

## Run locally

Start a PostgreSQL 16 database (from the repo root):

```bash
docker compose up -d db
```

Then run the backend against it (dev profile loads the seed data and enables Swagger):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/dms
export DB_USERNAME=dms
export DB_PASSWORD=dms_dev_password
export APP_JWT_SECRET=ZGV2LW9ubHktc2VjcmV0LWNoYW5nZS1tZS0wMTIzNDU2Nzg5YWJjZGVm
export APP_REFRESH_COOKIE_SECURE=false
export APP_SWAGGER_ENABLED=true
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Or the whole stack:

```bash
cd .. && cp .env.example .env && docker compose up --build
```

- Swagger UI (dev): <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>, `/actuator/health/liveness`, `/actuator/health/readiness`
- Dev login: `dr.perera` / `DevPassword#2026` via `POST /api/v1/auth/login`

## Required environment variables

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_JWT_SECRET` (Base64, >= 32 bytes after decoding).
All others have defaults — see `../docs/backend-requirements.md` section 4.3. Production needs
`APP_REFRESH_COOKIE_SECURE=true` (default) and the bootstrap-admin variables for the first start.

## Layout

`com.dispensary.dms` — `config`, `security`, `common` (error/web/time/idempotency/validation),
`auth`, `user`, `patient`, `medicine`, `consultation`, `prescription`, `pharmacy`, `queue`,
`audit`, `retention`. One module per domain; controllers delegate to `*Service` classes which own
transactions, business rules and the state machines.
