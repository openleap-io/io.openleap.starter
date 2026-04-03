package io.openleap.core.scheduling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// TODO (itaseski): Consider making it per handler retry
@ConfigurationProperties(prefix = "task.retry")
public class TaskRetryProperties {

    private int maxAttempts = 1;

    private double intervalSeconds = 1.0;

    private double backoffRate = 2.0;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public double getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(double intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public double getBackoffRate() {
        return backoffRate;
    }

    public void setBackoffRate(double backoffRate) {
        this.backoffRate = backoffRate;
    }
}
