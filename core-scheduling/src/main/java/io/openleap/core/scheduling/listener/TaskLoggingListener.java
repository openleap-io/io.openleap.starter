package io.openleap.core.scheduling.listener;

import io.openleap.core.scheduling.api.listener.TaskLifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskLoggingListener implements TaskLifecycleListener {

    private static final Logger log = LoggerFactory.getLogger(TaskLoggingListener.class);

    @Override
    public void onSubmitted(String taskId, String handlerName) {
        log.info("Task submitted taskId={} handler={}", taskId, handlerName);
    }

    @Override
    public void onCompleted(String taskId, String handlerName) {
        log.info("Task completed taskId={} handler={}", taskId, handlerName);
    }

    @Override
    public void onFailed(String taskId, String handlerName, Throwable error) {
        log.error("Task failed taskId={} handler={} error={}", taskId, handlerName, error.getMessage(), error);
    }

    @Override
    public void onCancelled(String taskId) {
        log.info("Task cancelled taskId={}", taskId);
    }
}
