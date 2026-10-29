# Package Delivery Tracker API

Et Java- og Javalin-baseret REST API, hvor brugere kan oprette pakker og følge deres leveringshistorik.

## Teknologier

- Java 25 og Maven
- Javalin REST API
- Hibernate/JPA og PostgreSQL
- Jackson DTO/JSON-mapping
- OpenRouteService til adressevalidering
- BCrypt password-hashing
- JWT authentication og rollebaseret authorization
- JUnit, Hamcrest, REST Assured og Testcontainers
- Docker og GitHub Actions

## Roller

- `USER`: Opretter og ser egne pakker.
- `EMPLOYEE`: Ser pakker og registrerer statusændringer.
- `ADMIN`: Har administrativ adgang og kan ændre status.

## Centrale endpoints

```text
POST   /api/auth/register
POST   /api/auth/login

POST   /api/parcels
GET    /api/parcels
GET    /api/parcels/tracking/{trackingNumber}
GET    /api/parcels/{id}/events
POST   /api/parcels/{id}/events
DELETE /api/parcels/{id}
```

REST-ruterne til pakker kræver `Authorization: Bearer <token>`. En almindelig bruger ser kun egne pakker. Statusændringer kræver `EMPLOYEE` eller `ADMIN`.

## Lokal konfiguration

`src/main/resources/config.properties` er ignoreret af Git og skal indeholde:

```properties
DB_NAME=PackageTracker
DB_USERNAME=postgres
DB_PASSWORD=dit-password
OPENROUTESERVICE_API_KEY=din-api-nøgle
JWT_SECRET=mindst-32-tilfældige-tegn
```

Start `app.Main` og brug API'et på `http://localhost:7070/api`.

Kør eventuelt `app.config.Populate` én gang for at oprette en demo-bruger og en demo-medarbejder til manuel test.

## Test

Docker Desktop skal køre, fordi integrationstestene bruger PostgreSQL gennem Testcontainers.

```bash
mvn clean test
```

REST-testene bruger REST Assured og kontrollerer registrering, login, JWT, roller, pakkeoprettelse og statusændring.

## Docker

Kopiér `.env.example` til `.env`, udfyld værdierne og kør:

```bash
docker compose up --build
```

Stop med:

```bash
docker compose down
```

## CI/CD

GitHub Actions kører testene ved pushes og pull requests til `main`. Ved push til `main` bygges og pushes et Docker-image til Docker Hub.

Repository secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```
