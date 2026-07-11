# SWAM — Scalable Event-Driven Incident Management System

REST API (Quarkus) + Angular UI for reporting and
tracking incidents, with an async Kafka-based duplicate-detection service on the side.
See `CLAUDE.md` for the full architecture/decision log.

```
backend/                   Quarkus REST API (Incident/Tag/User/Comment CRUD)
backend/analyzer-service/  Separate Quarkus project — Kafka consumer, duplicate detection
frontend/                  Angular 19 standalone SPA
deploy/docker-compose.yml  Whole stack: postgres + kafka + backend + analyzer-service + frontend
deploy/k8s/                Kubernetes manifests for the same stack (Minikube), + HPA on backend
```

## Prerequisites

Docker 24+ and Docker Compose v2+ (tested with Docker 29 / Compose v5). That's all you
need for the quick start below.

## Quick start

```bash
cd deploy
cp .env.example .env   # first time only
docker compose up -d --build
```

Open **http://localhost:4200** for the app. Postgres is on host port **5433** (not 5432).

Stop with `docker compose down` (add `-v` to also wipe the Postgres volume).

## API quick reference

- `POST /users/register`, `POST /users/login`
- `GET /incidents` — supports optional filters: `?tag=&severity=<LOW|MEDIUM|HIGH>&status=<open|closed>`
- `GET /incidents/{id}`, `GET /incidents/user/{userId}`
- `POST /incidents`, `PUT /incidents/{id}`, `DELETE /incidents/{id}`
- `PATCH /incidents/{id}/close` — admin-only, requires a closing comment
- `GET /incidents/{id}/comments`, `POST /incidents/{id}/comments` — comment thread (either role)
- `GET /tags`, `POST /tags`, `PUT /tags/{id}`, `DELETE /tags/{id}`

## Running services individually (dev mode, no Compose)

```bash
cd backend && mvn quarkus:dev                     # API on :8080, Dev Services auto-starts Postgres+Kafka
cd backend/analyzer-service && mvn quarkus:dev    # dedup consumer
cd frontend && npm install && npx ng serve        # UI on :4200, proxied to :8080
```

## Running on Minikube (K8s + HPA)

```bash
minikube start
minikube addons enable metrics-server

eval $(minikube docker-env)
docker build -t swam-backend:local  -f backend/src/main/docker/Dockerfile.jvm backend
docker build -t swam-analyzer:local -f backend/analyzer-service/src/main/docker/Dockerfile.jvm backend/analyzer-service
docker build -t swam-frontend:local frontend

kubectl apply -f deploy/k8s/
kubectl port-forward -n swam svc/frontend 8081:80   # open http://localhost:8081
kubectl get hpa -n swam -w                          # watch it scale
```

Everything deploys into its own `swam` namespace — see `CLAUDE.md` §7 Day 3 for why that
matters on a shared cluster, and for the headless-Service fix Kafka needed under Minikube.

## Running tests

```bash
cd backend && mvn test          # needs Docker running (Testcontainers/Dev Services)
cd backend/analyzer-service && mvn test
```

## Duplicate detection

Every new incident is published to Kafka (`incident-created` topic). `analyzer-service`
consumes it and runs a Postgres `pg_trgm` similarity check (threshold 0.4) against other
open incidents; a likely duplicate gets a system-authored comment flagging it. This runs
fully asynchronously — `POST /incidents` itself stays synchronous and unaffected.
