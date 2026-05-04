# suburb-service

Read-only suburb data service for the SuburbScore platform. Exposes Sydney suburb demographics, rental stats, crime index, walkability scores, public transport access times, and school data via a paginated REST API.

## Prerequisites

- Java 21
- Docker (for PostgreSQL + Redis)
- Eureka server running on port 8761

## Setup

```bash
# 1. Copy environment file
cp .env.example .env
# Edit .env with your values

# 2. Start dependencies (from repo root)
docker-compose up -d postgres redis

# 3. Apply schema and seed data
psql -h localhost -p 5433 -U suburbscore -d suburbscore -f src/main/resources/db/schema.sql
psql -h localhost -p 5433 -U suburbscore -d suburbscore -f src/main/resources/db/seed.sql

# 4. Run the service
./mvnw spring-boot:run
```

Service starts on **port 8082**.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/suburbs` | None | List all suburbs (paginated, default 20/page sorted by name) |
| GET | `/api/suburbs/{id}` | None | Full suburb detail with stats, transport, and schools |
| GET | `/api/suburbs/postcode/{postcode}` | None | Find suburbs by 4-digit postcode (may return multiple) |
| GET | `/api/suburbs/{id}/stats` | None | Full statistics: rent, crime, walkability, transport, schools |
| GET | `/actuator/health` | None | Health check |

### Pagination

```
GET /api/suburbs?page=0&size=20&sort=name,asc
GET /api/suburbs?page=1&size=10&sort=region,asc
```

### Example Response — `/api/suburbs/{id}`

```json
{
  "id": "00000000-0000-0000-0000-000000000002",
  "name": "Newtown",
  "postcode": "2042",
  "lga": "Inner West Council",
  "region": "Inner West",
  "latitude": -33.897900,
  "longitude": 151.179200,
  "stats": {
    "medianRentWeekly": 650,
    "crimeIndex": 45.0,
    "walkabilityScore": 88.0,
    "population": 15420,
    "medianAge": 29,
    "unemploymentRate": 5.80,
    "transport": {
      "nearestTrainStation": "Newtown Station",
      "trainStationWalkMins": 5,
      "numBusRoutes": 8,
      "cbdCommuteMinsTrain": 18,
      "cbdCommuteMinsBus": 25
    },
    "schools": {
      "numPrimarySchools": 2,
      "numHighSchools": 1,
      "avgNaplanScore": 520.0,
      "bestSchoolName": "Newtown High School of the Performing Arts"
    }
  }
}
```

## Swagger UI

Enable with `SWAGGER_ENABLED=true` (development only), then visit:

```
http://localhost:8082/swagger-ui.html
```

## Running Tests

```bash
./mvnw test
```

22 tests across 2 layers:
- `SuburbServiceTest` — 11 service unit tests (Mockito)
- `SuburbControllerTest` — 11 controller slice tests (MockMvc + @WebMvcTest)

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USERNAME` | PostgreSQL username | `suburbscore` |
| `DB_PASSWORD` | PostgreSQL password | `suburbscore123` |
| `JWT_SECRET` | HS384 signing secret (min 32 chars, must match user-service) | `your-secret-here` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:3000` |
| `SWAGGER_ENABLED` | Enable Swagger UI (default: false) | `true` |

## Security

- All suburb endpoints are **public** (no authentication required) — suburb data is not user-specific
- JWT filter is present for future protected endpoints (e.g. admin data ingestion)
- CORS policy via `CorsConfigurationSource` with origins from `CORS_ALLOWED_ORIGINS`
- Actuator restricted to `/actuator/health`, details hidden
- All errors returned as `application/problem+json` (RFC 7807)

## Caching

Responses are cached in Redis with a 1-hour TTL. Cache is keyed per endpoint:
- `suburbs` — paginated suburb list
- `suburb` — individual suburb by ID
- `suburbsByPostcode` — suburbs by postcode
- `suburbStats` — stats by suburb ID

## Database Schema

See `src/main/resources/db/schema.sql` for the full DDL.

| Table | Description |
|-------|-------------|
| `suburbs` | Core suburb data (name, postcode, LGA, coordinates) |
| `suburb_stats` | Rent, crime index, walkability, demographics |
| `transport_data` | Nearest station, walk time, commute durations |
| `school_data` | School counts, NAPLAN scores |
| `suburb_scores` | Per-user computed scores (populated by scoring-service) |
