# contentmunch-auth-spring-boot-starter

A Spring Boot starter for secure, cookie-based authentication in microservices. Includes prebuilt endpoints for login,
logout, and current user (`/me`) along with built-in distributed tracing and structured logging support
via contentmunch-foundation-spring-boot-starter.

## Features

- Cookie-based authentication using signed JWTs
- Plug-and-play login, logout, and `/me` endpoints
- Secure HTTP-only cookies (configurable)
- Spring Security integration with customizable user identity resolution
- Auto-enriched logs and traces with `traceId`, `userId`, and `requestId`
- Seamless MDC + OpenTelemetry context propagation
- Built-in support for distributed systems

## Getting Started

### 1. Add the dependencies

You need to include the authentication starter **along with** its required dependencies:

#### Maven

```xml

<dependency>
    <groupId>dev.contentmunch</groupId>
    <artifactId>contentmunch-auth-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

```

If you’re using Gradle:

```
implementation("dev.contentmunch:contentmunch-auth-spring-boot-starter:1.0.0")

```

### 2. Enable JWT Cookie Authentication

Out of the box, this starter sets up:
• A secure, HTTP-only cookie with a signed JWT
• Spring Security filter chain that authenticates using that cookie
• /auth/login, /auth/logout, and /auth/me endpoints

💡 Make sure you’re serving over HTTPS in production so the Secure cookie flag works correctly.

### 3. Configure JWT and Cookie Properties

In your application.yml:

```yaml
contentmunch:
  auth:
    jwt:
      secret: your-256-bit-secret
      issuer: contentmunch-auth
      expiration: 3600 # in seconds
    cookie:
      name: CONTENTMUNCH_AUTH
      secure: true
      http-only: true
      same-site: Lax
```

You can override these values as needed.

## Auth APIs

| Endpoint       | Method | Description                           |
|----------------|--------|---------------------------------------|
| `/auth/login`  | POST   | Authenticates a user, sets cookie     |
| `/auth/logout` | POST   | Invalidates cookie                    |
| `/auth/me`     | GET    | Returns details of the logged-in user |

Example: POST /auth/login

```http request
POST /auth/login
Content-Type: application/json

{
  "username": "alice@example.com",
  "password": "password123"
}
```

Response:
• 200 OK: Sets an HttpOnly cookie with the JWT
• 401 Unauthorized: Invalid credentials

Example: GET /auth/me

```http request
GET /auth/me
Cookie: CONTENTMUNCH_AUTH=<your-jwt>
```

Response:

```json
{
  "username": "alice@example.com",
  "roles": [
    "USER"
  ]
}
```

## Integration with Logging & Tracing
This starter automatically integrates with:
• contentmunch-logging-spring-boot-starter: Logs include traceId, and spanId via MDC
• contentmunch-telemetry-spring-boot-starter: Trace context and baggage are automatically propagated across services

You can trace a request across systems and correlate logs using the X-Trace-Id header and the userId set via
authentication.

## Publishing And Versioning

This starter follows [Semantic Versioning](https://semver.org) and is meant to be used with the contentmunch-parent BOM (Bill
of Materials) for consistent dependency alignment.



