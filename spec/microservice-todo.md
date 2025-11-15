# Microservice Onboarding ToDo Checklist

Use this checklist when starting a new microservice. It reflects the conventions in `microservice-developer-guideline.md` and the `base` module capabilities.

Legend: [ ] To do, [~] In progress, [x] Done

---

## 1. Bootstrap & Basics
- [ ] Create service skeleton with Maven/Gradle, Java package `io.openleap.<suite>.<service>`.
- [ ] Add dependency on `base` module (api + service as needed).
- [ ] Configure PostgreSQL datasource (Dev: local or Testcontainers; Prod: managed instance).
- [ ] Add Flyway; create baseline migration `V1__init.sql`.
- [ ] Define service identifier (used for queues/DLQs): `<serviceId>`.

## 2. Identity, Tenancy, Security
- [ ] Integrate Keycloak resource server (issuer, audience, JWKS). Add minimal scopes/roles. Document scope naming (TBD organization-wide).
- [ ] Enable/verify `IdentityHttpFilter` (HTTP) and `TraceIdFilter` from base.
- [ ] Verify messaging identity wiring: `MessagingIdentityPostProcessor` + `MessagingIdentityClearingAdvice` via `baseRabbitListenerContainerFactory`.
- [ ] Tenant-per-DB provisioning checklist (per tenant):
  - [ ] Create DB/instance with standard extensions (uuid-ossp/pgcrypto if needed).
  - [ ] Create credentials/secret for tenant; store in secret manager; rotate policy defined.
  - [ ] Run Flyway baseline and all migrations on the tenant DB before enabling traffic.
  - [ ] Register tenant connection in service config/registry (JDBC URL, username/secret ref).
- [ ] Choose connection routing strategy:
  - [ ] Static (one tenant per deployed instance) OR
  - [ ] Dynamic (resolve `DataSource` per request/message by `IdentityHolder.tenantId()`).
- [ ] Standard headers to propagate on HTTP and messaging:
  - [ ] `x-tenant-id`, `x-trace-id`, `x-correlation-id`, `x-causation-id`.
  - [ ] Commands include `x-idempotency-key` for external clients.
- [ ] Method security enabled and enforced on application services/controllers (e.g., `@PreAuthorize`).
- [ ] Service-to-service (S2S) auth:
  - [ ] Configure OAuth2 client credentials for outbound calls (least-privilege scopes).
  - [ ] Propagate correlation + tenant headers on outbound requests.
- [ ] Security error handling returns RFC 7807 (`401 unauthorized`, `403 forbidden`) with `traceId` in extensions.

## 3. Domain & Persistence
- [ ] Model aggregates/entities/value objects per DDD (package-by-domain). Define aggregate boundaries and enforce invariants via intent-revealing methods (no public setters).
- [ ] All JPA entities extend `OlPersistenceEntity` (pk, uuid, version, auditing).
- [ ] Enable JPA auditing and verify `createdBy`/`updatedBy` are populated from `IdentityHolder`.
- [ ] Choose soft delete strategy per entity/domain and implement consistently:
  - [ ] Hibernate annotations (`@SQLDelete`, `@Where`) with `deleted_at`, `deleted_by` columns, OR
  - [ ] Application-level filtering with `deletedAt`, `deletedBy` fields.
- [ ] Write Flyway migrations for all tables/constraints/indexes (include soft-delete columns if used).
- [ ] Repositories & Specifications:
  - [ ] Define minimal `JpaRepository` interfaces for aggregates (include `findByUuid(UUID)` where needed).
  - [ ] Provide common `Specification` helpers for text search and date ranges (e.g., `nameContains`, `createdBetween`).
  - [ ] Use projections for read paths; do not expose entities in controllers.
- [ ] Indexes & constraints:
  - [ ] Add `uuid` unique index for each aggregate table (e.g., `ux_<table>_uuid`).
  - [ ] Add supporting indexes for default sort/filter (e.g., `created_at`).
  - [ ] If soft delete is used, adjust unique constraints with partial unique indexes (Postgres): `CREATE UNIQUE INDEX ux_<table>_<col>_active ON <table>(<col>) WHERE deleted_at IS NULL;`.
- [ ] Optimistic locking:
  - [ ] Ensure `@Version` is mapped; map `ObjectOptimisticLockingFailureException` to RFC 7807 HTTP 409 in `@RestControllerAdvice`.
- [ ] Transactions & Outbox:
  - [ ] Ensure domain writes and outbox insert happen in the same `@Transactional` boundary.
  - [ ] Add an integration test that verifies outbox record is written together with domain changes.

## 4. Read Model (REST)
- [ ] Define REST endpoints for queries only (reads are synchronous).
- [ ] Use DTOs for all responses (do not expose entities). Implement mapping (manual or MapStruct).
- [ ] API versioning via header (e.g., `X-Api-Version: 1`). Document supported versions.
  - [ ] Reject missing/unsupported versions with RFC 7807 (`type: .../missing-api-version` or `.../unsupported-api-version`).
- [ ] Pagination & sorting parameters: `page`, `size`, `sort` (e.g., `field,asc`).
  - [ ] Enforce max page size (default 200). Clamp or return 400; document behavior. If clamping, add header `X-Page-Size-Clamped: true`.
  - [ ] Support multiple `sort` params; default sort by `createdAt,desc` when none provided.
- [ ] Filtering conventions documented (equality, ranges, enums, `q` for text search).
- [ ] Error responses use RFC 7807 Problem+JSON (`application/problem+json`).
  - [ ] Provide `@RestControllerAdvice` using Spring `ProblemDetail` for: validation, constraint violations, not found, conflict (optimistic locking), access denied, method/media type not allowed, missing headers.
  - [ ] Ensure `Content-Type: application/problem+json` is set for errors.
- [ ] Add `@WebMvcTest` suite to assert problem responses and pagination defaults.

## 5. Write Model (Commands via Messaging)
- [ ] Define domain-rich command schemas (may include sensitive fields; encrypt as required). Include field-level encryption plan and key management notes.
- [ ] Define routing keys using `<suite>.<domain>.<aggregate>.<kind>.<action>.v<major>`.
- [ ] Define consumer queue names `<serviceId>.<suite>.<domain>.<aggregate>.<kind>.<action>` and bind to exchange.
- [ ] Create DLQ for each consumer queue using `<queue>.dlq` and bind via a dead-letter exchange (DLX).
- [ ] Implement `@RabbitListener` handlers (use `baseRabbitListenerContainerFactory`).
- [ ] Ensure idempotency in handlers (dedup by `x-idempotency-key` or domain natural keys).
  - [ ] Create `idempotency_keys` table (tenant_id, idem_key PK) and repository helper.
  - [ ] Require `x-idempotency-key` header for external producers; validate/persist per-tenant.
- [ ] Persist domain changes and write outbox record in the SAME transaction.
- [ ] Use `EventPublisher` to enqueue messages; for coverage, set `ol.base.service.messaging.coverage=true` and register expected routing keys with `MessageCoverageTracker` at startup.
- [ ] Document DLQ remediation playbook and operational contacts for replays.

## 6. Informational Events (Notifications)
- [ ] Define minimal payload schemas (type + entityId(s); no extra details).
- [ ] Publish informational events following state changes where useful for other services.
- [ ] Use standard headers for identity/trace; tenant in headers only (unless domain requires in payload).

## 7. Serialization & Schemas
- [ ] Dev/Test default: JSON (no registry).
- [ ] Prod default: Avro + Schema Registry.
- [ ] Place `.avsc` files under `spec/src/main/resources/schemas/`.
- [ ] Schema filenames mirror routing keys: `<suite>.<domain>.<aggregate>.<kind>.<action>.v<major>.avsc`.
- [ ] Enforce compatibility policy (default BACKWARD) in registry.

## 8. Messaging Properties (example)
- [ ] Set base messaging properties and exchange name.
- [ ] Configure retries/backoff and DLQs.

```yaml
ol:
  base:
    service:
      messaging:
        events-exchange: openleap-events
        coverage: false
        registry:
          enabled: ${OL_REGISTRY_ENABLED:false}   # true in prod
          url: ${OL_REGISTRY_URL:http://localhost:8990}
          format: avro
spring:
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
# Listener retry/backoff (illustrative — align with chosen lib/settings)
listener:
  maxAttempts: 5
  backoff:
    initial: 200ms
    multiplier: 2.0
    max: 5s
```

## 9. Observability & Diagnostics
- [ ] Ensure `TraceIdFilter` and logging MDC include `traceId`, `tenantId`, `userId`.
- [ ] Propagate correlation headers on all outbound requests/messages: `x-trace-id`, `x-correlation-id`, `x-causation-id`, `x-tenant-id`.
- [ ] Create OpenTelemetry spans for:
  - [ ] REST operations (controller boundary) with attributes: `ol.endpoint`, `http.method`, `http.status`.
  - [ ] Command handlers (listener boundary) with attributes: `ol.queue`, `ol.routing_key`, `ol.command`, `ol.aggregate`, `ol.tenant_id`.
- [ ] Logging: use structured JSON in prod; include MDC (`traceId`, `tenantId`, `userId`); redact PII.
- [ ] Metrics: define and instrument at minimum:
  - [ ] Command processing latency histogram (`ol_command_duration_seconds`).
  - [ ] Outbox dispatch lag gauge (`ol_outbox_dispatch_lag_seconds`).
  - [ ] Consumer failure counter (`ol_consumer_failures_total`).
  - [ ] REST latency histogram by endpoint/status (`ol_rest_request_duration_seconds`).
- [ ] Alerts/dashboards:
  - [ ] Alert on sustained outbox lag and DLQ growth.
  - [ ] Dashboard showing command rates, failure counts, and p95 latencies.
- [ ] Configuration profiles (observability):
  - [ ] Dev: verbose logging, JSON optional, metrics export optional.
  - [ ] Test/CI: deterministic logs; disable external exporters; keep Micrometer in-memory.
  - [ ] Prod: structured JSON logs; OTEL exporter enabled if available; sampling strategy defined.

## 10. Testing
- [ ] Unit tests for domain logic and invariants.
- [ ] Persistence tests using Testcontainers Postgres + Flyway.
- [ ] Messaging integration tests: outbox insert + dispatcher publish; listener consumption with retries/DLQ behavior.
- [ ] Contract tests: REST (OpenAPI) and Avro schema compatibility (for prod profile).

## 11. Operational Readiness
- [ ] Health/readiness/liveness endpoints implemented and exposed.
- [ ] Runbooks documented in repo (link in README):
  - [ ] DLQ drain & safe replay procedure (pause, inspect, remediate, batch replay, closeout).
  - [ ] Outbox backlog/lag monitoring and remediation.
  - [ ] Incident triage checklist (trace correlation, rollback/toggle, stakeholder comms).
- [ ] Monitoring & alerts configured:
  - [ ] Outbox backlog and oldest undelivered age alerts (thresholds defined per service).
  - [ ] DLQ growth alert and consumer failure rate alert.
  - [ ] REST p95 latency SLO dashboard.
- [ ] Capacity plan validated:
  - [ ] Consumer concurrency/prefetch tuned and documented.
  - [ ] Dispatcher batch size and cadence tuned.
  - [ ] DB pool sizes verified under expected load.
- [ ] Rollout plan: backward-compatible API/events; canary/blue-green as needed; rollback steps defined.

## 12. Acceptance Criteria (Definition of Done)
- [ ] All endpoints return RFC 7807 for errors; success DTOs only.
- [ ] Commands accepted via queues with idempotency, retries configured, and DLQ present.
- [ ] DLQ runbook available and a replay drill performed in staging.
- [ ] Outbox monitoring dashboards and alerts active; thresholds agreed with SRE.
- [ ] Informational events published on relevant state changes (minimal payload).
- [ ] Entities extend `OlPersistenceEntity`; auditing populated from `IdentityHolder`.
- [ ] Soft delete policy selected and implemented consistently.
- [ ] Avro enabled in prod profile; JSON used in local/dev by default.
- [ ] Tests: unit, persistence, messaging, and contracts passing.
- [ ] Observability: trace/log/metrics verified in test/staging.
