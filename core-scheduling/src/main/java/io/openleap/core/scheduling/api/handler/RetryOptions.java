package io.openleap.core.scheduling.api.handler;

public record RetryOptions(
        int maxAttempts,
        double intervalSeconds,
        double backoffRate
) {
    public static RetryOptions of() {
        return new RetryOptions(1, 1.0, 2.0);
    }

    public RetryOptions withMaxAttempts(int maxAttempts) {
        return new RetryOptions(maxAttempts, intervalSeconds, backoffRate);
    }

    public RetryOptions withIntervalSeconds(double intervalSeconds) {
        return new RetryOptions(maxAttempts, intervalSeconds, backoffRate);
    }

    public RetryOptions withBackoffRate(double backoffRate) {
        return new RetryOptions(maxAttempts, intervalSeconds, backoffRate);
    }
}
