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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ol.messaging")
public class MessagingProperties {

    private boolean enabled;
    private String eventsExchange;
    private boolean coverage = false;
    private Registry registry = new Registry();
    private Outbox outbox = new Outbox();
    private Metrics metrics = new Metrics();
    private Retry retry = new Retry();

    // Getters and Setters


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEventsExchange() {
        return eventsExchange;
    }

    public void setEventsExchange(String eventsExchange) {
        this.eventsExchange = eventsExchange;
    }

    public boolean isCoverage() {
        return coverage;
    }

    public void setCoverage(boolean coverage) {
        this.coverage = coverage;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public void setOutbox(Outbox outbox) {
        this.outbox = outbox;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    // Nested configuration classes

    public static class Retry {
        private int maxAttempts = 3;
        private long initialInterval = 1000L;
        private double multiplier = 2.0;
        private long maxInterval = 10000L;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(long initialInterval) {
            this.initialInterval = initialInterval;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxInterval() {
            return maxInterval;
        }

        public void setMaxInterval(long maxInterval) {
            this.maxInterval = maxInterval;
        }
    }

    public static class Registry {
        private boolean enabled = false;
        private String url = "http://localhost:8990";
        private String format = "application/*+avro";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    public static class Outbox {
        private Dispatcher dispatcher = new Dispatcher();

        public Dispatcher getDispatcher() {
            return dispatcher;
        }

        public void setDispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        public static class Dispatcher {
            private long fixedDelay = 1000L;
            private boolean enabled = true;
            private boolean wakeupAfterCommit = true;
            private boolean deleteOnAck = false;
            private int maxAttempts = 10;
            private long confirmTimeoutMillis = 5000L;
            private String type = "rabbitmq";

            public long getFixedDelay() {
                return fixedDelay;
            }

            public void setFixedDelay(long fixedDelay) {
                this.fixedDelay = fixedDelay;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isWakeupAfterCommit() {
                return wakeupAfterCommit;
            }

            public void setWakeupAfterCommit(boolean wakeupAfterCommit) {
                this.wakeupAfterCommit = wakeupAfterCommit;
            }

            public boolean isDeleteOnAck() {
                return deleteOnAck;
            }

            public void setDeleteOnAck(boolean deleteOnAck) {
                this.deleteOnAck = deleteOnAck;
            }

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public long getConfirmTimeoutMillis() {
                return confirmTimeoutMillis;
            }

            public void setConfirmTimeoutMillis(long confirmTimeoutMillis) {
                this.confirmTimeoutMillis = confirmTimeoutMillis;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }
        }
    }

    public static class Metrics {
        private Queues queues = new Queues();

        public Queues getQueues() {
            return queues;
        }

        public void setQueues(Queues queues) {
            this.queues = queues;
        }

        public static class Queues {
            private String main = "";
            private String dlq = "";

            public String getMain() {
                return main;
            }

            public void setMain(String main) {
                this.main = main;
            }

            public String getDlq() {
                return dlq;
            }

            public void setDlq(String dlq) {
                this.dlq = dlq;
            }
        }
    }
}
