# IAM Authorization Integration Guide

This guide explains how any service in the openleap platform integrates with IAM for authorization decisions. The `io.openleap.template` project is used as the reference implementation throughout.

<!-- TOC -->
* [IAM Authorization Integration Guide](#iam-authorization-integration-guide)
  * [Architecture Overview](#architecture-overview)
  * [What Is in the JWT](#what-is-in-the-jwt)
  * [Step-by-Step Integration](#step-by-step-integration)
    * [1. Add the Starter Dependency](#1-add-the-starter-dependency)
    * [2. Register the IAM Package Marker in Component Scan](#2-register-the-iam-package-marker-in-component-scan)
    * [3. Configure the IAM Authz URL](#3-configure-the-iam-authz-url)
    * [4. Set the Security Mode](#4-set-the-security-mode)
    * [5. Configure Spring Security for Multi-Realm Keycloak](#5-configure-spring-security-for-multi-realm-keycloak)
    * [6. Identity Extraction at Request Time](#6-identity-extraction-at-request-time)
    * [7. Making Authorization Decisions](#7-making-authorization-decisions)
      * [Option A — Annotation-driven (simple permission check)](#option-a--annotation-driven-simple-permission-check)
      * [Option B — Programmatic check with resource and context](#option-b--programmatic-check-with-resource-and-context)
  * [Permission and Policy Provisioning](#permission-and-policy-provisioning)
    * [Permissions — service-owned, registered at startup](#permissions--service-owned-registered-at-startup)
    * [Roles and Policies — provisioned at tenant onboarding](#roles-and-policies--provisioned-at-tenant-onboarding)
    * [Role Assignments — owned by user management, not the service](#role-assignments--owned-by-user-management-not-the-service)
    * [Ownership Summary](#ownership-summary)
  * [Resource-Level Grants and Sharing](#resource-level-grants-and-sharing)
    * [Current recommendation: service-local ACL table](#current-recommendation-service-local-acl-table)
    * [Future path: OpenFGA for relationship-based access control](#future-path-openfga-for-relationship-based-access-control)
    * [When to use each approach](#when-to-use-each-approach)
  * [How the IAM HTTP Call Works](#how-the-iam-http-call-works)
    * [Request / Response Schema](#request--response-schema)
  * [Local Development and Testing](#local-development-and-testing)
  * [Checklist for a New Service](#checklist-for-a-new-service)
<!-- TOC -->
---

## Architecture Overview

```
Client
  │
  ▼
API Gateway  ──►  Keycloak (authentication, multi-realm)
  │               Issues JWT with principalId + tenantId
  ▼
Microservice
  │
  ├── Spring Security (validates JWT signature via Keycloak JWK set)
  ├── IdentityHttpFilter (extracts principalId, tenantId, roles, scopes into thread-local)
  └── AuthorizationService ──► IAM Authz Service (POST /api/v1/iam/authz/check)
```

**Key responsibilities:**
- **Keycloak**: authentication only — verifies identity, issues signed JWTs
- **Gateway**: routes authenticated requests downstream, enforces HTTPS, handles multi-realm routing
- **Service**: validates JWT, extracts identity, then calls IAM for every authorization decision
- **IAM Authz Service**: the policy decision point — evaluates permissions based on userId, permission code, optional resourceId, and optional context

---

## What Is in the JWT

The JWT issued by Keycloak contains IAM-domain identity claims that the service relies on:

| Claim | Aliases | Type | Description                                                            |
|---|---|---|------------------------------------------------------------------------|
| `tenantId` | `tenantid`, `tenant_id` | UUID | The tenant the user belongs to — custom claim added by Keycloak mapper |
| `userId` | `userid`, `user_id`, `sub`, `subject` | UUID | The Keycloak user ID (`sub` is the standard OIDC subject claim)        |
| `principalId` | `principal_id` | UUID | The IAM domain principal ID — custom claim added by Keycloak mapper    |
| `roles` | — | string / array | Granted roles (used for admins only)                                   |
| `scope` / `scopes` | — | space-delimited string or array | OAuth2 scopes                                                          |

`tenantId` and `principalId` are not standard OIDC — they are custom claims added via Keycloak protocol mappers from IAM domain data. `userId` resolves to the standard `sub` claim (Keycloak's own user identifier) when no explicit `userId` claim is present.

---

## Step-by-Step Integration

### 1. Add the Starter Dependency

The IAM module ships inside `io.openleap.core:core-service` (via `io.openleap.core:starter`). Pull it in via the parent BOM — no explicit version needed:

```xml

<dependency>
    <groupId>io.openleap.coreio.openleap.core</groupId>
    <artifactId>core-service</artifactId>
</dependency>
```

### 2. Register the IAM Package Marker in Component Scan

The starter ships beans in package `io.openleap.core.iam`. Because your application's `@SpringBootApplication` only scans its own package by default, you must explicitly include the IAM marker:

```java
@SpringBootApplication
@ComponentScan(basePackageClasses = {
        YourServiceApplication.class,
        SecurityPackageMarker.class,
        IamAuthzPackageMarker.class,
        // ... other markers
})
public class YourServiceApplication { ... }
```

`IamAuthzPackageMarker` is a no-op interface in `io.openleap.core.iam` that anchors component scanning to that package. Without it, `AuthorizationService`, `PermissionCheckAspect`, `IamAuthzClientConfig`, and related beans will not be loaded.

### 3. Configure the IAM Authz URL

In `application.yml` (default / local):

```yaml
iam:
  authz:
    url: ${IAM_AUTHZ_URL:http://localhost:8082}
```

In Kubernetes (`application-k8s.yml`):

```yaml
iam:
  authz:
    url: ${IAM_AUTHZ_URL:http://iam-authz-svc:8082}
```

`IamAuthzClientConfig` reads this property and builds the `RestClient` + `IamAuthzClient` proxy automatically.

### 4. Set the Security Mode

The `IdentityHttpFilter` behaves differently depending on `ol.security.http.mode`:

| Mode | Behavior |
|---|---|
| `iamsec` | Extracts identity from the validated JWT in Spring Security context |
| `nosec` | Extracts identity from plain headers (`X-Tenant-Id`, `X-User-Id`, etc.) — for local dev/test only |

In Kubernetes / production (`application-k8s.yml`):

```yaml
ol:
  security:
    http:
      mode: iamsec
```

### 5. Configure Spring Security for Multi-Realm Keycloak

The gateway is multi-tenant — different users may belong to different Keycloak realms. The starter provides `SecurityKeycloakConfig` (activated with the `keycloak` Spring profile) that handles this dynamically using `JwtIssuerAuthenticationManagerResolver`:

```java
// From SecurityKeycloakConfig in the starter:
@Bean
public JwtIssuerAuthenticationManagerResolver authenticationManagerResolver() {
    Map<String, AuthenticationManager> managers = new ConcurrentHashMap<>();
    return new JwtIssuerAuthenticationManagerResolver(issuer -> {
        if (!issuer.startsWith(keycloakBaseUrl + "/realms/")) {
            throw new IllegalArgumentException("Untrusted issuer: " + issuer);
        }
        return managers.computeIfAbsent(issuer, i -> {
            JwtDecoder decoder = JwtDecoders.fromIssuerLocation(i);
            JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
            provider.setJwtAuthenticationConverter(customJwtAuthenticationConverter());
            return provider::authenticate;
        });
    });
}
```

This validates any realm under the configured `keycloak.server-url` and caches decoders per issuer. You must provide `keycloak.server-url` in your config (typically from Config Server when using the `keycloak` profile).

Your service's own `SecurityConfig` wires in the resolver for its API paths:

```java
@Profile({"keycloak"})
@Configuration("templateSecurityConfig")
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain iamPrincipalFilterChain(
            HttpSecurity http,
            JwtIssuerAuthenticationManagerResolver authenticationManagerResolver) throws Exception {
        http.securityMatcher("/api/v1/template/**")
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api/v1/template/**").authenticated())
                .oauth2ResourceServer(oauth2 ->
                        oauth2.authenticationManagerResolver(authenticationManagerResolver));
        return http.build();
    }
}
```

### 6. Identity Extraction at Request Time

`IdentityHttpFilter` runs at `HIGHEST_PRECEDENCE + 10` (after any trace filter) on every request. In `iamsec` mode:

1. Reads the `JwtAuthenticationToken` from the Spring Security context (already validated by Spring Security).
2. Extracts `tenantId`, `userId`, `principalId`, `roles`, and `scope` claims.
3. Stores them in `IdentityHolder` — a set of `ThreadLocal` values scoped to the request lifecycle.
4. Bridges values to MDC so they appear in structured logs automatically.
5. Clears `IdentityHolder` and MDC in a `finally` block after the filter chain completes, preventing cross-request leakage in thread pools.

```java
// IdentityHolder — read anywhere in the request thread:
UUID tenantId    = IdentityHolder.getTenantId();
UUID userId      = IdentityHolder.getUserId();
UUID principalId = IdentityHolder.getPrincipalId();
Set<String> roles = IdentityHolder.getRoles();
```

### 7. Making Authorization Decisions

#### Option A — Annotation-driven (simple permission check)

Annotate a controller or service method with `@RequiresPermission`. The `PermissionCheckAspect` intercepts it before the method body executes:

```java
@GetMapping("/{id}")
@RequiresPermission("FI_ARRR_INVOICE_CREATE")
public ResponseEntity<ResourceDto> getResource(@PathVariable UUID id) {
    return ResponseEntity.ok(new ResourceDto(id, "Example Resource"));
}
```

Internally the aspect calls `authorizationService.check(permission)` using `IdentityHolder.getPrincipalId()` (the IAM principal ID) as the userId sent to IAM Authz. If IAM returns `allowed: false`, `PermissionDeniedException` is thrown before the method runs.

#### Option B — Programmatic check with resource and context

Inject `AuthorizationService` and call `check` directly when you need to pass a resource ID or additional context attributes:

```java
@GetMapping("/{id}/approve")
public ResponseEntity<ResourceDto> approveResource(@PathVariable UUID id) {
    authorizationService.check(
            "FI_ARR_INVOICE_CREATE",
            null,                          // resourceId — null for non-resource-scoped permissions
            Map.of("department", "FINANCE") // context — arbitrary key/value pairs for policy evaluation
    );
    return ResponseEntity.ok(new ResourceDto(id, "Approved Resource"));
}
```

This overload uses `IdentityHolder.getUserId()` (the Keycloak `sub`) as the userId sent to IAM Authz. Use this form when policies depend on attributes beyond the identity (e.g., resource owner, department, status).

---

## Permission and Policy Provisioning

Each service in the platform is responsible for provisioning its own IAM artifacts. The lifecycle is split into three concerns with different owners and trigger points.

### Permissions — service-owned, registered at startup

Permission codes (e.g., `DMS_DOC_DOCUMENT_READ`) are part of the service's own domain definition. Only the service knows what actions exist on its resources. IAM stores them globally but does not define them.

Register permissions idempotently on application startup using an `ApplicationRunner` (or `@PostConstruct`). Treat this the same way you treat Flyway migrations — run on every startup, skip gracefully if already present (IAM returns 409 on duplicates).

```java
@Component
public class PermissionBootstrap implements ApplicationRunner {

    private final IamAuthzProvisioningClient iamClient;

    @Override
    public void run(ApplicationArguments args) {
        iamClient.registerPermissionIfAbsent("DMS_DOC_DOCUMENT_READ", "dms.doc.document", "READ", "TENANT");
        iamClient.registerPermissionIfAbsent("DMS_DOC_DOCUMENT_WRITE", "dms.doc.document", "WRITE", "TENANT");
    }
}
```

Permission codes follow the pattern `{SUITE}_{DOMAIN}_{ENTITY}_{ACTION}` (uppercase). Resource identifiers follow `suite.domain.entity` (lowercase dot-separated).

### Roles and Policies — provisioned at tenant onboarding

Roles and ABAC policies are tenant-scoped. They must be created **once per tenant** when a tenant is onboarded into the service — not per request, not per user. Tenant onboarding is the correct trigger point.

In the service's tenant creation handler, after persisting the tenant locally, call IAM to provision the tenant's authorization structure:

```java
// Inside TenantService.create():
iamProvisioningClient.createRole(tenantId, "DMS_TENANT_ADMIN", List.of(readPermissionId, writePermissionId));
iamProvisioningClient.activateRole(tenantId, roleId);
iamProvisioningClient.createPolicy(tenantId, "DMS_DOC_OWNER_READ", ownerConditionExpr, "ALLOW", 10);
iamProvisioningClient.activatePolicy(tenantId, policyId);
```

This ensures every tenant has a consistent authorization baseline from the moment they are created.

**ABAC owner policies** that compare two context fields (e.g., `uploadedBy == requestUserId`) require the `$ref` syntax in the condition expression:

```json
{
  "field": "uploadedBy",
  "operator": "equals",
  "value": { "$ref": "requestUserId" }
}
```

The service then passes the required fields in `context` when calling `/check` at runtime.

### Role Assignments — owned by user management, not the service

Which users get which roles is **not the responsibility of the consumer service**. Role assignment is an administrative concern handled by a user management or onboarding flow that calls IAM directly:

```
POST /api/v1/iam/authz/roles/{roleId}/assignments
{ "userId": "...", "validFrom": "..." }
```

The consumer service never creates role assignments. It only calls `/check` to evaluate whether a user already has the required permission.

### Ownership Summary

| Artifact | Owner | Trigger |
|---|---|---|
| Permissions | Consumer service | Application startup (idempotent bootstrap) |
| Roles | Consumer service | Tenant onboarding |
| ABAC policies | Consumer service | Tenant onboarding |
| Role assignments | Admin / identity management flow | User onboarding / role management UI |

---

## Resource-Level Grants and Sharing

RBAC and ABAC policies cover tenant-wide and general-rule authorization. They are not suited for **per-resource, per-user grants** such as sharing a specific document with a specific user. Attempting to model these as IAM policies creates one policy per share, exhausts the priority space (1–1000 per tenant), and abuses a mechanism designed for general rules.

### Current recommendation: service-local ACL table

For explicit sharing (e.g., user-a shares document-X with user-b), the consumer service maintains its own row-level ACL table. The grant is written at share time and checked as a fallback when IAM denies:

```
GET /documents/{id}
  │
  ├─ 1. POST /iam/authz/check { userId, permission, resourceId, context: { uploadedBy, requestUserId } }
  │       ├─ Admin (RBAC)  → ✅ allow
  │       └─ Owner (ABAC)  → ✅ allow
  │
  ├─ 2. IAM denied → query local ACL: scope=Document, scopeRef=documentId, subjectId=userId
  │       └─ explicit share entry found → ✅ allow
  │
  └─ 3. Still denied → 403
```

On share, insert a local ACL entry:

```java
AccessControl share = new AccessControl();
share.setSubjectId(userBId);
share.setSubjectType(SubjectType.User);
share.setPermission(Permission.Read);
share.setScope(AccessScope.Document);
share.setScopeRef(documentId);
accessControlRepository.save(share);
```

### Future path: OpenFGA for relationship-based access control

When sharing requirements grow beyond simple per-document grants — hierarchical sharing (share a folder, all documents inherit), transitive relationships (member of group → viewer of all group documents), or cross-service resource grants — the right extension is **OpenFGA** integrated into IAM as a third evaluation layer.

OpenFGA models authorization as relationship tuples. The DMS example maps naturally:

```
Authorization model:
  type document
    relations
      define owner: [user]
      define viewer: [user] or owner

Tuples:
  (user:user-a, owner,  document:doc-uuid)   ← written on document creation
  (user:user-b, viewer, document:doc-uuid)   ← written on share
```

The existing `resourceId` field on the `/check` request is the OpenFGA object identifier — no API contract change is needed. The `AuthorizationStrategy` pattern in IAM is designed for pluggable evaluation layers; OpenFGA becomes a fourth step in the decision chain:

```
1. ABAC DENY          → denied immediately
2. RBAC allow         → allowed (admin)
3. ABAC ALLOW         → allowed (owner policy)
4. OpenFGA check      → allowed (explicit share / relationship)
5. else               → denied
```

Tuple writes can be driven by service domain events (RabbitMQ outbox) rather than direct IAM calls, keeping the consumer service decoupled from IAM's internal FGA store.

### When to use each approach

| Scenario | Approach |
|---|---|
| Tenant-wide role (admin accesses all resources) | IAM RBAC |
| General ownership rule (creator can access own resource) | IAM ABAC policy |
| Explicit share with a specific user on a specific resource | Service-local ACL table |
| Hierarchical / transitive / cross-service relationships | OpenFGA in IAM (future) |

---

## How the IAM HTTP Call Works

`IamAuthzClientConfig` constructs an `IamAuthzClient` proxy backed by `RestClient` (Spring 6 HTTP interfaces):

```java
@HttpExchange("/api/v1/iam/authz")
public interface IamAuthzClient {
    @PostExchange("/check")
    CheckPermissionResponseDto check(@RequestBody CheckPermissionRequestDto request);
}
```

The `RestClient` is configured with a request interceptor that forwards the caller's JWT:

```java
RestClient.builder()
    .baseUrl(baseUrl)
    .requestInterceptor((request, body, execution) -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION,
                    "Bearer " + jwtAuth.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    })
    .build();
```

The IAM Authz Service receives the original user JWT, so it can apply any additional token-based policy logic.

### Request / Response Schema

**POST** `{iam.authz.url}/api/v1/iam/authz/check`

Request body (`CheckPermissionRequestDto`):
```json
{
  "userId": "uuid",
  "permission": "FI_ARRR_INVOICE_CREATE",
  "resourceId": "uuid | null",
  "context": { "key": "value" }
}
```

Response body (`CheckPermissionResponseDto`):
```json
{
  "allowed": true,
  "reason": "policy matched",
  "source": "policy-engine",
  "evaluationTimeMs": 3
}
```

If `allowed` is `false`, `AuthorizationService` throws `PermissionDeniedException` carrying the full response (including `reason`).

---

## Local Development and Testing

For local development without a live Keycloak, set:

```yaml
ol:
  security:
    http:
      mode: nosec
```

In `nosec` mode, `IdentityHttpFilter` reads identity from plain HTTP headers instead of a JWT. In integration tests (`AbstractIT`), identity is injected via `NosecHeaderFilter` headers:

```
X-Tenant-Id: <uuid>
X-User-Id: <uuid>
X-Roles: ROLE_USER
```

This allows tests to run without Keycloak or IAM Authz Service running.

---

## Checklist for a New Service

- [ ] Include `io.openleap.core:core-service` in `pom.xml`
- [ ] Add `IamAuthzPackageMarker.class` and `SecurityPackageMarker.class` to `@ComponentScan`
- [ ] Set `iam.authz.url` in `application.yml` (and environment-specific overrides)
- [ ] Set `ol.security.http.mode: iamsec` in production/k8s config
- [ ] Activate the `keycloak` Spring profile in Kubernetes/production
- [ ] Provide `keycloak.server-url` (via Config Server or env var)
- [ ] Define a service-specific `SecurityConfig` (`@Profile("keycloak")`) to protect your API paths
- [ ] Use `@RequiresPermission("PERMISSION_CODE")` for simple method-level guards
- [ ] Use `authorizationService.check(permission, resourceId, context)` when context-aware policies are needed
- [ ] In tests, use `nosec` mode and inject identity via headers through `NosecHeaderFilter`
- [ ] Implement an `ApplicationRunner` to register service permissions idempotently on startup
- [ ] Provision tenant roles and ABAC policies inside the tenant creation handler (not at request time)
- [ ] Never create role assignments from the consumer service — delegate to user management flows
- [ ] When using ABAC owner policies, pass the required context fields in every `/check` call (e.g., `uploadedBy`, `requestUserId`)
- [ ] For per-resource sharing, maintain a service-local ACL table and check it as a fallback after IAM denies
- [ ] Do not model per-resource shares as IAM policies — consider OpenFGA when relationship complexity grows
