package io.openleap.core.scheduling.registry;

import io.openleap.core.scheduling.api.exception.TaskHandlerNotFoundException;
import io.openleap.core.scheduling.api.handler.TaskHandler;

import java.util.*;

public class TaskHandlerRegistry {

    private final Map<String, TaskHandler<?, ?>> handlers = new HashMap<>();

    public TaskHandlerRegistry(List<TaskHandler<?, ?>> handlers) {
        handlers.forEach(h -> this.handlers.put(h.name(), h));
    }

    public TaskHandler<?, ?> get(String name) {
        TaskHandler<?, ?> handler = handlers.get(name);
        if (handler == null) {
            throw new TaskHandlerNotFoundException(name);
        }
        return handler;
    }

    public boolean contains(String name) {
        return handlers.containsKey(name);
    }

    public boolean isAbsent(String name) {
        return !handlers.containsKey(name);
    }

    public Collection<TaskHandler<?, ?>> all() {
        return Collections.unmodifiableCollection(handlers.values());
    }
}
