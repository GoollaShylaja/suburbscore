# SuburbScore — Claude Instructions

This is the SuburbScore platform (Sydney suburb intelligence app).
Always follow the patterns established in `user-service` when building new microservices.

---

## New Microservice Prompt Template

Use this when asked to build a new service. Fill in the placeholders.

```
Build a production-ready Spring Boot 3.5 microservice called `<service-name>`
as part of the SuburbScore platform (Sydney suburb intelligence app).

## Stack
- Java 21, Spring Boot 3.5, Spring Cloud 2025.0.0
- Spring Data JPA + PostgreSQL
- Spring Security (JWT — stateless, no sessions)
- springdoc-openapi 2.8.3 (Swagger UI)
- Lombok, Maven
- Eureka service discovery (client)
- Port: <port-number>

## What to build
<describe the domain>

## Entities needed
<list the entities and their fields>

## API endpoints needed
<list the endpoints>

## Security requirements
- Public endpoints: <list them>
- Protected endpoints (require JWT Bearer token): <list them>
```

---

## Standards — apply to EVERY microservice

### Security hardening
- All secrets via environment variables — no hardcoded values in any file
- JWT secret and DB password from ${JWT_SECRET}, ${DB_USERNAME}, ${DB_PASSWORD}
- Actuator restricted to /actuator/health only, details hidden
- Rate limiting (10 req/min per IP) on public POST endpoints via RateLimitFilter
- CORS policy via CorsConfigurationSource, origins from ${CORS_ALLOWED_ORIGINS}
- Swagger UI gated behind ${SWAGGER_ENABLED} env var (default false)
- Token revocation via JTI blacklist + POST /logout endpoint
- Password fields: @Size(min=8, max=72) + @Pattern (letter + number required)
- Generic error messages — no internal data leaked to client
- Security event logging (failed auth, invalid tokens logged with client IP)

### Error handling
- All errors in ProblemDetail format (RFC 7807) — application/problem+json
- GlobalExceptionHandler covering: ResourceNotFoundException (404),
  ConflictException (409), BusinessException (400), ExternalServiceException (502),
  BadCredentialsException (401), MethodArgumentNotValidException (400 with field errors)
- Custom exception hierarchy — all extend a base AppException class

### API documentation (Swagger)
- @Operation, @ApiResponses, @ExampleObject on every endpoint
- All status codes documented (200, 201, 400, 401, 404, 409 as applicable)
- @Schema(example=...) on every DTO field
- JWT Bearer SecurityScheme in OpenApiConfig
- @SecurityRequirement on protected endpoints

### Database
- UUID primary keys (@GeneratedValue(strategy = GenerationType.UUID))
- @PrePersist sets createdAt + updatedAt
- @PreUpdate sets updatedAt
- Proper JPA annotations (@Column, @Table, @Entity)

### JUnit tests — write all three layers
1. **Service unit tests** — @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks
   - Cover success path and all failure/edge cases per method
2. **Controller slice tests** — @WebMvcTest, @MockitoBean, MockMvc
   - Cover all endpoints: valid request, validation errors, each error status code
   - Use TestSecurityConfig (csrf/formLogin/httpBasic disabled, anyRequest().permitAll())
   - Use @WithMockUser on protected endpoint tests
3. **Utility tests** — e.g. JwtUtilTest using ReflectionTestUtils

### Environment files
- `.env` with real local values (gitignored)
- `.env.example` with placeholder values (committed to git)
- `.gitignore` must include `.env` and `**/.env`

### application.yml structure
- Default profile: local dev (localhost DB, Eureka on 8761)
- Docker profile override: container hostnames
- All secrets via ${ENV_VAR} placeholders
- springdoc disabled by default, enabled via SWAGGER_ENABLED=true

### README.md — required for every service
Each service must have a README.md covering:
- What the service does (1-2 lines)
- Prerequisites (Java version, Docker)
- Setup steps (copy .env, start DB, run service)
- API endpoint table (method, path, auth, description)
- Swagger UI URL
- How to run tests (with test count)
- Environment variables table (variable, description, example)
- Security summary (auth mechanism, rate limiting, notable behaviours)

### Git commit style
- Use conventional commits: feat, fix, docs, test, chore, refactor
- Split swagger changes and test changes into separate commits
- Never commit .env files
