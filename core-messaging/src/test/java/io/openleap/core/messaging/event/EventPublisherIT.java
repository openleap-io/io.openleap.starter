package io.openleap.core.messaging.event;

import io.openleap.core.MessagingTestApplication;
import io.openleap.core.TestConfig;
import io.openleap.core.messaging.MessagingConstants;
import io.openleap.core.messaging.RoutingKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {MessagingTestApplication.class, TestConfig.class})
@Testcontainers
@ActiveProfiles("test")
class EventPublisherIT {

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.2-management");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    }

    @Autowired
    EventPublisher eventPublisher;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    JsonMapper jsonMapper;

    @Test
    @DisplayName("Should successfully persist event to database and publish to RabbitMQ")
    void shouldPersistEventAndPublishToMessageBroker() {
        // given
        RoutingKey routingKey = RoutingKey.of(MessagingConstants.OUTBOX_ROUTING_KEY, "test-description");

        BaseDomainEvent domainEvent = BaseDomainEvent.builder()
                .aggregateId("aggregate-id")
                .aggregateType("aggregate-type")
                .changeType("change-type")
                .build();

        // when
        eventPublisher.enqueue(routingKey, domainEvent, new HashMap<>());

        // then
        await()
                .atMost(5, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .untilAsserted(() -> {
                    BaseDomainEvent baseDomainEvent = rabbitTemplate.receiveAndConvert(MessagingConstants.OUTBOX_QUEUE, new ParameterizedTypeReference<>() {});
                    assertThat(baseDomainEvent)
                            .isNotNull()
                            .usingRecursiveComparison()
                            .isEqualTo(domainEvent);
                });
    }

}
