package io.openleap.common.messaging.event;

import io.openleap.common.TestConfig;
import io.openleap.common.messaging.MessagingConstants;
import io.openleap.common.messaging.RoutingKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.HashMap;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = TestConfig.class)
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
                    String jsonMessage = (String) rabbitTemplate.receiveAndConvert(MessagingConstants.OUTBOX_QUEUE);
                    BaseDomainEvent baseDomainEvent = jsonMapper.readValue(jsonMessage, BaseDomainEvent.class);
                    assertThat(baseDomainEvent)
                            .isNotNull()
                            .usingRecursiveComparison()
                            .isEqualTo(domainEvent);
                });
    }

}
