# SWAM — Scalable Event-Driven Incident Management System

REST API (Quarkus) + Angular UI for reporting and tracking incidents, with an async Kafka-based duplicate-detection service.

## Quick Start (How to Run)

The absolute easiest way to run the entire application (PostgreSQL Database, Kafka, Backend API, Analyzer Microservice, and Frontend UI) is using Docker Compose.

1. Ensure you have Docker running on your machine.
2. Open a terminal in the root folder of this project and run:

```bash
docker compose up -d --build
```

3. Wait a minute or two for the services to fully start up.
4. Open your web browser and navigate to: **http://localhost:4200**

### Test Accounts

The following test accounts are automatically seeded into the database so you can test the application immediately:

- **Super Admin (Admin Access):**
  - Email: `superadmin@swam.local`
  - Password: `Admin1234`

- **Reporter (Standard Access):**
  - Email: `user@swam.local`
  - Password: `User12345`

- **Department Admin (IT Dept):**
  - Email: `it-admin@swam.local`
  - Password: `ItAdmin123`

## Stopping the Application

When you are done testing, you can stop all services by running:

```bash
docker compose down
```
