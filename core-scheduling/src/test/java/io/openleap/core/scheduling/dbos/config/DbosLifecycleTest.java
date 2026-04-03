package io.openleap.core.scheduling.dbos.config;

import dev.dbos.transact.DBOS;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DbosLifecycleTest {

    @Test
    void start_launchesDbosAndSetsRunningToTrue() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            DbosLifecycle lifecycle = new DbosLifecycle();

            lifecycle.start();

            dbos.verify(DBOS::launch);

            assertTrue(lifecycle.isRunning());
        }
    }

    @Test
    void stop_shutsDownDbosAndSetsRunningToFalse() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            DbosLifecycle lifecycle = new DbosLifecycle();
            lifecycle.start();

            lifecycle.stop();

            dbos.verify(DBOS::shutdown);

            assertFalse(lifecycle.isRunning());
        }
    }

    @Test
    void stop_setsRunningToFalse_evenWhenShutdownThrows() {
        try (MockedStatic<DBOS> dbos = mockStatic(DBOS.class)) {
            dbos.when(DBOS::shutdown).thenThrow(new RuntimeException("shutdown failed"));

            DbosLifecycle lifecycle = new DbosLifecycle();

            lifecycle.start();

            assertThrows(RuntimeException.class, lifecycle::stop);
            assertFalse(lifecycle.isRunning());
        }
    }

    @Test
    void isAutoStartup_returnsTrue() {
        DbosLifecycle lifecycle = new DbosLifecycle();

        assertTrue(lifecycle.isAutoStartup());
    }

    @Test
    void getPhase_returnsMinusOne() {
        DbosLifecycle lifecycle = new DbosLifecycle();

        assertEquals(-1, lifecycle.getPhase());
    }
}
