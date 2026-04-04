# ADR-008: Error Handling & Standardized Responses

**Status:** Accepted

## Context

Microservices need consistent error responses across all endpoints. Inconsistent error formats make it difficult for API consumers to handle errors programmatically and for monitoring tools to parse error payloads. A centralized exception handler ensures that all errors — validation, business, and infrastructure — are returned in a uniform structure.

## Decision

The starter provides a `GlobalExceptionHandler` (`@RestControllerAdvice`) that catches exceptions and transforms them into a standardized `ErrorResponse` DTO. An `ErrorCode` catalog defines well-known error codes. Services extend this by throwing standard exceptions that the handler maps to appropriate HTTP status codes.

### Key Classes

| Class | FQCN | Purpose |
|-------|------|---------|
| `GlobalExceptionHandler` | `io.openleap.common.http.error.GlobalExceptionHandler` | Centralized `@RestControllerAdvice` exception handler |
| `ErrorResponse` | `io.openleap.common.http.error.ErrorResponse` | Standardized error response DTO |
| `ErrorCode` | `io.openleap.common.http.error.ErrorCode` | Catalog of well-known error codes |
| `PageableResponseDto` | `io.openleap.common.http.api.PageableResponseDto` | Generic paginated response wrapper |
| `RetryableException` | `io.openleap.common.messaging.exception.RetryableException` | Marks exceptions as retryable (for messaging) |
| `NonRetryableException` | `io.openleap.common.messaging.exception.NonRetryableException` | Marks exceptions as non-retryable (for messaging) |

### ErrorResponse Structure

```json
{
  "errorCode": "ORDER_NOT_FOUND",
  "message": "Order with ID 123 not found",
  "timestamp": "2026-01-15T10:30:00Z",
  "path": "/api/orders/123"
}
```

## Usage

### Enabling Error Handling

```java
@SpringBootApplication
@EnableOpenLeapErrorHandling
public class MyServiceApplication { }
```

### Throwing Business Exceptions

The `GlobalExceptionHandler` maps standard exceptions to HTTP status codes:

```java
@Service
class OrderService {

    public Order findById(UUID id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + id));
    }

    public void fulfillOrder(UUID id) {
        Order order = findById(id);
        if (order.isFulfilled()) {
            throw new IllegalStateException("Order already fulfilled");
        }
        // ...
    }
}
```

### Paginated Responses

```java
@RestController
class OrderController {

    @GetMapping("/orders")
    public PageableResponseDto<OrderDto> listOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);
        List<OrderDto> dtos = page.getContent().stream()
            .map(OrderDto::from)
            .toList();
        return new PageableResponseDto<>(dtos, page);
    }
}
```

### Messaging Exception Types

For message consumers, distinguish between retryable and non-retryable failures:

```java
@RabbitListener(
    queues = "payments.queue",
    containerFactory = "starterRabbitListenerContainerFactory"
)
public void onPaymentRequest(PaymentCommand cmd) {
    try {
        paymentGateway.charge(cmd);
    } catch (TemporaryGatewayException e) {
        throw new RetryableException("Payment gateway temporarily unavailable", e);
    } catch (InvalidCardException e) {
        throw new NonRetryableException("Invalid card, will not retry", e);
    }
}
```

## Configuration

No additional `application.yml` properties are required. The `@EnableOpenLeapErrorHandling` annotation activates the `GlobalExceptionHandler`.

## Compliance Rules

1. `@EnableOpenLeapErrorHandling` MUST be present on the application class.
2. REST APIs MUST NOT return raw exception stack traces — the `GlobalExceptionHandler` ensures this.
3. Business exceptions SHOULD use standard Java exceptions (`IllegalStateException`, `IllegalArgumentException`, `EntityNotFoundException`) that the handler maps automatically.
4. Custom error codes SHOULD be defined as constants following the `ErrorCode` pattern.
5. Message consumers MUST classify exceptions as `RetryableException` or `NonRetryableException` to control retry behavior.
6. Paginated endpoints SHOULD return `PageableResponseDto` for consistent pagination metadata.
7. API error responses MUST include an `errorCode` that clients can use for programmatic handling.

## Anti-Patterns

| Anti-Pattern | Correct Approach |
|-------------|-----------------|
| Catching exceptions in controllers and building custom error responses | Let `GlobalExceptionHandler` handle it uniformly |
| Returning stack traces in API responses | The handler strips stack traces automatically |
| Using HTTP status codes without error body | Always return `ErrorResponse` with code and message |
| Throwing generic `RuntimeException` in message consumers | Use `RetryableException` or `NonRetryableException` |
| Building pagination response manually | Use `PageableResponseDto` |
| Swallowing exceptions silently | Let them propagate to the handler or log explicitly |
