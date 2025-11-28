# Recognitions Service

Local Spring Boot service to monitor employee recognitions with REST and GraphQL endpoints, heuristic insights, CSV/PNG exports, and scheduled reports.

## Quick start

1. Start Postgres with Docker Compose:

```bash
docker-compose up -d
```

2. Build and run the Spring Boot app:

```bash
./gradlew bootRun
```

The app will connect to `jdbc:postgresql://localhost:5432/recognitions` with user `postgres`/`postgres` and apply Flyway migrations to create schema and seed sample data.

## Endpoints

- REST: http://localhost:8080/api/recognitions
  - GET `/api/recognitions` list (pagination params `page`,`size`)
  - GET `/api/recognitions/export.csv` download CSV
  - GET `/api/recognitions/insights` JSON insights
  - GET `/api/recognitions/graph.png` PNG chart

- GraphQL: http://localhost:8080/graphql
  - Query `recognitions`, `employees`, `recognitionTypes`

## Reports
Daily and monthly reports are written to `./reports` by default (configurable via `REPORTS_PATH` env var). Cron schedules are defined in `src/main/resources/application.yml`.

## Notes
- IDs: integer `id` for PK and FKs; `uuid` column present for external references.
- Timezone: all datetimes stored/processed in UTC.


