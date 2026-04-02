package io.openleap.core.scheduling.api.listener;

public interface TaskLifecycleListener {

    default void onSubmitted(String taskId, String handlerName) {}

    default void onCompleted(String taskId, String handlerName) {}

    default void onFailed(String taskId, String handlerName, Throwable error) {}

    default void onCancelled(String taskId) {}

}
