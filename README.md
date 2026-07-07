# SWAM — Scalable Event-Driven Incident Management System

University exam project (SWAM course). REST API (Quarkus) + Angular UI for reporting and
tracking incidents, with an async Kafka-based duplicate-detection service on the side.
See `CLAUDE.md` for the full architecture/decision log.

## Project layout

```
backend/                 Quarkus REST API (Incident/Tag/User/Comment CRUD)
backend/analyzer-service/  Separate Quarkus project — Kafka consumer, duplicate detection
frontend/                Angular 19 standalone SPA
deploy/docker-compose.yml  Whole stack: postgres + kafka + backend + analyzer-service + frontend
```

## Prerequisites

- Docker Desktop (running)
- Java 17 + Maven (only needed if you want to run `backend/` outside Docker)
- Node 18+ (only needed if you want to run `frontend/` outside Docker)

## Quickest way to run everything (Docker Compose)

```bash
cd deploy
cp .env.example .env        # first time only — edit DB_USER/DB_PASSWORD/DB_NAME if you like
docker compose up --build
```

Then open **http://localhost:4200** — that's the Angular app, which proxies API calls to
the backend over the internal Docker network.

Postgres is reachable from the host on port **5433** (not 5432 — kept free for another
project on this machine).

To stop everything: `docker compose down` (add `-v` to also wipe the Postgres volume).

To see the backend scale (no fixed host port, multiple replicas share load):
```bash
docker compose up --scale backend=3
```

## Running services individually (for development)

**Backend** (needs Docker running — Quarkus Dev Services auto-starts Postgres+Kafka):
```bash
cd backend
mvn quarkus:dev
```
API available at http://localhost:8080.

**Analyzer service** (same Dev Services trick, separate Quarkus project):
```bash
cd backend/analyzer-service
mvn quarkus:dev
```

**Frontend**:
```bash
cd frontend
npm install
npx ng serve
```
App available at http://localhost:4200, proxied to `localhost:8080` via `proxy.conf.json`.

## API quick reference

- `POST /users/register`, `POST /users/login`
- `GET /incidents` — all incidents; supports optional filters:
  `GET /incidents?tag=<tagTitle>&severity=<LOW|MEDIUM|HIGH>&status=<open|closed>`
- `GET /incidents/{id}`, `GET /incidents/user/{userId}`
- `POST /incidents`, `PUT /incidents/{id}`, `DELETE /incidents/{id}`
- `PATCH /incidents/{id}/close` — admin-only, requires a closing comment
- `GET /incidents/{id}/comments`, `POST /incidents/{id}/comments` — comment thread (either role)
- `GET /tags`, `POST /tags`, `PUT /tags/{id}`, `DELETE /tags/{id}`

## Running tests

```bash
cd backend && mvn test          # needs Docker running (Testcontainers/Dev Services)
cd backend/analyzer-service && mvn test
```

## Duplicate detection

Every new incident is published to Kafka (`incident-created` topic). `analyzer-service`
consumes it and runs a Postgres `pg_trgm` similarity check (threshold 0.4) against other
open incidents; a likely duplicate gets a system-authored comment on the incident
flagging it. This runs fully asynchronously — `POST /incidents` itself stays synchronous
and unaffected.
