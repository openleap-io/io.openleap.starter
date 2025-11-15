# Event Schemas and Serialization Modes (Guideline Section 4)

This service follows environment-driven serialization:

- Dev/Test (default): JSON payloads, no schema registry
  - `contentType=application/json`
  - Property defaults:
    - `ol.base.service.messaging.registry.enabled=false`
    - `ol.base.service.messaging.registry.format=json`
- Prod (opt-in): Avro + Schema Registry
  - Enable with: `ol.base.service.messaging.registry.enabled=true`
  - Set registry URL: `ol.base.service.messaging.registry.url=http://<registry-host>:<port>`
  - `contentType=application/*+avro` (managed by the converter)

Schema files live here:
- spec/src/main/resources/schemas/
  - fi.acc.journal.event.posted.v1.avsc
  - fi.acc.account.event.status_changed.v1.avsc
  - fi.acc.ledger.event.snapshot_created.v1.avsc

Naming convention
- `<suite>.<domain>.<aggregate>.<kind>.<action>.v<major>.avsc`
- Mirrors routing keys (`<suite>.<domain>.<aggregate>.<event|command>`)

Contract shape (informational events)
- Envelope fields (carried in headers or payload depending on converter):
  - eventId (UUID), traceId, tenantId, occurredAt (timestamptz), producer, schemaRef (when JSON), payload (minimal)
- Minimal payload structure (guideline):
  ```json
  {
    "aggregateType": "fi.acc.<aggregate>",
    "changeType": "created|updated|deleted|statusChanged",
    "entityIds": ["<uuid>", "<uuid>"],
    "version": 123
  }
  ```

How to switch formats
- JSON default is already applied in `service/src/main/resources/application.yml`:
  ```yaml
  ol:
    base:
      service:
        messaging:
          registry:
            enabled: false
            format: json
  ```
- To enable Avro + Registry (Prod), set:
  ```yaml
  ol:
    base:
      service:
        messaging:
          registry:
            enabled: true
            url: http://registry:8990
  ```

Notes
- Compatibility policy: BACKWARD (default). Breaking changes require a new major version in the file name (e.g., `...v2.avsc`).
- When JSON is used (Dev/Test), publish `contentType=application/json`; when Avro is enabled, converter sets `application/*+avro` and manages registry subject/version.
