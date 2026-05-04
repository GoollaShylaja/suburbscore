# user-service

Handles user registration, authentication, and preference management for the SuburbScore platform.
Issues JWT tokens on register and login. Preferences capture what a user is looking for in a suburb.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (for PostgreSQL)

## Setup

**1. Start the database**
```bash
docker-compose up -d postgres
```

**2. Configure environment**
```bash
cp .env.example .env
# Edit .env with your values
```

**3. Run the service**
```bash
export $(cat .env | xargs) && mvn spring-boot:run
```

Service starts on **http://localhost:8081**

## API

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/users/register | Public | Register a new user |
| POST | /api/users/login | Public | Login and get JWT token |
| POST | /api/users/logout | Bearer | Invalidate current token |
| GET | /api/users/profile | Bearer | Get profile and preferences |
| PUT | /api/users/preferences | Bearer | Save or update preferences |

**Swagger UI:** http://localhost:8081/swagger-ui.html *(requires `SWAGGER_ENABLED=true` in `.env`)*

## Running tests

```bash
export $(cat .env | xargs) && mvn test
```

34 tests — service, controller, and JWT utility layers.

## Environment variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USERNAME` | PostgreSQL username | `suburbscore` |
| `DB_PASSWORD` | PostgreSQL password | `suburbscore123` |
| `JWT_SECRET` | JWT signing key (min 256 bits) | `your-secret-here` |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `86400000` (24h) |
| `SWAGGER_ENABLED` | Enable Swagger UI | `true` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins | `http://localhost:3000` |

## Security

- JWT Bearer token authentication (stateless)
- Rate limited: 10 requests/min per IP on login and register
- Tokens can be revoked via `/logout` (JTI blacklist)
- All errors returned as ProblemDetail (RFC 7807)
