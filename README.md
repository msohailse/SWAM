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
deploy/k8s/               Kubernetes manifests for the same stack (Minikube), + HPA on backend
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

## Running on Minikube (K8s + HPA)

The same stack, deployed to a real local Kubernetes cluster, with a CPU-based
`HorizontalPodAutoscaler` on the backend.

```bash
minikube start
minikube addons enable metrics-server   # required for HPA to read CPU usage

# Build the three images straight into minikube's own Docker daemon (no registry needed)
eval $(minikube docker-env)
docker build -t swam-backend:local  -f backend/src/main/docker/Dockerfile.jvm backend
docker build -t swam-analyzer:local -f backend/analyzer-service/src/main/docker/Dockerfile.jvm backend/analyzer-service
docker build -t swam-frontend:local frontend

# Apply everything (creates its own "swam" namespace — see note below)
kubectl apply -f deploy/k8s/
```

Reach the app:
```bash
kubectl port-forward -n swam svc/frontend 8081:80
# open http://localhost:8081
```

Watch the HPA:
```bash
kubectl get hpa -n swam -w
```

**Important — always deploy into a dedicated namespace.** All manifests in `deploy/k8s/`
are pinned to `namespace: swam` (see `00-namespace.yaml`). Don't remove that if you copy
these files elsewhere: on a shared/multi-project minikube cluster, generic resource names
like `frontend`/`backend` can collide with another project's identically-named
Deployment/Service in the `default` namespace and silently overwrite it on `kubectl
apply`. This happened once during development against a shared cluster that already had
an unrelated demo app's `frontend` Deployment/Service — always double check with `kubectl
get all -A` before applying unnamespaced manifests to a cluster you don't fully control.

**Kafka needs a headless Service.** `deploy/k8s/02-kafka.yaml`'s Service is
`clusterIP: None`. A single-broker KRaft node connects to itself (`kafka:9093`, its own
controller/voter) at startup; through a normal ClusterIP that's a hairpin NAT back to the
same pod, which doesn't reliably work on every cluster network setup and crash-loops the
broker. Headless makes the "kafka" DNS name resolve straight to the pod IP, side-stepping
the hairpin entirely.

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
