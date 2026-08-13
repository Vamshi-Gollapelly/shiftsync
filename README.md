![CI](https://github.com/Vamshi-Gollapelly/shiftsync/actions/workflows/ci.yml/badge.svg)
# ShiftSync

A multi-tenant shift-rostering API for small hospitality and retail businesses — the kind of internal tool that lets one system safely serve many independent cafés, restaurants, or shops without their data ever mixing.

## Why this exists

Most small businesses schedule staff through spreadsheets or group chats — which means double-bookings, no audit trail, and no easy way to check who worked a public holiday shift. ShiftSync is a backend built to solve that properly, with the same architectural concerns a real SaaS product would have: tenant isolation, role-based access, and an audit trail baked in from the start.

## What it does right now

- **Multi-tenant by design** — one API, many independent businesses, with tenant isolation enforced explicitly at the service layer (not an implicit global filter that's easy to forget)
- **JWT-based auth** — access + refresh tokens, business context embedded directly in the token
- **Role-based access** — OWNER / MANAGER / STAFF, with method-level authorization
- **Audit logging** — every meaningful action (business registration, login, staff changes) is recorded
- **Flyway-managed schema** — the database schema is version-controlled, not hand-managed

## Stack

Java 21 · Spring Boot 3 · Spring Security · PostgreSQL · Flyway · Docker · JWT (JJWT)

## Architecture

```
Client → JWT Auth Filter → Controller → Service (tenant-scoped) → Repository → PostgreSQL
```

Tenant isolation is the core design decision here: every table that belongs to a business carries an explicit `business_id`, and every repository method that touches those tables requires that ID as a parameter — there's no implicit "current tenant" filter that a future query could accidentally skip. The `business_id` itself comes exclusively from the signed JWT, never from anything the client sends directly, so there's no way for one business to query or modify another's data even by mistake.

## Running it locally

**Requirements:** Java 21, Docker, Maven (or use your IDE's built-in Maven support)

```bash
# start Postgres
docker compose up -d

# run the app (from your IDE, or)
./mvnw spring-boot:run
```

The API will be live at `http://localhost:8080`.

### Try it

```bash
# register a business
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "businessName": "Cafe Lulu",
    "slug": "cafe-lulu",
    "ownerFullName": "Your Name",
    "ownerEmail": "you@example.com",
    "password": "SuperSecret123"
  }'
```

This returns a JWT access token you can use to hit protected endpoints.
## Testing

Full test suite: 11 tests, all passing.

- **Unit tests** (`ShiftServiceTest`, `StaffServiceTest`) — 8 tests covering shift overlap detection, RBAC enforcement, and validation rules, using Mockito to isolate business logic from the database.
- **Integration test** (`AuthIntegrationTest`) — 3 tests exercising the full registration/login flow against a real, disposable PostgreSQL container via Testcontainers, proving the whole stack (controller → service → repository → database) works end-to-end.

## What's next

- Shift-scheduling logic with overlap detection and award-rate/public-holiday handling
- Automated test suite (JUnit, Mockito, Testcontainers)
- CI/CD pipeline via GitHub Actions
- Live deployment
- Observability (metrics, structured logging)

## Status

Actively being built as part of a personal portfolio project. Not production-ready yet — this README will be updated as each milestone lands.
