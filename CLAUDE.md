# CLAUDE.md — SWAM Project: Scalable Event-Driven Incident Management System

> University exam project (SWAM, Prof. Vicario's course). The graded deliverables are a
> **technical report** and a **~20 minute presentation**. The professor confirmed a
> **prototype with limited functionality suffices** — we optimize for architectural
> quality and report material, not production completeness.
>
> This file is the single source of truth for scope, decisions, and progress.
> Full original proposal + professor's reply: [ProjectIdea.md](ProjectIdea.md).

---

## 0. Status as of 2026-07-07 (read this first, it supersedes stale detail below)

**Built and verified working end-to-end, in containers:**
- `backend/` — the user's existing `incident-reporting-system` repo, migrated from a
  plain Java 8/Swing/Hibernate desktop app into a **Quarkus 3.37.1 REST API** (Java 17).
  Swing UI and the hand-rolled `TransactionManager` are gone; CDI `@Transactional`
  services replace them. Full CRUD (create/read/**update**/**delete**) on `Incident` and
  `Tag`, register/login on `User`. New `Comment` entity (one-to-many off `Incident`) —
  **a real conversation thread, not just a close-time note**: an admin closing an
  incident must attach a comment (`PATCH /incidents/{id}/close`), and *either* the
  reporter *or* an admin can keep posting replies afterward
  (`POST /incidents/{id}/comments`, `GET /incidents/{id}/comments` for the full thread).
  Only the closing action itself is admin-gated; replying is not. `UserType { REPORTER,
  ADMIN }` stub role (no JWT/real auth).
- `frontend/` — new Angular 19 standalone app: login/register, an incidents screen whose
  data source and actions are role-driven (reporters see/manage their own; admins see
  everyone's + a Close-with-comment action), a per-incident expandable comment thread
  with a reply box (both roles can post), an admin-only tags CRUD screen gated by a
  functional route guard (`adminGuard`).
- `deploy/docker-compose.yml` — postgres + kafka + backend + analyzer-service +
  frontend(nginx), env-var credentials only (`.env`, gitignored), backend verified to
  scale (`docker compose up --scale backend=3`) since it has no fixed host port.
- **Kafka + `backend/analyzer-service/` — built and verified 2026-07-07.** `POST
  /incidents` stays exactly as it was (synchronous, returns the created incident
  immediately — this did NOT become the `202`/async flow described in §1); after saving,
  `IncidentService.create()` publishes a JSON event on the `incident-created` topic via a
  new `IncidentEventPublisherPort`/`KafkaIncidentEventPublisher` adapter. A **separate**
  Quarkus project, `backend/analyzer-service` (own `pom.xml`, own Docker image, zero REST
  endpoints, zero shared Java code with `backend/` — only the DB schema and the Kafka
  topic connect them), consumes that topic and runs **Stage-1 dedup only** (`pg_trgm`
  `similarity()` via a native query, threshold 0.4, against other open incidents,
  excluding itself). On a hit, it writes a system-authored `Comment` ("Possible duplicate
  of incident #X") directly to Postgres using its own minimal JPA entities. Verified live:
  two near-identical incidents → the second gets flagged; a genuinely unrelated third
  incident does not (no false positive). The pg_trgm extension is created by the analyzer
  itself at startup (`CREATE EXTENSION IF NOT EXISTS pg_trgm`), since `backend/`'s own
  schema generation only knows about its `@Entity` classes.
- **CQRS-lite — built and verified 2026-07-07.** `GET /incidents` now accepts optional
  `?tag=&severity=&status=` query params (`IncidentResource.findAll`,
  `IncidentService.findFiltered`, `IncidentRepositoryPort.findFiltered` +
  `IncidentPostgresRepository`'s dynamic-JPQL implementation). This is the "bare minimum"
  version of the CQRS pattern named in `ProjectIdea.md` §2 (praised by the professor) —
  it separates the read side's query flexibility from the write path, without a separate
  read-model/projection table or event sourcing (that stays out of scope, see D3). Plain
  `GET /incidents` with no params still returns everything, unfiltered, exactly as
  before. Verified live via curl: filter by tag alone, by severity alone, by status
  alone, a combined filter with no matches, and an invalid `status` value returning a
  clean `400`.
- **Top-level `README.md` added** — setup/run steps (Docker Compose quick-start, running
  each service individually for dev, API quick reference, test commands).
- **Minikube/K8s + HPA — built and verified 2026-07-07.** `deploy/k8s/` deploys the full
  stack (postgres, kafka, backend, analyzer-service, frontend) to a real local Kubernetes
  cluster in its own `swam` namespace, with a plain CPU-based `HorizontalPodAutoscaler`
  (D6) on the backend. Verified live end-to-end (register → create incidents → dedup
  flagged, same as Docker Compose) and verified the HPA itself: a synthetic load
  generator drove backend CPU from 6% to 127% of the 50% target, and `kubectl get hpa -w`
  showed replicas climb 1 → 3 → 5 (max), then scale back down once load stopped. Full
  details, including a naming-collision incident on a shared cluster and a Kafka headless-
  Service fix, in §7 Day 3.
- **Lightweight load comparison — built and verified 2026-07-07.** 1500 requests at
  concurrency 100 against `GET /incidents`, backend at 1 replica vs. 5 replicas: p95
  latency 85.4ms → 17.8ms (~4.8x), mean 35.6ms → 8.1ms (~4.4x). Chart at
  `docs/diagrams/scaling-latency-comparison.png`; full writeup in
  `report/technical-report.md` §10.3 and its own presentation slide.
- **Report + presentation — built 2026-07-07.** `report/technical-report.md` (+ PDF)
  covers all 12 items from the professor's checklist in `ProjectIdea.md`; 18-slide
  `report/SWAM-Presentation.pptx` (+ PDF) built on the user-provided reference deck's own
  template/theme. Mermaid diagram sources + renders in `docs/diagrams/`, live UI
  screenshots (captured via headless Playwright, not mockups) in `docs/screenshots/`.
- **Everything not required by `ProjectIdea.md`/the professor's checklist has been
  deliberately dropped from remaining scope** (not silently forgotten) — see §7 Day 1-3
  for the specific items and why each was dropped: an ACID rollback test, an automated
  dedup test, dedup threshold tuning against a real pair set, and a formal k6 script all
  fell to this rule, since testing is confirmed not a professor requirement (§8) and
  none of these were named in the proposal.
- **Genuinely still open, deliberately deferred, not dropped**: Stage-2 semantic dedup
  (pgvector/embeddings — parked on its own branch, see §3/§11 of the report), the
  transactional outbox pattern, real JWT auth, and the async `202`/tracking flow from
  the original proposal (replaced by a documented synchronous pivot, see above). These
  are true future-developments items, listed as such in the report, not TODOs.

**Decisions revised from earlier in the file (details in §4/§8):**
- Hexagonal variant is **Pragmatic**, not Strict, for this codebase — domain entities
  keep their `@Entity` annotations (they were already there, already tested; rewriting
  them pure would've been throwaway work). §9's "domain/ must stay framework-free" rule
  is **no longer accurate** — don't enforce it literally, the entities are the domain.
- **Testing is not a professor requirement** (checked `ProjectIdea.md` — zero mentions).
  Keep tests only as long as they pass; don't invest further time perfecting them.
- Postgres in `deploy/docker-compose.yml` uses **host port 5433, not 5432** — 5432 is
  already used by another project on this machine.
- Incident↔Tag is `@ManyToOne` (one tag per incident), not the many-to-many
  `ProjectIdea.md` describes — a deliberate deviation, kept from the existing codebase.
  Document this explicitly in the report; the ACID showcase now targets Incident+Comment
  transactional integrity instead of a join-table scenario.

---

## 1. The Idea (condensed)

Users report incidents through a REST API. The API does **not** process reports
synchronously — it returns `202 Accepted` with a tracking ID and publishes the report
to **Kafka**. An **Analyzer Service** consumes reports, decides whether each one is a
**duplicate of an existing case** (this is the intelligent part — see §3), and either
assigns a new Case Number or links the report to the existing case. Users poll a status
endpoint to retrieve their Case Number. An Angular SPA sits on top.

**Domain entities:** `Incident` (title, description, severity, resolvedAt), `User`,
`Tag` — with an ACID-critical many-to-many between Incident and Tag.

**Architectural styles (the exam showcase):**
- **Hexagonal Architecture** — domain core isolated behind ports/adapters (professor called this "rare" and valuable — make it visibly clean)
- **Event-Driven Architecture** — Kafka decouples ingestion from analysis
- **CQRS (lightweight)** — separate write path (ingestion) from read models (status + analytics queries)

---

## 2. Architecture Overview

```
                ┌─────────────┐
                │  Angular SPA │
                └──────┬──────┘
                       │ REST
        ┌──────────────▼──────────────┐
        │        API Service          │  Quarkus (Jakarta REST/CDI/JPA)
        │  POST /reports → 202 + id   │
        │  GET  /reports/{id}/status  │──── read models ────┐
        │  GET  /incidents, /tags ... │                     │
        └──────┬──────────────────────┘                     │
               │ transactional outbox → Kafka topic         │
        ┌──────▼──────────────────────┐              ┌──────▼──────┐
        │      Analyzer Service       │              │ PostgreSQL  │
        │  consume → dedup analysis   │──────────────│ + pgvector  │
        │  stage 1: fuzzy (pg_trgm)   │   projections│ + pg_trgm   │
        │  stage 2: semantic (embed.) │              └─────────────┘
        │  assign / link Case Number  │
        └─────────────────────────────┘
     Both services: hexagonal (domain / ports / adapters)
     Deploy: Docker → Kubernetes (kind) → KEDA scales Analyzer on Kafka lag
```

Two deployable backend services in one monorepo, plus the Angular app. Each backend
service follows hexagonal layering internally:

```
service/
  domain/          # entities, value objects, domain services — NO framework imports
  application/     # use cases / ports (interfaces in + out)
  adapters/
    in/rest/       # JAX-RS resources
    in/messaging/  # Kafka consumers
    out/persistence/  # JPA repositories implementing ports
    out/messaging/    # Kafka producers implementing ports
```

---

## 3. The Interesting Complication: Two-Stage Deduplication (RAG-lite)

This is the "intelligent event processing" from the proposal, and the most novel report
chapter. When the Analyzer consumes a report:

1. **Stage 1 — cheap lexical match:** Postgres `pg_trgm` trigram similarity on
   title+description against open cases. Above a high threshold → duplicate, done.
2. **Stage 2 — semantic match:** compute an **embedding vector** of the report text with
   an in-process ONNX model (`langchain4j-embeddings-all-minilm-l6-v2` — pure Java, runs
   offline, no API keys, no cost), then do a **cosine similarity search in pgvector**
   against embeddings of existing open cases. Above threshold → duplicate; otherwise →
   new Case Number, store the new embedding.

Why this design and not a full LLM/RAG pipeline: no external dependencies, deterministic
enough to test, fast enough to load-test, and it still demonstrates the exact core
mechanic of RAG (embed → vector search → retrieve nearest neighbors). If motivated later,
a stretch goal upgrades Stage 2 into true RAG (retrieve top-k similar cases and ask an
LLM "is this the same incident?").

**📚 Learning references (embeddings / vector search / RAG):**
- What embeddings are and why they work: https://simonwillison.net/2023/Oct/23/embeddings/
- pgvector README (install, operators, indexes): https://github.com/pgvector/pgvector
- langchain4j in-process embedding models: https://docs.langchain4j.dev/category/embedding-models + https://docs.langchain4j.dev/integrations/embedding-stores/pgvector
- pg_trgm docs (Stage 1): https://www.postgresql.org/docs/current/pgtrgm.html
- RAG concept overview (for the stretch goal + report "future developments"): https://docs.langchain4j.dev/tutorials/rag

---

## 4. Key Decisions (made autonomously — verify and amend)

| # | Decision | Rationale | Status |
|---|----------|-----------|--------|
| D1 | **Quarkus** as Jakarta EE runtime | Implements Jakarta REST/CDI/JPA + MicroProfile; first-class Kafka (SmallRye Reactive Messaging), Dev Services (auto-starts Postgres/Kafka in dev), tiny containers for K8s | ✅ **verified 2026-07-06**: course's own module IIB (2026-04-21 lecture, Dr. Scommegna) is literally "Quarkus in the development of microservices orchestrated by Kubernetes" (paired with Istio/K6/Grafana/OpenTelemetry per `course_content` forum announcement). No WildFly/Payara/GlassFish named anywhere in the export. |
| D1b | **Pragmatic Hexagonal (JPA-in-domain)** — domain entities keep `@Entity` directly, no separate mapping layer | Reversed 2026-07-07 from the earlier Strict default: the migrated `backend/` codebase's entities were already JPA-annotated and already tested; rewriting them pure would be throwaway work for no report benefit, since the course's own slides (`SWAM26-19_HexagonalAndCleanArchitectures.pdf`) explicitly sanction this variant too. Ports (`application/port/out/`) still isolate persistence behind interfaces — only the "zero framework imports in domain" purity rule is relaxed | ✅ implemented 2026-07-07 |
| D2 | **Two services** (API + Analyzer), one monorepo, no shared Java code between them (only the DB schema + Kafka topic connect them) | Enough to demonstrate EDA + independent scaling; more services = scope creep. No shared library because that would couple two "independent" services at the code level — each owns its own minimal model of the shared tables | ✅ implemented 2026-07-07: `backend/` (api-service) + `backend/analyzer-service/`, verified live via Docker |
| D3 | **Lightweight CQRS, no event sourcing** | Separate read models (JPA projections / views) updated from events; event sourcing would double the project size | proposed, not started — current reads are direct queries against the write-side tables |
| D4 | **Transactional outbox pattern** for publishing to Kafka | Incident+Tags+outbox row commit in ONE transaction → this *is* the "strict ACID" requirement from the proposal, and a strong report section. A simple poller relays outbox → Kafka | **cut for time** (see §7 Day-1 cuts) — publish happens directly in `IncidentService.create()` after `save()`, not via an outbox row. Document the dual-write risk this reintroduces as "future developments." |
| D5 | **Dedup = pg_trgm** implemented; **+ pgvector embeddings** deferred (§3) | Intelligent, self-contained, testable, free | ✅ Stage-1 (pg_trgm) implemented + verified 2026-07-07 in `analyzer-service`'s `DuplicateDetector`; Stage-2 (embeddings) still not started |
| D6 | **Plain Kubernetes HPA (CPU-based)**, not KEDA | User already knows K8s and wants a real deployment within the 4-day window; plain HPA is exactly what the course's own Kubernetes lecture teaches (control loop, desired-replica formula, stabilization window) — safer to actually deploy correctly than KEDA-on-Kafka-lag, which isn't covered in any course material we found | ✅ decided 2026-07-06, actually deployed (not manifests-only) |
| D7 | **Minikube** for local K8s | User's preference — already knows Kubernetes; same role `kind` would have played (single-node cluster on the laptop), swapped for the tool they're fluent in. Real deployment, not "designed, not deployed" | ✅ decided 2026-07-06 |
| D8 | **k6** for synthetic load + measurements | Scriptable, produces CSV/JSON for report charts | proposed |
| D9 | **Testcontainers** for integration tests | Real Postgres+Kafka in tests, no mock drift | proposed |
| D10 | **Kafka in KRaft mode, single broker** | No ZooKeeper, one container; clustering is out of scope | proposed |
| D11 | Angular app kept to **3 screens** (see §5) | Frontend is a report requirement, not the showcase | proposed |

---

## 5. Scope: MVP vs. Stretch (YAGNI enforced)

### MVP (this is the whole prototype — resist adding more)
- `POST /reports` → validates, persists report + outbox row transactionally, returns `202` + tracking UUID
- Outbox relay publishes to one Kafka topic `incident-reports`
- Analyzer consumes, runs 2-stage dedup, assigns/links Case Number, updates read model
- `GET /reports/{id}` → status + Case Number; `GET /cases`, `GET /cases/{id}` → read side
- Incident↔Tag many-to-many with single-transaction creation (ACID showcase)
- Angular: (1) submit report form, (2) "track my report" status page with polling, (3) cases dashboard with tag filter
- Docker Compose for dev; K8s manifests + KEDA for the scaling demo
- One k6 scenario: traffic spike → watch Analyzer replicas scale 1→N → measure lag drain + latency

### Stretch (only if time remains; each is one report paragraph either way)
- Dead-letter topic for poison messages
- True RAG dedup confirmation via LLM (langchain4j + any provider)
- Server-Sent Events instead of polling for status
- Grafana dashboard for the scaling demo (looks great in the presentation)
- Keycloak / JWT auth (otherwise: `X-User-Id` header stub — document as future work)

### Explicitly OUT of scope (write in report as "future developments")
- Multi-broker Kafka, schema registry, exactly-once semantics
- Microfrontends, mobile app, i18n
- Real user management / GDPR concerns

---

## 6. Repo Layout — actual (2026-07-07), vs. originally planned

```
SWAM/
  CLAUDE.md               # this file
  ProjectIdea.md          # original proposal + professor reply
  course_content/         # Moodle export (read-only reference)
  backend/                # Quarkus REST API (api-service) — Incident/Tag/User/Comment CRUD
    src/main/java/com/msohailse/app/incident/
      domain/             # Incident, Tag, User, UserType, Severity, Comment — @Entity (Pragmatic Hexagonal)
      application/port/out/    # *RepositoryPort + IncidentEventPublisherPort interfaces
      application/service/     # IncidentService, TagService, UserService (@Transactional)
      adapters/in/rest/        # IncidentResource, TagResource, UserResource + exception mapper
      adapters/out/persistence/ # *PostgresRepository (@ApplicationScoped, @Inject EntityManager)
      adapters/out/messaging/   # KafkaIncidentEventPublisher — publishes incident-created events
    src/main/docker/Dockerfile.jvm
    analyzer-service/     # separate Quarkus project — Kafka consumer, Stage-1 dedup (pg_trgm)
      src/main/java/com/msohailse/app/analyzer/
        domain/                    # own minimal Incident/User/Comment @Entity mapped to the same tables — no shared code with backend/
        application/DuplicateDetector.java  # pg_trgm similarity query + system-comment write + pg_trgm extension bootstrap
        adapters/in/messaging/     # IncidentCreatedConsumer (@Incoming)
      src/main/docker/Dockerfile.jvm
  frontend/               # Angular 19 standalone SPA (login/register/incidents/tags, adminGuard)
    Dockerfile, nginx.conf
  deploy/
    docker-compose.yml    # kafka + postgres + backend + analyzer-service + frontend, host port 5433 for postgres (5432 taken by another project)
    .env.example          # copy to .env (gitignored) for real credentials
    k8s/                  # Minikube manifests — dedicated "swam" namespace, headless Kafka Service, CPU-based HPA on backend
```

**Created 2026-07-07:** `deploy/k8s/` (Minikube manifests, built + deployed, see §0/§7 Day 3);
`docs/diagrams/` + `docs/screenshots/` (Mermaid sources/renders + live UI captures);
`report/` (technical report + presentation, Markdown/PDF/PPTX, see §7 Day 4).
**Not yet created:** `loadtest/` (k6 — Day 3, a manual busybox loop already proved the HPA
mechanism).

---

## 7. TODO — 4-Day Sprint Plan (re-baselined 2026-07-06, hard deadline 2026-07-10)

**Timeline collapsed from 7 days to 4** — deadline confirmed 2026-07-10, no code exists yet
on 2026-07-06. The four cuts from the old Cut List are now **applied by default, not
conditional** — there is no slack left to try the fuller version first. Phases below are
merged/compressed accordingly. Working style unchanged: Claude does the heavy lifting on
boilerplate/scaffolding, you review and steer; last ~30 min of each day = check off todos +
do that day's 📸 captures while fresh.

**Cuts applied by default (see §5/old §7 for the full reasoning):**
1. Angular: **2 screens only** (submit + track) — no cases dashboard.
2. **No real Kubernetes/KEDA deployment** — write the K8s manifests + `ScaledObject` as
   "designed, not deployed" (included in repo, described in report), and get the scaling
   *chart* instead from `docker compose up --scale analyzer=3` vs `1`, measuring Kafka lag
   drain manually. This still produces a legitimate scaling chart for the report.
3. **No transactional outbox** — publish to Kafka directly after commit in the same
   service call. Keep the incident+tags single-transaction ACID test (that's the
   non-negotiable ACID showcase, unrelated to the outbox). Outbox is documented as the
   known fix for the dual-write problem under "future developments."
4. **Threshold tuning**: eyeball ~5 hand-picked report pairs instead of a 20-pair test set.
5. Load-testing rigor (Phase 8, §7 below) is scaled down from full GQM/hypothesis-testing
   to a **lightweight version**: state the GQM goal + report descriptive stats (means,
   p95) with one comparison chart; skip formal t-test/Spearman/multi-trial repetition and
   the full threats-to-validity table unless Day 4 morning has spare time.

**Never cut regardless of time pressure:** the embeddings dedup (Stage 2, RAG-lite — it's
the report's star chapter), the 202/async flow, hexagonal layering (Strict variant, D1b),
and the phase 📸 captures (the report *is* the deliverable).

### The 4 Days at a Glance

| Day | Date | Goal (end-of-day demo) | Old phases compressed in |
|-----|------|------------------------|---------------------------|
| **1** | 2026-07-07 | Both services boot (Postgres+Kafka via Docker Compose); domain model + ports done; ACID rollback test green; `POST /reports` → 202 + UUID; direct-publish to Kafka works; `GET /reports/{id}` status endpoint live | Phase 0 + 1 + 2 |
| **2** | 2026-07-08 | Analyzer consumes, Stage-1 trgm dedup + Stage-2 embedding dedup both work end-to-end; 2 similar reports → 1 case; read-model (`case_summary`) + `GET /cases`/`GET /stats` endpoints live | Phase 3 + 4 + 5 |
| **3** | 2026-07-09 | Angular: submit → track → see Case Number appear (2 screens only); **real Minikube deployment** (Deployments/Services/HPA on CPU) with the autoscaling demo actually run on the cluster | Phase 6 + 7 (real deploy) + 8 (lightweight) |
| **4** | 2026-07-10 | Report assembled from all captures; ~15 slides; demo video recorded as live-demo fallback; **submit** | Phase 9 |

### Day 1 — Foundations + Domain + Ingestion
**Reordered vs. the original plan** — built the full synchronous CRUD foundation (REST +
Angular + Docker) first instead of the async Kafka/`202` flow, since the existing
`incident-reporting-system` codebase gave a head start on CRUD but had zero REST/UI. The
async ingestion flow (`POST /reports` → `202`, Kafka, Analyzer) is now the start of "Day 2."
- [x] `git init` done (repo: `msohailse/SWAM`, not yet pushed); `.gitignore` added; `ProjectIdea.md`/`CLAUDE.md` not yet committed
- [x] `backend/` migrated to Quarkus (this is the api-service; `analyzer-service` doesn't exist yet)
- [x] `deploy/docker-compose.yml`: postgres + backend + frontend — **no Kafka yet** (that's Day 2)
- [x] Domain model — `Incident`, `Tag`, `User`, `UserType`, `Severity`, `Comment` — kept `@Entity` (Pragmatic Hexagonal, D1b revised)
- [x] Ports: `IncidentRepositoryPort`, `TagRepositoryPort`, `UserRepositoryPort`, `CommentRepositoryPort`
- [x] JPA adapters (`adapters/out/persistence/`), Incident↔Tag `@ManyToOne` (kept from the existing codebase — deviates from `ProjectIdea.md`'s many-to-many, document this explicitly in the report)
- [x] ~~ACID showcase test~~ — **dropped 2026-07-07**: testing is not a professor
  requirement (zero mentions in `ProjectIdea.md`'s checklist, confirmed §8). The ACID
  property itself *is* named in the proposal (single-transaction incident+tag creation)
  and is satisfied architecturally by `IncidentService`'s `@Transactional` methods —
  described in the report (§6.3/§7), no automated test needed to prove it for this exam.
- [x] Full CRUD REST instead of the async flow: `POST/GET/PUT/DELETE /incidents`, `/tags`, `POST /users/register|login`, `PATCH /incidents/{id}/close` (admin-only, requires a `Comment`), `GET /incidents/{id}/comments`, `POST /incidents/{id}/comments` (reply — either role, added 2026-07-07 so the reporter can respond to the admin's closing comment, not just read it)
- [x] ~~`POST /reports` → `202` + Kafka publish~~ — **intentionally not built, not a gap**:
  this was the original proposal's ingestion shape, but the project deliberately pivoted
  to a synchronous `POST /incidents` (see §0) to reuse the existing, already-tested CRUD
  codebase rather than rebuild it around a tracking-ID/polling model. Documented as an
  explicit deviation in the report (§5), not a missed requirement — the professor's own
  confirmation that "a prototype with limited functionality suffices" covers this.
- [x] Angular frontend (login/register/incidents/tags, `adminGuard`, per-incident expandable comment thread + reply box) — originally planned for Day 3, built early since it was needed to validate the REST API
- [x] Verified end-to-end: `mvn test` (107 tests green), `mvn quarkus:dev` + manual curl CRUD, `ng serve` via proxy, and the full `docker compose up --build` stack (including `--scale backend=3`)
- 📸 Capture: tech stack table, domain class diagram (now includes `Comment`), ER diagram, REST API sequence diagram

### Day 2 — Analyzer + Full Dedup + CQRS Read Side
- [x] Kafka consumer in analyzer-service (consumer group `analyzer`) — not explicitly
  idempotent (no dedup-of-events-themselves logic) since re-processing the same
  incident-created event twice just adds one more duplicate-flag comment, harmless for a prototype
- [x] Stage-1: `pg_trgm` similarity query against other open incidents, threshold 0.4 (native query, `DuplicateDetector.checkForDuplicate`) — verified live: similar pair flagged, unrelated incident not
- [x] ~~Stage-2: `pgvector` + embeddings~~ — **built on a separate branch, deliberately not
  merged, and not required**: `ProjectIdea.md` only asks the system to "identify if an
  incident is a duplicate," which Stage-1 (pg_trgm) already satisfies; embeddings were
  the project's own stretch-goal RAG-lite idea (§3), not a professor requirement. Parked
  on `stage2-semantic-dedup` for future work, per §11 of the report.
- [x] "Combine stages" simplified to Stage-1 only for now: trgm hit → system comment flagging the duplicate; no hit → nothing (there's no separate `CaseNumber`/case concept anymore — see §0, we pivoted to direct Incident CRUD, so "duplicate" is expressed as a comment on the incident, not a case-linking operation)
- [x] ~~Threshold tuning against a hand-picked pair set~~ — **dropped**: not a professor
  requirement; the current 0.4 threshold is documented as an unverified first guess in
  both the code comments and the report (§6.1), which is sufficient disclosure for a
  prototype exam project.
- [x] CQRS-lite filtered read endpoint — `GET /incidents?tag=&severity=&status=` (bare minimum, no separate read-model/projection table); `case_summary`/`/stats` style read models still not built, current reads otherwise are direct `GET /incidents`, `GET /incidents/user/{id}` against the write-side tables
- [x] ~~Automated test for the dedup flow~~ — **dropped, testing not required** (see §8);
  verified manually via curl instead, which is documented in the report.
- 📸 Capture: EDA component diagram (backend → Kafka → analyzer-service), dedup decision flow (trgm-only for now)

### Day 3 — Frontend + Real Minikube Deployment + Scaling Demo
- [x] Angular scaffold — done earlier than planned, see Day 1 (needed to validate the REST API as it was built)
- [x] Wire CORS on API service — done earlier (Day 1), `quarkus.http.cors=true`
- [x] Containerfiles for all three services (backend, analyzer-service, frontend) — done earlier (Day 1/2); built straight into Minikube's own Docker daemon via `eval $(minikube docker-env)` + `docker build`, no registry push needed (`imagePullPolicy: Never`)
- [x] K8s manifests — `deploy/k8s/`: `00-namespace.yaml` (dedicated `swam` namespace — see incident note below), `00-secret.yaml` (DB credentials), `01-postgres.yaml`, `02-kafka.yaml` (Deployment + **headless** Service — see note below), `03-backend.yaml` (Deployment + Service + CPU requests/limits for HPA), `04-analyzer.yaml` (Deployment, no Service — no REST), `05-frontend.yaml` (Deployment + NodePort Service), `06-backend-hpa.yaml` (`autoscaling/v2` HPA, CPU target 50%, min 1/max 5)
- [x] Deployed for real to Minikube (not "designed, not deployed") — verified live: registered a user, created two near-duplicate incidents through the K8s-hosted stack, confirmed the full EDA pipeline (backend → Kafka → analyzer-service → Postgres) flags the second one, exactly as it does under Docker Compose
- [x] HPA scaling demo — ran a 6-replica `busybox` load-generator Deployment hammering `GET /incidents`; watched `kubectl get hpa -n swam -w`: backend climbed from 1 → 3 → 5 replicas (max) as CPU rose from 6% to 127%/50% target, then scaled back down once the load generator was removed (HPA's default 5-minute scale-down stabilization window)
- [x] Lightweight load comparison (no k6, no professor requirement for it — dropped the
  "formal k6 script" ambition, done with plain parallel `curl` instead) — 1500 requests
  at concurrency 100 against `GET /incidents`, backend at 1 replica vs. 5 replicas:
  p95 latency 85.4ms → 17.8ms (~4.8x), mean 35.6ms → 8.1ms (~4.4x). Chart at
  `docs/diagrams/scaling-latency-comparison.png`, full writeup in the report §10.3 and
  its own presentation slide. Explicitly framed as descriptive-stats-only, single trial,
  no formal hypothesis testing — consistent with the "lightweight" scope agreed in §7's
  cut list.
- 📸 Capture: screenshots of the app, K8s deployment diagram, HPA replica-count-climbing screenshot (1→5), scaling-down screenshot

**Incident during this slice, resolved — worth remembering:** the first `kubectl apply` of
these manifests targeted the default (unnamespaced) namespace, on a *shared* Minikube
cluster that already had an unrelated demo project running (Google's "Online Boutique"
sample — `adservice`, `cartservice`, etc., up 21 days). That project also had a `frontend`
Deployment + Service, and the plain-name collision caused `kubectl apply` to silently
overwrite the boutique's `frontend` Deployment image and the Service's `targetPort`.
Caught it immediately, fixed it (`kubectl rollout undo` for the Deployment, a manual
`targetPort` patch for the Service, both confirmed working again), then moved every SWAM
resource into its own `swam` namespace (`00-namespace.yaml`) so this class of collision
can't recur. Separately, the single-broker Kafka pod crash-looped under a plain
`ClusterIP` Service because a KRaft node's self-connection to its own controller listener
(`kafka:9093`) hairpins back through the Service NAT, which this cluster's networking
doesn't handle reliably — fixed by making the Kafka Service headless (`clusterIP: None`),
which resolves the name straight to the pod IP instead.

### Day 4 — Report, Slides, Submission
- [x] Mermaid diagrams — `docs/diagrams/*.mmd` (architecture, domain-model, dedup-sequence,
  k8s-deployment, use-case), rendered to PNG via `@mermaid-js/mermaid-cli` (`npx mmdc`),
  versioned as source per §9's working agreement
- [x] Live UI screenshots — `docs/screenshots/*.png`, captured via Playwright (headless
  Chromium) against the running Docker Compose stack with real seed data (register,
  create incidents, close-with-comment, reply), not mocked/hand-drawn
- [x] Technical report — `report/technical-report.md` (+ `technical-report.pdf` via
  pandoc/xelatex), covering all 12 items from the professor's checklist in
  `ProjectIdea.md` (objective, functional/non-functional requirements, use case diagram,
  domain class diagram, preliminary + detailed design, architecture, DB design, frontend
  mockup/preview, main components, future developments/conclusions)
- [x] Presentation — `report/SWAM-Presentation.pptx` (+ `.pdf`), 17 slides, built with
  python-pptx directly off the user-provided reference deck's own template/theme/layouts
  (Copertina/Interna_solo testo/Controcopertina) so visual style matches; content mapped
  1:1 to the report's sections
- [x] "Future developments" section — written into both the report §11 and the closing
  slide, covering Stage-2 semantic dedup, transactional outbox, real JWT auth, formal k6
  load testing, and full CQRS read models
- [~] Record a demo video as live-demo fallback — **not a professor requirement**
  (`ProjectIdea.md` asks only for a report + a live ~20 min presentation, zero mentions
  of a video); left as an optional personal safety net, not tracked as required work.
- [ ] Final proofread + **submit** — inherently the user's own final pass before
  submission, not something to automate away.

### Reference — Learning Links (kept from the original 7-day plan, still valid)
- 📚 Quarkus Kafka guide: https://quarkus.io/guides/kafka · Testcontainers: https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/
- 📚 Hexagonal architecture original: https://alistair.cockburn.us/hexagonal-architecture/ · practical intro: https://www.baeldung.com/hexagonal-architecture-ddd-spring
- 📚 Kafka consumer groups: https://developer.confluent.io/courses/architecture/consumer-group-protocol/
- 📚 Dedup/RAG-lite references: see §3 above (Simon Willison's post first, then pgvector README)
- 📚 Fowler on CQRS: https://martinfowler.com/bliki/CQRS.html
- 📚 Angular standalone tutorial: https://angular.dev/tutorials/learn-angular
- 📚 kind quickstart: https://kind.sigs.k8s.io/docs/user/quick-start/ · KEDA Kafka scaler: https://keda.sh/docs/latest/scalers/apache-kafka/ · Quarkus→K8s guide: https://quarkus.io/guides/deploying-to-kubernetes
- 📚 k6 getting started: https://grafana.com/docs/k6/latest/ · course's own Empirical SE lectures (`course_content/.../Folder_Empirical_Software_Eng...`) for the GQM/stats/threats-to-validity template
- 📚 Transactional outbox pattern (for the "future developments" writeup): https://microservices.io/patterns/data/transactional-outbox.html
- 📚 PlantUML (diagrams as code, version-controlled): https://plantuml.com/

---

## 8. Open Questions (to verify with course / decide later)

1. ~~Quarkus vs classic WildFly~~ — **answered 2026-07-06: Quarkus confirmed.** See D1 —
   `course_content` forum announcement documents a 2026-04-21 lecture specifically on
   Quarkus + Kubernetes (paired with Istio/K6/Grafana/OpenTelemetry). That specific
   lecture's slide deck itself was **not found** in the exported `course_content` —
   action item: pull it from Moodle directly, since it's the single most relevant deck
   we're missing (may clarify whether Istio/OpenTelemetry are expected in the report).
2. **Report language/format** — LaTeX vs Word; any template from the course? (still open —
   not addressed in the materials searched so far)
2b. ~~Is testing required?~~ — **answered 2026-07-06: no.** `ProjectIdea.md` (proposal +
   professor reply) has zero mentions of testing/coverage. Kept the existing test suite
   (107 tests, migrated JUnit4→5) only because it already passed after the Quarkus
   migration — not investing further time in it per explicit user instruction.
3. ~~Deadline~~ — **answered 2026-07-06: hard deadline is 2026-07-10, i.e. 4 days, not 7.**
   Sprint plan in §7 rewritten around this with default cuts applied (no transactional
   outbox, no real K8s/KEDA deploy, 2 Angular screens, lightweight load-test rigor).
4. **User entity depth** — is authentication expected at all, or is a stubbed user acceptable? (MVP assumes stub.)
5. Dedup thresholds (trgm + cosine) — tune empirically in Phase 4, document chosen values.
6. **Module track — IIA (Empirical SE) vs IIB (RESTful/Microservice SOA)** — `about.html`
   states these are alternatives, not both required. The whole project design already
   matches IIB (Quarkus/K8s/Kafka/K6), so defaulting to that assumption; Phase 8 now
   adopts the IIA module's GQM/stats/threats-to-validity report structure anyway since
   it's a strict rigor upgrade regardless of track. **Needs user confirmation.**
7. **Team size** — solo vs. 2-3 (course allows either, per `Swam26-1...pdf` slide 9/30).
   CLAUDE.md is written assuming solo ("you review and steer") — **needs user confirmation**
   since a team changes how phases can be parallelized.
8. ~~Hexagonal variant~~ — **answered 2026-07-07: Pragmatic**, reversed from the earlier
   Strict default (see D1b). Both are explicitly sanctioned by
   `SWAM26-19_HexagonalAndCleanArchitectures.pdf`; Pragmatic won because the migrated
   codebase's entities were already JPA-annotated and tested.

---

## 9. Working Agreements (for Claude sessions in this repo)

- MVP scope in §5 is a **ceiling**, not a floor — push back on additions, point to "stretch"
- `domain/` follows **Pragmatic Hexagonal** (D1b, revised 2026-07-07): `@Entity` directly
  on domain classes is expected, not a bug — the purity rule from earlier in this file no
  longer applies. Ports still isolate persistence/REST from the domain+application layers.
- Every phase ends with its 📸 capture task done — the report is the actual deliverable
- Prefer boring technology inside the boxes; the architecture between the boxes is the showcase
- **Report, presentation slides, and all UML diagrams are co-created with Claude in these sessions** — diagrams as PlantUML/Mermaid source in `docs/` so they're versioned and regenerable; report drafted section-by-section as phases complete, not all at the end
- Keep this file updated: check off todos, log decision changes in §4 with date
