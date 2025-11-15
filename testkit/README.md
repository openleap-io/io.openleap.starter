# OpenLeap Base Compliance Testkit

Reusable helpers and rules to automatically verify compliance with the OpenLeap microservice guideline.

This module is intended to be added as a test dependency in new/existing services. It provides:
- ArchUnit rules for architecture and persistence conventions.
- WebMvc helpers to assert RFC 7807 Problem+JSON responses.
- Identity test extension (`@WithTenant`) to seed/clear `IdentityHolder`.
- RabbitMQ test harness (Testcontainers-based) to validate headers, DLQ, and routing.
- Avro schema checks to validate file naming mirroring routing keys and parse schemas.

## How to use in a service

Add dependency (pom.xml):
```xml
<dependency>
  <groupId>io.openleap.base</groupId>
  <artifactId>core-testkit</artifactId>
  <version>${openleap.base.version}</version>
  <scope>test</scope>
</dependency>
```

### 1) ArchUnit rules

```java
import com.tngtech.archunit.core.domain.JavaClasses;
import io.openleap.starter.testkit.ArchitectureRules;
import org.junit.jupiter.api.Test;

class ArchitectureComplianceTest {
    @Test
    void entities_extend_base_and_not_exposed() {
        JavaClasses classes = ArchitectureRules.load("io.openleap.<suite>.<service>");
        ArchitectureRules.entitiesExtendOlPersistenceEntity().check(classes);
        ArchitectureRules.controllersDoNotReturnEntities().check(classes);
    }
}
```

### 2) REST + RFC 7807

```java

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
class ProblemComplianceTest {
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @org.junit.jupiter.api.Test
    void missing_version_returns_problem_json() throws Exception {
        mvc.perform(get("/customers"))
                .andExpect(status().isBadRequest())
                .andExpect(problemJsonContentType())
                .andExpect(problemType("https://openleap.io/problems/missing-api-version"));
    }
}
```

### 3) Identity seeding in tests

```java
import io.openleap.starter.testkit.identity.WithTenant;

class AuditingTest {
    @WithTenant(tenant = "00000000-0000-0000-0000-000000000001")
    @org.junit.jupiter.api.Test
    void createdBy_is_populated() {
        // persist entity and assert auditing via repository
    }
}
```

### 4) RabbitMQ harness & header checks

```java
import io.openleap.starter.testkit.messaging.RabbitTestSupport;

class MessagingComplianceTest {
    @org.junit.jupiter.api.Test
    void producer_sets_standard_headers() throws Exception {
        try (RabbitTestSupport rabbit = new RabbitTestSupport().startContainer().connect()) {
            String exchange = "openleap-events";
            String queue = "svc.acc.account.customer.command.create";
            String dlx = "openleap-dlx";
            String rk = "acc.account.customer.command.create.v1";
            rabbit.declareTopicExchange(exchange)
                    .declareQueueWithDlq(queue, dlx)
                    .bind(exchange, queue, rk)
                    .publishJson(exchange, rk, "{}", java.util.Map.of(
                            "x-tenant-id", "t",
                            "x-trace-id", "tr",
                            "x-correlation-id", "co",
                            "x-causation-id", "ca",
                            "x-idempotency-key", "idem"
                    ));
            var msg = rabbit.getOne(queue, 2000);
            assert msg != null;
            RabbitTestSupport.assertStandardHeadersPresent(msg.headers(), true);
        }
    }
}
```

### 5) Avro schema checks

```java
import io.openleap.starter.testkit.schema.SchemaChecks;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SchemaComplianceTest {
    @org.junit.jupiter.api.Test
    void filenames_follow_routing_key_pattern() {
        var violations = SchemaChecks.validateFilenames(Path.of("spec/src/main/resources/schemas"));
        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }
}
```

## Notes
- Testcontainers dependencies are marked as `provided` in the testkit; add them with `test` scope in your service if you use the Rabbit harness.
- The routing key regex used by the kit is:
```
^(?<suite>[a-z0-9]+)\.(?<domain>[a-z0-9]+)\.(?<aggregate>[a-z0-9]+)\.(?<kind>command|event)\.(?<action>[a-z0-9_]+)\.v(?<major>\d+)$
```
- WebMvc helpers assume Spring 6 `ProblemDetail` mapping to RFC 7807 `application/problem+json`.
