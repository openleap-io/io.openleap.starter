# OpenLeap Starter

Starter modules that are shared across OpenLeap microservices and client applications. This repository provides common APIs, client utilities, service infrastructure, specification templates, and a reusable testing toolkit.

## Modules

- api — common ground for data exchange
  - Purpose: Define shared DTOs and API contracts used across services and clients.
  - What’s inside:
    - `io.openleap.starter.core.api.*` Java types (e.g., `ErrorResponse`, `ErrorCode`).
    - OpenAPI generator setup and mustache templates for generating API interfaces and DTOs.
    - Source of truth for cross-service payload shapes and error models.
  - When to use: Import from services and clients that need consistent request/response models or shared error contracts.

- client — library code used/usable by all clients
  - Purpose: Provide small, framework-agnostic helper libraries for building clients (CLI, desktop, web backends) that interact with OpenLeap services.
  - What’s inside:
    - `ClientProperties` and related configuration helpers.
  - When to use: In any standalone application or service acting as a consumer of OpenLeap APIs.

- service — common library for all microservices in OpenLeap
  - Purpose: Share Spring-centric infrastructure and conventions for building OpenLeap microservices.
  - What’s inside (examples):
    - Auto-configurations: Jackson, security, tracing, auditing, JPA, multi-tenancy helpers.
    - Messaging: RabbitMQ topology, event publisher, idempotency utilities, outbox dispatcher.
    - Web: global exception handling and common filters.
    - Utilities: money helpers, metrics service, etc.
    - Unit tests for core service behaviors.
  - When to use: As a dependency of every OpenLeap microservice to standardize runtime behavior and infrastructure.

- spec — general spec templates
  - Purpose: Curate specs, templates, and guidelines that drive consistency across services.
  - What’s inside:
    - Avro schemas and OpenAPI examples under `src/main/resources`.
    - Developer guidelines (e.g., `spec/microservice-developer-guideline.md`).
    - Architecture compliance examples and CI templates under `spec/acc-core-compliance`.
  - When to use: As a reference and source for generating models/APIs and aligning implementations with OpenLeap standards.

- testkit — shared compliance test library
  - Purpose: Reusable JUnit helpers and ArchUnit rules that validate compliance for microservices.
  - What’s inside:
    - Architecture rules, web compliance tests, messaging test support, identity/tenant test extensions, schema checks.
  - When to use: Add as a test dependency in microservices to get out‑of‑the‑box compliance coverage.

## Quick Start

Build all modules:

```bash
mvn -q -DskipTests clean install
```

Build a single module, for example `service`:

```bash
mvn -q -DskipTests -pl service -am clean install
```

Run tests:

```bash
mvn test
```

## Using the modules

- Microservices should depend on `service` (compile) and `testkit` (test scope).
- Client applications or SDKs should depend on `client` and reuse models from `api`.
- Schemas and guidelines in `spec` inform both `api` generation and service behavior.

## Repository layout

- `api/` — core API contracts and OpenAPI/templating setup
- `client/` — client-side common utilities
- `service/` — microservice infrastructure and conventions
- `spec/` — schemas, guidelines, compliance templates
- `testkit/` — reusable testing toolkit for compliance and integration

## Links

- Service developer guideline: `spec/microservice-developer-guideline.md`
- Spec TODOs and backlog: `spec/microservice-todo.md`
- Testkit usage: `testkit/README.md`
- Core-Service Starter Doku: `docs/core-service-starter.md`
 - Core-Service Starter docs (EN): `docs/core-service-starter.en.md`

## Versioning

Modules are versioned together under the `io.openleap.starter` group. Use the same minor/patch when publishing to ensure compatibility across modules.

## License

Copyright (c) OpenLeap. All rights reserved.
