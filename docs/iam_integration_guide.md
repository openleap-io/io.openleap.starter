# IAM Authorization Integration Guide

This guide explains how any service in the openleap platform integrates with IAM for authorization decisions. The `io.openleap.template` project is used as the reference implementation throughout.

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
