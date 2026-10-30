# Package Delivery Tracker API

## Vision

Et backend-API, hvor brugere kan oprette pakker og følge deres leveringsstatus. Medarbejdere kan registrere statusændringer.

## Links

- Deployed API: https://api.ozeldemo.dk/api/
- GitHub repository: https://github.com/EmilOzel/Package_Delivery_Tracker
- Portfolio: https://emilozel.github.io/Portfolio/

# Architecture

## System Overview

Projektet anvender dette arkitekturlag:

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
- ER-DIAGRAMMET FINDES I MAPPEN `DOCS`.
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

- Som besøgende ønsker jeg at kunne registrere en konto og logge ind, så jeg kan bruge systemet.
- Som bruger ønsker jeg at kunne oprette en pakke med en valideret destinationsadresse, så adressen og koordinaterne bliver gemt korrekt.
- Som bruger ønsker jeg at kunne se mine egne pakker, så jeg kan følge deres aktuelle status.
- Som bruger ønsker jeg at kunne finde en af mine pakker med dens trackingnummer, så jeg hurtigt kan slå den op.
- Som bruger ønsker jeg at kunne se en pakkes leveringshistorik, så jeg kan følge pakkens statusændringer.
- Som medarbejder ønsker jeg at kunne registrere delivery events og ændre en pakkes status, så leveringsforløbet bliver opdateret.

# Development Notes

Projektet indeholder DAO- og REST-tests med JUnit, REST Assured og Testcontainers.

GitHub Actions tester og bygger Docker-imaget. API'et er deployet på DigitalOcean med Docker Compose og Caddy.
