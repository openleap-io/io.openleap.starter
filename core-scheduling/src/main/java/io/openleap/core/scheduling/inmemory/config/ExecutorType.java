package io.openleap.core.scheduling.inmemory.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public enum ExecutorType {

    FIXED {
        @Override
        public ExecutorService create(int threadPoolSize) {
            return Executors.newFixedThreadPool(threadPoolSize);
        }
    },
    CACHED {
        @Override
        public ExecutorService create(int threadPoolSize) {
            return Executors.newCachedThreadPool();
        }
    },
    VIRTUAL {
        @Override
        public ExecutorService create(int threadPoolSize) {
            return Executors.newVirtualThreadPerTaskExecutor();
        }
    };

    public abstract ExecutorService create(int threadPoolSize);
}
