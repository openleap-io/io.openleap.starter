package io.openleap.core.scheduling.dbos.config;

import dev.dbos.transact.DBOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

public class DbosLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DbosLifecycle.class);

    private volatile boolean running = false;

    @Override
    public void start() {
        log.info("Launching DBOS");
        DBOS.launch();
        running = true;
    }

    @Override
    public void stop() {
        log.info("Shutting down DBOS");
        try {
            DBOS.shutdown();
        } finally {
            running = false;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return -1;
    }
}
