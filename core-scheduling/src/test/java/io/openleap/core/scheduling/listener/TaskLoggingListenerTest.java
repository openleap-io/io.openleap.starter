package io.openleap.core.scheduling.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class TaskLoggingListenerTest {

    private static final String TASK_ID = "task-1";
    private static final String HANDLER_NAME = "audit-log";

    private TaskLoggingListener listener;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        listener = new TaskLoggingListener();
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(TaskLoggingListener.class)).addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(TaskLoggingListener.class)).detachAppender(appender);
    }

    @Test
    void onSubmitted_logsAtInfo() {
        listener.onSubmitted(TASK_ID, HANDLER_NAME);

        assertThat(appender.list)
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.INFO);
                    assertThat(e.getFormattedMessage()).contains(TASK_ID, HANDLER_NAME);
                });
    }

    @Test
    void onCompleted_logsAtInfo() {
        listener.onCompleted(TASK_ID, HANDLER_NAME);

        assertThat(appender.list)
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.INFO);
                    assertThat(e.getFormattedMessage()).contains(TASK_ID, HANDLER_NAME);
                });
    }

    @Test
    void onFailed_logsAtError() {
        RuntimeException error = new RuntimeException("connection timeout");
        listener.onFailed(TASK_ID, HANDLER_NAME, error);

        assertThat(appender.list)
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.ERROR);
                    assertThat(e.getFormattedMessage()).contains(TASK_ID, HANDLER_NAME, "connection timeout");
                });
    }

    @Test
    void onCancelled_logsAtInfo() {
        listener.onCancelled(TASK_ID);

        assertThat(appender.list)
                .hasSize(1)
                .first()
                .satisfies(e -> {
                    assertThat(e.getLevel()).isEqualTo(Level.INFO);
                    assertThat(e.getFormattedMessage()).contains(TASK_ID);
                });
    }
}
