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
package io.openleap.starter.testkit.messaging;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Lightweight RabbitMQ test harness using Testcontainers + Rabbit Java client.
 *
 * Notes:
 * - This class does not depend on Spring; services may still use Spring AMQP in their tests.
 * - You can start/stop the container here or manage it externally and only use the client helpers.
 */
public class RabbitTestSupport implements AutoCloseable {

    private RabbitMQContainer container;
    private Connection connection;
    private Channel channel;

    public RabbitTestSupport startContainer() {
        if (container == null) {
            container = new RabbitMQContainer("rabbitmq:3.13-management")
                    .withStartupTimeout(Duration.ofSeconds(60));
            container.start();
        }
        return this;
    }

    public RabbitTestSupport connect() throws Exception {
        if (container == null) throw new IllegalStateException("Container not started. Call startContainer() first or use connect(host,port,...) overload.");
        return connect(container.getHost(), container.getAmqpPort(), container.getAdminUsername(), container.getAdminPassword());
    }

    public RabbitTestSupport connect(String host, int port, String username, String password) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setPort(port);
        factory.setUsername(username);
        factory.setPassword(password);
        this.connection = factory.newConnection();
        this.channel = connection.createChannel();
        return this;
    }

    public RabbitTestSupport declareTopicExchange(String exchange) throws IOException {
        ensureChannel();
        channel.exchangeDeclare(exchange, "topic", true);
        return this;
    }

    public RabbitTestSupport declareQueueWithDlq(String queue, String dlxName) throws IOException {
        ensureChannel();
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", dlxName);
        channel.queueDeclare(queue, true, false, false, args);
        channel.queueDeclare(queue + ".dlq", true, false, false, null);
        // DLQ typically binds to DLX with routing key '#'
        channel.exchangeDeclare(dlxName, "topic", true);
        channel.queueBind(queue + ".dlq", dlxName, "#");
        return this;
    }

    public RabbitTestSupport bind(String exchange, String queue, String routingKey) throws IOException {
        ensureChannel();
        channel.queueBind(queue, exchange, routingKey);
        return this;
    }

    public RabbitTestSupport publishJson(String exchange, String routingKey, String jsonBody, Map<String, Object> headers) throws IOException {
        ensureChannel();
        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .headers(headers)
                .build();
        channel.basicPublish(exchange, routingKey, props, jsonBody.getBytes(StandardCharsets.UTF_8));
        return this;
    }

    public GetResult getOne(String queue, long timeoutMillis) throws IOException {
        ensureChannel();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            com.rabbitmq.client.GetResponse r = channel.basicGet(queue, true);
            if (r != null) {
                String body = new String(r.getBody(), StandardCharsets.UTF_8);
                Map<String, Object> headers = r.getProps() != null ? r.getProps().getHeaders() : Map.of();
                return new GetResult(body, headers);
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        return null;
    }

    public static void assertStandardHeadersPresent(Map<String, Object> headers, boolean requireIdempotency) {
        requireHeader(headers, "x-tenant-id");
        requireHeader(headers, "x-trace-id");
        requireHeader(headers, "x-correlation-id");
        requireHeader(headers, "x-causation-id");
        if (requireIdempotency) requireHeader(headers, "x-idempotency-key");
    }

    private static void requireHeader(Map<String, Object> headers, String key) {
        if (headers == null || !headers.containsKey(key)) {
            throw new AssertionError("Missing required header: " + key);
        }
        Object v = headers.get(key);
        if (v == null || Objects.toString(v).isBlank()) {
            throw new AssertionError("Header present but blank: " + key);
        }
    }

    private void ensureChannel() {
        if (channel == null) throw new IllegalStateException("Not connected; call connect() first");
    }

    @Override
    public void close() throws Exception {
        if (channel != null) {
            try { channel.close(); } catch (IOException | TimeoutException ignored) {}
        }
        if (connection != null) {
            try { connection.close(); } catch (IOException ignored) {}
        }
        if (container != null) {
            container.stop();
        }
    }

    public record GetResult(String body, Map<String, Object> headers) {}
}
