/*
 * This file is part of the openleap.io software project.
 *
 *  Copyright (C) 2025 Dr.-Ing. Sören Kemmann
 *
 * This software is dual-licensed under:
 *
 * 1. The European Union Public License v.1.2 (EUPL)
 *    https://joinup.ec.europa.eu/collection/eupl
 *
 *     You may use, modify and redistribute this file under the terms of the EUPL.
 *
 *  2. A commercial license available from:
 *
 *     B+B Unternehmensberatung GmbH & Co.KG
 *     Robert-Bunsen-Straße 10
 *     67098 Bad Dürkheim
 *     Germany
 *     Contact: license@bb-online.de
 *
 *  You may choose which license to apply.
 */
package io.openleap.common.messaging.config;

import io.openleap.common.messaging.MessageCoverageTracker;
import io.openleap.common.messaging.exception.NonRetryableException;
import io.openleap.common.messaging.exception.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.backoff.ExponentialBackOff;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
@ConditionalOnProperty(prefix = "ol.messaging", name = "enabled", havingValue = "true")
public class MessagingConfig {

    private static final Logger log = LoggerFactory.getLogger(MessagingConfig.class);

    private final MessageCoverageTracker coverageTracker;

    private final MessagingProperties olStarterServiceProperties;

    public MessagingConfig(MessageCoverageTracker coverageTracker,
                           MessagingProperties olStarterServiceProperties) {
        this.coverageTracker = coverageTracker;
        this.olStarterServiceProperties = olStarterServiceProperties;
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(olStarterServiceProperties.getEventsExchange(), true, false);
    }

    @Bean
    public JacksonJsonMessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    // TODO (itaseski): This method is outdated and currently there is no avro support in the starter.
    // Specifically, 'org.springframework.cloud.stream.schema.avro.AvroSchemaMessageConverter'
    // is missing from the classpath. This manual reflection-based logic should be removed
    @Bean
    @ConditionalOnClass(name = "org.springframework.cloud.stream.schema.avro.AvroSchemaMessageConverter")
    @Deprecated
    public MessageConverter starterMessageConverter(JacksonJsonMessageConverter jsonConverter) {
        boolean registryEnabled = olStarterServiceProperties.getRegistry().isEnabled();
        if (!registryEnabled) {
            return jsonConverter;
        }
        // Try to create Avro converter via reflection to avoid hard dependency
        try {
            Class<?> convClass = Class.forName("org.springframework.cloud.stream.schema.avro.AvroSchemaMessageConverter");
            Object conv = convClass.getDeclaredConstructor().newInstance();
            // If SchemaRegistryClient is present and URL configured, try to set it
            try {
                Class<?> clientClass = Class.forName("org.springframework.cloud.schema.registry.client.SchemaRegistryClient");
                String url = olStarterServiceProperties.getRegistry().getUrl();
                if (url != null && !url.isBlank()) {
                    // Create a default client if available (implementation varies by version). If not, fallback to JSON.
                    Object client;
                    try {
                        Class<?> impl = Class.forName("org.springframework.cloud.schema.registry.client.DefaultSchemaRegistryClient");
                        client = impl.getDeclaredConstructor().newInstance();
                        impl.getMethod("setEndpoint", String.class).invoke(client, url);
                        convClass.getMethod("setSchemaRegistryClient", clientClass).invoke(conv, client);
                    } catch (ClassNotFoundException e) {
                        log.warn("Schema Registry client implementation not found on classpath; falling back to JSON.");
                        return jsonConverter;
                    }
                }
            } catch (ClassNotFoundException ignored) {
                log.warn("Spring Cloud Schema Registry classes not found; falling back to JSON.");
                return jsonConverter;
            }
            if (olStarterServiceProperties.getRegistry().getFormat() != null) {
                try {
                    convClass.getMethod("setContentType", String.class)
                            .invoke(conv, "application/*+avro");
                } catch (NoSuchMethodException ignored) {
                    // older versions may not have this, ignore
                }
            }
            return (MessageConverter) conv;
        } catch (Throwable t) {
            log.warn("Failed to initialize Avro message converter; using JSON instead: {}", t.toString());
            return jsonConverter;
        }
    }

    @Bean
    public CachingConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password) {
        CachingConnectionFactory cf = new CachingConnectionFactory(host, port);
        cf.setUsername(username);
        cf.setPassword(password);
        cf.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        cf.setPublisherReturns(true);
        return cf;
    }

    @Bean
    @ConditionalOnProperty(
            name = "ol.messaging.coverage",
            havingValue = "true"
    )
    public RabbitTemplate rabbitTemplateCoverage(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        // Add interceptor to track sent messages
        if (coverageTracker != null) {
            template.setBeforePublishPostProcessors(message -> {
                MessageProperties props = message.getMessageProperties();
                String exchange = props.getReceivedExchange();
                String routingKey = props.getReceivedRoutingKey();
                coverageTracker.recordSentMessage(exchange, routingKey);
                return message;
            });
        }
        // Confirm and returns callbacks are used by the dispatcher via CorrelationData futures as well
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(RabbitTemplate.class)
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        // Confirm and returns callbacks are used by the dispatcher via CorrelationData futures as well
        return template;
    }

    @Bean
    public MessagingIdentityClearingAdvice messagingIdentityClearingAdvice() {
        return new MessagingIdentityClearingAdvice();
    }

    @Bean(name = "starterRabbitListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory starterRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter converter,
            MessagingIdentityPostProcessor identityPostProcessor,
            MessagingIdentityClearingAdvice clearingAdvice,
            MessagingProperties olStarterServiceProperties) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setDefaultRequeueRejected(false);
        factory.setMissingQueuesFatal(false);
        // Identity extraction/validation for incoming messages + always clear afterward
        factory.setAfterReceivePostProcessors(identityPostProcessor);
        // TODO (itaseski): Check the use of StatelessRetryOperationsInterceptor and StatefulRetryOperationsInterceptor
        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless()
                        .retryPolicy(retryPolicy(olStarterServiceProperties))
                        .recoverer((message, cause) -> {
                            throw new AmqpRejectAndDontRequeueException(cause);
                        })
                        .build(),
                clearingAdvice);
        return factory;
    }

    // TODO (itaseski): Check the use of RabbitTemplateRetrySettingsCustomizer and RabbitListenerRetrySettingsCustomizer
    @Bean
    public RetryTemplate rabbitRetryTemplate(MessagingProperties properties) {
        RetryPolicy retryPolicy = retryPolicy(properties);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        return template;
    }

    private RetryPolicy retryPolicy(MessagingProperties properties) {
        ExponentialBackOff backoff = new ExponentialBackOff();
        backoff.setInitialInterval(properties.getRetry().getInitialInterval());
        backoff.setMultiplier(properties.getRetry().getMultiplier());
        backoff.setMaxInterval(properties.getRetry().getMaxInterval());
        backoff.setMaxElapsedTime(properties.getRetry().getMaxAttempts() * properties.getRetry().getMaxInterval()); // Control max attempts via time

        return RetryPolicy.builder()
                .includes(RetryableException.class)
                .excludes(NonRetryableException.class, IllegalArgumentException.class)
                // TODO (itaseski): Should set delay?
                .backOff(backoff)
                .build();
    }
}
