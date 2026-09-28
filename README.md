# DPDMS Starter Kit

Starter scaffolding for the **Rushinga Provincial Disaster Monitoring and
Management System** (HCS201/HCC201/HAI201 group assignment).

This is not the whole system — it's a working, correctly-structured skeleton
covering the two pieces every other service depends on, plus one complete
hazard service (`flood-service`) built end-to-end so your group can clone its
shape for drought, fire, zoonotic-disease, and mining-accident.

## What's included

| Module | Purpose | Status |
|---|---|---|
| `discovery-service` | Eureka service registry | Complete, runnable |
| `gateway-service` | Spring Cloud Gateway, single entry point | Complete, runnable |
| `flood-service` | Full hazard microservice: entity, repository, DTOs, service layer, REST controller, approval workflow, hazard-scoped security, exception handling, unit test | Complete, runnable |

Not included (build these next, using `flood-service` as your pattern):
`drought-service`, `fire-service`, `zoonotic-disease-service`,
`mining-accident-service`, `auth-service`, `report-service`,
`alert-service`, `dashboard-service`, and the front end.

## Why the code looks the way it does

- **Package-by-layer inside package-by-feature** (`flood/model`,
  `flood/service`, `flood/controller`...) — keeps each service self-contained,
  which matters when five people are each cloning it into their own service.
- **DTOs are separate from entities.** Never return a JPA entity directly
  from a controller — you lose control over what's exposed and you couple
  your API contract to your schema.
- **The approval workflow is a real state machine**, not just a status
  string set ad hoc. `IncidentStatus` transitions are validated in
  `FloodIncidentService`, not left to the controller to get right or wrong.
- **Hazard scoping is enforced in the service layer**, reading claims off
  the authenticated principal — never trust a hazard/ward value the client
  sends in the request body for a write.
- **Nothing is hardcoded.** DB credentials and the JWT secret come from
  environment variables via `application.yml` placeholders.

## Running locally

```bash
# 1. Start MySQL (or use docker-compose below)
# 2. Start discovery-service
cd discovery-service && mvn spring-boot:run

# 3. Start gateway-service (in a new terminal)
cd gateway-service && mvn spring-boot:run

# 4. Start flood-service (in a new terminal)
cd flood-service && mvn spring-boot:run
```

Then visit:
- Eureka dashboard: http://localhost:8761
- Flood service via gateway: http://localhost:8080/api/floods
- Flood service Swagger UI (direct): http://localhost:8081/swagger-ui.html

## Environment variables flood-service expects

```
DB_URL=jdbc:mysql://localhost:3306/flood_db
DB_USERNAME=root
DB_PASSWORD=yourpassword
JWT_SECRET=a-long-random-secret-string-min-32-chars
```

## Docker Compose (add as you build more services)

```yaml
version: "3.8"
services:
  mysql:
    image: mysql:8
    environment:
      MYSQL_ROOT_PASSWORD: rootpass
      MYSQL_DATABASE: flood_db
    ports: ["3306:3306"]

  rabbitmq:
    image: rabbitmq:3-management
    ports: ["5672:5672", "15672:15672"]

  discovery-service:
    build: ./discovery-service
    ports: ["8761:8761"]

  gateway-service:
    build: ./gateway-service
    ports: ["8080:8080"]
    depends_on: [discovery-service]

  flood-service:
    build: ./flood-service
    environment:
      DB_URL: jdbc:mysql://mysql:3306/flood_db
      DB_USERNAME: root
      DB_PASSWORD: rootpass
      JWT_SECRET: change-me-in-production-please
    depends_on: [mysql, discovery-service]
```

## Cloning flood-service into the next hazard service

1. Copy the whole `flood-service` folder, rename it (e.g. `drought-service`).
2. Find-and-replace `Flood`/`flood` → `Drought`/`drought` throughout.
3. Replace the five flood-specific fields in `FloodIncident` with the five
   indicators for your hazard (see the assignment brief).
4. Change the `spring.application.name` and `server.port` in
   `application.yml` so it doesn't collide with the other services.
5. Re-run the unit test, fix field names, done.

## RBAC model used throughout

JWT claims: `role` (RECORDER / SUPERVISOR / NATIONAL / ADMIN), `hazard`
(FLOOD / DROUGHT / FIRE / ZOONOTIC_DISEASE / MINING_ACCIDENT — omitted for
NATIONAL), `ward` (only for RECORDER).

- RECORDER: CRUD on their own (hazard, ward) records only, cannot approve.
- SUPERVISOR: approve/reject/request-corrections on their hazard only,
  read-only otherwise for that hazard.
- NATIONAL: read-only, all hazards, approved records only.
- ADMIN: full access (use sparingly, mainly for seed/ops tasks).

Every hazard service re-validates these claims itself — the gateway does
**not** do authorization, only routing. That satisfies the brief's
requirement that scoping "must be enforced in the backend... and hold across
every service."
