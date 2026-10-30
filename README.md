# Package Delivery Tracker API

## Vision

Et backend-API, hvor brugere kan oprette pakker og følge deres leveringsstatus. Medarbejdere kan registrere statusændringer.

## Links

- Deployed API: https://api.ozeldemo.dk/api/
- GitHub repository: https://github.com/EmilOzel/Package_Delivery_Tracker
- Portfolio: https://emilozel.github.io/Portfolio/

# Architecture

## System Overview

Projektet anvender en lagdelt arkitektur:

```text
Client → Controller → Service → DAO → PostgreSQL
                         ↓
                OpenRouteService
```

Teknologier:

- Java 25 og Javalin
- Hibernate/JPA og PostgreSQL
- JWT og BCrypt
- Docker og GitHub Actions

## Key Design Decisions

- DTO'er styrer API'ets JSON-data.
- BCrypt bruges til password-hashing.
- JWT bruges til authentication.
- Roller styrer adgangen til endpoints.
- OpenRouteService validerer adresser.

# Data Model
- 'docs' package indeholder ERD
```text
User "1" ─── "0..*" Parcel
Parcel "1" ─── "1..*" DeliveryEvent
```

- `User` indeholder kontooplysninger og rolle.
- `Parcel` indeholder trackingnummer, adresse og nuværende status.
- `DeliveryEvent` indeholder en statusændring med tidspunkt og lokation.

Statusser:

```text
CREATED
PICKED_UP
IN_TRANSIT
AT_SORTING_CENTER
OUT_FOR_DELIVERY
DELIVERED
CANCELLED
```

# API Documentation

Base URL:

```text
https://api.ozeldemo.dk/api
```

| Method | Endpoint | Beskrivelse |
|---|---|---|
| POST | `/auth/register` | Registrér bruger |
| POST | `/auth/login` | Login og modtag JWT |
| POST | `/parcels` | Opret pakke |
| GET | `/parcels` | Hent pakker |
| GET | `/parcels/tracking/{trackingNumber}` | Find pakke |
| GET | `/parcels/{id}/events` | Hent statushistorik |
| POST | `/parcels/{id}/events` | Tilføj statusændring |
| DELETE | `/parcels/{id}` | Slet ny pakke |

Eksempel på oprettelse:

```json
{
  "senderName": "Emil Ozel",
  "recipientName": "Bo Jensen",
  "destinationAddress": "Amperestien 10, 2605 Brøndby"
}
```

Beskyttede endpoints kræver:

```http
Authorization: Bearer <token>
```

# User Stories

- Som besøgende vil jeg kunne registrere en konto, så jeg kan bruge systemet.
- Som bruger vil jeg kunne oprette en pakke.
- Som bruger vil jeg kunne se mine egne pakker.
- Som bruger vil jeg kunne finde en pakke ved hjælp af dens trackingnummer.
- Som bruger vil jeg kunne se en pakkes leveringshistorik.
- Som bruger vil jeg være sikker på, at andre brugere ikke kan se mine pakker.
- Som medarbejder vil jeg kunne registrere leveringsproblemer.
- Som medarbejder vil jeg kunne ændre en pakkes status.
- Som administrator vil jeg kunne administrere pakker.
- Som bruger vil jeg have min destinationsadresse bekræftet, når jeg opretter en pakke.

# Development Notes

Projektet indeholder DAO- og REST-tests med JUnit, REST Assured og Testcontainers.

GitHub Actions tester og bygger Docker-imaget. API'et er deployet på DigitalOcean med Docker Compose og Caddy.
