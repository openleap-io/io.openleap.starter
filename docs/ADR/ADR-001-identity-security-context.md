# ADR-001: Identity & Security Context

**Status:** Accepted

## Context

Microservices in a multi-tenant environment need a consistent way to identify the calling user and tenant. The system must support two deployment modes: a development/internal mode where identity is passed via plain HTTP headers, and a production mode using JWT-based authentication via an IAM provider (Keycloak/OAuth2).

## Decision

The core-service starter provides a dual-mode security system controlled by separate properties for HTTP and messaging channels.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `SecurityProperties` | `io.openleap.common.http.security.config.SecurityProperties` | Configuration properties for security modes |
| `IdentityHolder` | `io.openleap.common.http.security.identity.IdentityHolder` | Thread-local holder for the current identity context |
| `IdentityContext` | `io.openleap.common.http.security.identity.IdentityContext` | Record holding identity data (tenantId, userId, roles, scopes) |
| `@AuthenticatedIdentity` | `io.openleap.common.http.security.identity.AuthenticatedIdentity` | Parameter annotation for injecting identity into controller methods |
| `IdentityContextArgumentResolver` | `io.openleap.common.http.security.identity.IdentityContextArgumentResolver` | Resolves `@AuthenticatedIdentity` parameters |
| `IdentityHttpFilter` | `io.openleap.common.http.security.identity.IdentityHttpFilter` | Extracts identity from HTTP requests and populates `IdentityHolder` |
| `NosecHeaderFilter` | `io.openleap.common.http.security.NosecHeaderFilter` | Filter for nosec mode identity headers |
| `SecurityKeycloakConfig` | `io.openleap.common.http.security.SecurityKeycloakConfig` | Keycloak/OAuth2 resource server configuration for iamsec mode |
| `CustomJwtGrantedAuthoritiesConverter` | `io.openleap.common.http.security.CustomJwtGrantedAuthoritiesConverter` | Converts JWT claims to Spring Security granted authorities |
| `JwtUtils` | `io.openleap.common.http.security.JwtUtils` | Utility for extracting claims from JWT tokens |

### Security Modes

| Mode | Channel | Description |
|------|---------|-------------|
| `nosec` | HTTP | Identity extracted from plain headers: `X-Tenant-Id`, `X-User-Id`, `X-Scopes`, `X-Roles` |
| `nosec` | Messaging | Identity extracted from AMQP headers: `x-tenant-id`, `x-user-id` |
| `iamsec` | HTTP | JWT-based authentication via `Authorization: Bearer <token>` or `X-JWT` header |
| `iamsec` | Messaging | JWT extracted from `x-jwt` AMQP message header |

## Usage

### Reading Identity via `IdentityHolder` (Programmatic)

```java
@Service
class OrderService {

    public List<Order> findMyOrders() {
        UUID tenantId = IdentityHolder.getTenantId();
        UUID userId = IdentityHolder.getUserId();
        Set<String> roles = IdentityHolder.getRoles();
        return orderRepository.findByTenantAndUser(tenantId, userId);
    }
}
```

### Injecting Identity via `@AuthenticatedIdentity` (Controller Parameter)

```java
@RestController
class OrderController {

    @GetMapping("/orders")
    public List<Order> getOrders(@AuthenticatedIdentity IdentityContext identity) {
        // identity.tenantId(), identity.userId(), identity.roles(), identity.scopes()
        return orderService.findByTenant(identity.tenantId());
    }
}
```

### Enabling Security

```java
@SpringBootApplication
@EnableOpenLeapSecurity
public class MyServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyServiceApplication.class, args);
    }
}
```

## Configuration

```yaml
ol:
  security:
    http:
      mode: nosec        # nosec | iamsec (default: nosec)
    messaging:
      mode: nosec        # nosec | iamsec (default: nosec)
```

For `iamsec` mode, standard Spring Security OAuth2 resource server properties apply:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://keycloak.example.com/realms/my-realm
```

## Compliance Rules

1. Every service MUST set `ol.security.http.mode` and `ol.security.messaging.mode` explicitly.
2. Production deployments MUST use `iamsec` mode.
3. Controller methods that need identity MUST use `@AuthenticatedIdentity IdentityContext` parameter annotation or `IdentityHolder` — never parse tokens manually.
4. Services using `iamsec` mode MUST configure `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
5. The `@EnableOpenLeapSecurity` annotation MUST be present on the application class.
6. Identity headers (`X-Tenant-Id`, etc.) MUST NOT be trusted in production — use `iamsec`.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Parsing JWT manually in controllers | Use `@AuthenticatedIdentity IdentityContext` or `IdentityHolder` |
| Using `nosec` in production | Switch to `iamsec` with proper IAM provider |
| Storing `IdentityHolder` values in fields | Always read from `IdentityHolder` at point of use — it is thread-local |
| Passing identity through custom headers alongside JWT | The starter extracts identity from JWT automatically in `iamsec` mode |
| Setting only `ol.security.mode` (flat) | Use `ol.security.http.mode` and `ol.security.messaging.mode` separately |
