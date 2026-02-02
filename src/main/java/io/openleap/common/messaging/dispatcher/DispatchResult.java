package io.openleap.common.messaging.dispatcher;

public record DispatchResult(boolean success, String reason) {

    public static DispatchResult ok() {
        return new DispatchResult(true, null);
    }

    public static DispatchResult fail(String reason) {
        return new DispatchResult(false, reason);
    }

}
