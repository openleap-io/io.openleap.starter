package io.openleap.core.scheduling.inmemory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "task.in-memory")
public class InMemoryTaskProperties {

    private ExecutorType executorType = ExecutorType.FIXED;

    private int threadPoolSize = Runtime.getRuntime().availableProcessors();

    public ExecutorType getExecutorType() {
        return executorType;
    }

    public void setExecutorType(ExecutorType executorType) {
        this.executorType = executorType;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }
}
