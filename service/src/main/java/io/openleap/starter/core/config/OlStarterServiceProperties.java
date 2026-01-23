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
package io.openleap.starter.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ol.service")
public class OlStarterServiceProperties {

    private Messaging messaging = new Messaging();
    private Security security = new Security();

    public Messaging getMessaging() { return messaging; }
    public void setMessaging(Messaging messaging) { this.messaging = messaging; }

    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }

    public static class Security {
        /**
         * Security mode selector. Valid values:
         * - nosec: headers carry plain identity info (X-Tenant-Id, X-User-Id, X-Scopes, X-Roles)
         * - simplesec: JWT is provided (HTTP: Authorization Bearer or X-JWT; Messaging: x-jwt header)
         */
        private Mode mode = Mode.nosec;

        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode; }

        public enum Mode { nosec, iamsec}
    }

    public static class Messaging {
        private String eventsExchange = "ol.exchange.events";
        private String commandsExchange = "ol.exchange.commands";
        private boolean coverage = false;
        private Registry registry = new Registry();
        private Outbox outbox = new Outbox();
        private Metrics metrics = new Metrics();
        private Retry retry = new Retry();

        public String getEventsExchange() { return eventsExchange; }
        public void setEventsExchange(String eventsExchange) { this.eventsExchange = eventsExchange; }
        public String getCommandsExchange() { return commandsExchange; }
        public void setCommandsExchange(String commandsExchange) { this.commandsExchange = commandsExchange; }
        public boolean isCoverage() { return coverage; }
        public void setCoverage(boolean coverage) { this.coverage = coverage; }
        public Registry getRegistry() { return registry; }
        public void setRegistry(Registry registry) { this.registry = registry; }
        public Outbox getOutbox() { return outbox; }
        public void setOutbox(Outbox outbox) { this.outbox = outbox; }
        public Metrics getMetrics() { return metrics; }
        public void setMetrics(Metrics metrics) { this.metrics = metrics; }
        public Retry getRetry() { return retry; }
        public void setRetry(Retry retry) { this.retry = retry; }

        public static class Retry {
            private int maxAttempts;
            private long initialInterval;
            private double multiplier;
            private long maxInterval;

            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            public long getInitialInterval() { return initialInterval; }
            public void setInitialInterval(long initialInterval) { this.initialInterval = initialInterval; }
            public double getMultiplier() { return multiplier; }
            public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
            public long getMaxInterval() { return maxInterval; }
            public void setMaxInterval(long maxInterval) { this.maxInterval = maxInterval; }
        }

        public static class Registry {
            private boolean enabled = false;
            private String url = "http://localhost:8990";
            private String format = "avro"; // only avro supported for now

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public String getFormat() { return format; }
            public void setFormat(String format) { this.format = format; }
        }

        public static class Outbox {
            private Dispatcher dispatcher = new Dispatcher();
            private int maxAttempts = 10;
            private long confirmTimeoutMillis = 5000L;

            public Dispatcher getDispatcher() { return dispatcher; }
            public void setDispatcher(Dispatcher dispatcher) { this.dispatcher = dispatcher; }
            public int getMaxAttempts() { return maxAttempts; }
            public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
            public long getConfirmTimeoutMillis() { return confirmTimeoutMillis; }
            public void setConfirmTimeoutMillis(long confirmTimeoutMillis) { this.confirmTimeoutMillis = confirmTimeoutMillis; }

            public static class Dispatcher {
                private long fixedDelay = 1000L;
                private boolean enabled = true;
                private boolean wakeupAfterCommit = true;

                public long getFixedDelay() { return fixedDelay; }
                public void setFixedDelay(long fixedDelay) { this.fixedDelay = fixedDelay; }
                public boolean isEnabled() { return enabled; }
                public void setEnabled(boolean enabled) { this.enabled = enabled; }
                public boolean isWakeupAfterCommit() { return wakeupAfterCommit; }
                public void setWakeupAfterCommit(boolean wakeupAfterCommit) { this.wakeupAfterCommit = wakeupAfterCommit; }
            }
        }

        public static class Metrics {
            private Queues queues = new Queues();

            public Queues getQueues() { return queues; }
            public void setQueues(Queues queues) { this.queues = queues; }

            public static class Queues {
                private String main = "";
                private String dlq = "";

                public String getMain() { return main; }
                public void setMain(String main) { this.main = main; }
                public String getDlq() { return dlq; }
                public void setDlq(String dlq) { this.dlq = dlq; }
            }
        }
    }
}
