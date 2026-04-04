# ADR-009: Observability — Tracing & Structured Logging

**Status:** Accepted

## Context

In a distributed microservice architecture, correlating logs across service boundaries is essential for debugging and monitoring. Each request must carry a trace ID that flows through HTTP calls, message processing, and log statements. The system needs OpenTelemetry integration for distributed tracing and MDC-based structured logging for log correlation.

## Decision

The starter provides a `TraceIdFilter` that populates the SLF4J MDC with a `traceId` for every HTTP request. When OpenTelemetry is enabled, the trace ID is sourced from the active span; otherwise, a UUID is generated. Configuration is managed via `TracingProperties` (a proper `@ConfigurationProperties` class, not `@Value` injection).

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `TraceIdFilter` | `io.openleap.common.http.telemetry.TraceIdFilter` | Servlet filter that sets `traceId` in MDC for every request |
| `OtelConfig` | `io.openleap.common.http.telemetry.OtelConfig` | OpenTelemetry SDK configuration |
| `TracingProperties` | `io.openleap.common.http.telemetry.TracingProperties` | Configuration properties (`@ConfigurationProperties(prefix = "ol.tracing")`) |

### How It Works

```
1. HTTP request arrives
2. TraceIdFilter executes:
   a. If OpenTelemetry is active → extract traceId from current span
   b. Otherwise → generate a UUID as traceId
3. traceId is placed in SLF4J MDC
4. All log statements within the request include traceId
5. MDC is cleared after the request completes
```

## Usage

### Enabling Telemetry

```java
@SpringBootApplication
@EnableOpenLeapTelemetry
public class MyServiceApplication { }
```

### Logging with Trace ID

No code changes needed — `traceId` is automatically available in MDC:

```java
@RestController
class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable String id) {
        log.info("Fetching order {}", id);
        // Log output includes traceId automatically
        return orderService.findById(id);
    }
}
```

### Logback Pattern Configuration

Include `traceId` in your log pattern:

```xml
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n</pattern>
```

### JSON Structured Logging

For production deployments, use a JSON encoder that automatically includes MDC fields:

```xml
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>
```

The `traceId` MDC field will appear in every JSON log entry.

## Configuration

```yaml
ol:
  tracing:
    otel:
      enabled: ${OL_OTEL_ENABLED:false}       # default: false
      endpoint: ${OL_OTEL_ENDPOINT:http://localhost:4317}  # default: http://localhost:4317
```

### TracingProperties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `ol.tracing.otel.enabled` | `boolean` | `false` | Enable OpenTelemetry SDK |
| `ol.tracing.otel.endpoint` | `String` | `http://localhost:4317` | OTLP collector endpoint (gRPC) |

## Compliance Rules

1. `@EnableOpenLeapTelemetry` MUST be present on the application class.
2. All services MUST include `traceId` in their log pattern.
3. Production deployments SHOULD enable OpenTelemetry (`ol.tracing.otel.enabled=true`) with a collector endpoint.
4. Log patterns MUST use `%X{traceId}` (Logback) or equivalent to include the trace ID.
5. Services MUST NOT generate or manage trace IDs manually — the `TraceIdFilter` handles this.
6. JSON structured logging SHOULD be used in production for machine-parseable logs.
7. The OTLP endpoint MUST be reachable when OpenTelemetry is enabled.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Generating trace IDs manually in application code | Let `TraceIdFilter` handle it automatically |
| Using `@Value("${ol.tracing.otel.enabled}")` for configuration | Use `TracingProperties` which is a proper `@ConfigurationProperties` class |
| Logging without trace ID in the pattern | Include `%X{traceId}` in all log patterns |
| Enabling OpenTelemetry without a reachable collector | Ensure the OTLP endpoint is configured and reachable |
| Using plain text logs in production | Use JSON structured logging for log aggregation systems |
| Passing trace IDs as method parameters | Read from MDC (`MDC.get("traceId")`) if needed programmatically |
