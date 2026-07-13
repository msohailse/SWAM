# SWAM API Service (`backend/`)

The main REST API of the SWAM incident-management project (SWAM course exam project —
see the top-level `README.md` for running the whole stack and `CLAUDE.md` for the full
architecture/decision log). A Quarkus 3 service that owns all synchronous work: CRUD for
incidents, tags, users, and comment threads, plus publishing an `incident-created` Kafka
event after every new incident — consumed asynchronously by the separate
[`analyzer_microservice/`](analyzer_microservice/) for duplicate detection.

This service started life as a Java 8 Swing desktop app from an earlier course project
and was migrated to this REST API; none of the Swing/desktop code remains.

## Tech stack

- Java 17, Quarkus 3.37.1 (Jakarta REST / CDI / JPA, SmallRye Reactive Messaging for Kafka)
- PostgreSQL via Hibernate ORM (`database.generation=update` — fine for this prototype)
- JUnit 5 test suite (`mvn test`) — Quarkus Dev Services auto-starts throwaway
  Postgres/Kafka containers, so tests and dev mode need nothing running beforehand

## Run

```bash
mvn quarkus:dev    # API on :8080 — Dev Services starts Postgres+Kafka automatically
mvn test
```

For the whole stack (this service + analyzer + frontend + real Postgres/Kafka), use
Docker Compose from `../deploy` — see the top-level `README.md`, which also has the API
quick reference. The image is built by `src/main/docker/Dockerfile.jvm` (multi-stage, no
local `mvn package` needed).

Two logins are seeded at startup if missing (`UserSeeder`): an admin and a reporter,
configurable via `SEED_*` env vars in `deploy/.env` (defaults in
`src/main/resources/application.properties`). Registration itself always creates
REPORTER users, so this is how the first admin exists.

## Structure — Hexagonal (Ports & Adapters), pragmatic variant

```
src/main/java/com/msohailse/app/incident/
  domain/                  Incident, Tag, User, UserType, Severity, Comment
                           JPA entities with the business rules in their setters
                           (e.g. password complexity, non-blank comment text)
  application/
    port/out/              interfaces the core needs from the outside world:
                           Incident/Tag/User/CommentRepositoryPort, IncidentEventPublisherPort
    service/               use cases: IncidentService, TagService, UserService
                           (@Transactional), UserSeeder (startup seed users)
  adapters/
    in/rest/               IncidentResource, TagResource, UserResource
                           + exception mapper (business-rule violations -> 400 JSON)
    out/persistence/       *PostgresRepository — JPA implementations of the ports
    out/messaging/         KafkaIncidentEventPublisher — incident-created events
analyzer_microservice/          separate Quarkus project (own pom.xml, own image, zero shared
                           Java code with this one) — Kafka consumer + pg_trgm dedup
```

"Pragmatic" hexagonal: the domain entities keep their `@Entity` annotations instead of a
separate pure domain + mapping layer, but all persistence and messaging still sit behind
`application/port/out/` interfaces, so the core never references Postgres or Kafka
directly.

## Entities

- **User** — firstName, lastName, email (unique), password (plain text — documented stub,
  no hashing/JWT), `UserType` role: `REPORTER` or `ADMIN`
- **Incident** — title, description, severity (`LOW`/`MEDIUM`/`HIGH`), closed flag;
  `@ManyToOne` to its reporting User and to one Tag
- **Tag** — tagTitle, tagDescription (one tag per incident, admin-managed)
- **Comment** — thread entry on an incident (author + timestamp); created by an admin
  closing an incident, by either role replying, or by `analyzer_microservice` flagging a
  likely duplicate
