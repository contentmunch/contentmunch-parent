# contentmunch-foundation-spring-boot-starter

A foundational Spring Boot starter module for all `contentmunch-*` projects, providing:

- **Standardized Error Handling**
- **Distributed Tracing & Telemetry** (OpenTelemetry)
- **Structured JSON Logging**

> Built to ensure consistent behavior and observability across all services in the Contentmunch ecosystem.

---

## Features

### Global Exception Handling
- Categorized exceptions: `ClientException`, `BusinessException`, `ServerException`
- Built-in handler via `@ControllerAdvice`
- Returns structured `ErrorResponse` JSON
- Automatically sets `traceId`, `spanId` in response headers (not the body)

### Logging
- JSON-based structured logs
- Includes trace/span IDs for observability
- Uses SLF4J and logback (can be overridden by consuming app)

### Telemetry & Tracing
- OpenTelemetry auto-configuration
- Baggage and context propagation via Spring filters
- Support for exporting spans to a tracing backend (e.g. Jaeger, OTEL Collector)

#### Telemetry architecture

```
                          ┌──────────────────────────┐
                          │        Grafana           │
                          │   (Dashboards + Explore) │
                          └────────────┬─────────────┘
                                       │
             ┌─────────────────────────┼─────────────────────────┐
             │                         │                         │
             ▼                         ▼                         ▼
┌────────────────┐        ┌─────────────────────┐     ┌──────────────────┐
│   Prometheus   │        │ Grafana Tempo/Jaeger│     │  Alertmanager    │
│ (Time-series   │        │   (Trace Storage)   │     │ (Alert Routing)  │
│   Metrics DB)  │        └───────────┬─────────┘     └─────────┬────────┘
└────────┬───────┘                    │                         │
         │                            │                         │
┌────────┴────────────┐        ┌──────┴─────────────┐           │
│ Prometheus Scrape   │        │ OTel Collector     │           │
│ /actuator/prometheus│        │  (OTLP Receiver)   │           │
└────────────┬────────┘        └─────────┬──────────┘           │
             │                           │                      │
┌────────────┼───────────────────────────┼──────────────────────┘
│            │                           │
▼            ▼                           ▼
┌────────────────────────────┐   ┌────────────────────────────┐
│ Spring Boot App A          │   │ Spring Boot App B          │
│ - Micrometer +             │   │ - Micrometer +             │
│   Prometheus               │   │   Prometheus               │
│ - OTel SDK                 │   │ - OTel SDK                 │
│ - /actuator/prometheus     │   │ - /actuator/prometheus     │
│ - Trace Context Propagation│   │ - Trace Context Propagation│
└────────────────────────────┘   └────────────────────────────┘
```

## Getting Started

Add the dependency to your `pom.xml`:

```xml
<dependency>
  <groupId>com.contentmunch</groupId>
  <artifactId>contentmunch-foundation-spring-boot-starter</artifactId>
  <version>0.0.1</version>
</dependency>
```

>The starter auto-configures itself — no setup needed!

## How It Works

### Error Response Format
```json
{
  "errorCode": "USER_NOT_FOUND",
  "errorMessage": "User not found"
}
```
### Exception Categories

The `contentmunch-foundation-spring-boot-starter` defines a clean and extensible exception hierarchy for consistent error handling across services.

| Category            | Class Name            | Typical HTTP Status | When to Use                                                       |
|---------------------|------------------------|----------------------|-------------------------------------------------------------------|
| **Client Exception** | `ClientException`      | 400, 403, 404        | Invalid input, missing fields, unauthorized or forbidden access   |
| **Business Exception** | `BusinessException`   | 422                  | Valid request but unacceptable in the current business context    |
| **Server Exception** | `ServerException`      | 500, 503             | Unexpected runtime issues like database failures or null pointers |
| **Fallback Handler** | `Exception` (generic)  | 500                  | Unhandled or unknown exceptions                                   |

### Best Practices

- Throw `ClientException` for user-induced errors (e.g., invalid credentials).
- Use `BusinessException` for domain rules (e.g., "Plan limit exceeded").
- Reserve `ServerException` for infrastructure/system issues.
- Customize the `ErrorMessage` to return appropriate `errorCode`, `message`, and `HttpStatus`.
