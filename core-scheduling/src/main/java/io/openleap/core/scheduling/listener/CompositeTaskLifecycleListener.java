package io.openleap.core.scheduling.listener;

import io.openleap.core.scheduling.api.listener.TaskLifecycleListener;

import java.util.List;

public class CompositeTaskLifecycleListener implements TaskLifecycleListener {

    private final List<TaskLifecycleListener> listeners;

    public CompositeTaskLifecycleListener(List<TaskLifecycleListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onSubmitted(String taskId, String handlerName) {
        listeners.forEach(l -> l.onSubmitted(taskId, handlerName));
    }

    @Override
    public void onCompleted(String taskId, String handlerName) {
        listeners.forEach(l -> l.onCompleted(taskId, handlerName));
    }

    @Override
    public void onFailed(String taskId, String handlerName, Throwable error) {
        listeners.forEach(l -> l.onFailed(taskId, handlerName, error));
    }

    @Override
    public void onCancelled(String taskId) {
        listeners.forEach(l -> l.onCancelled(taskId));
    }
}
