package io.openleap.core.scheduling.support;

import io.openleap.core.scheduling.dbos.config.DbosLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Profile("test")
public class TestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TestExceptionHandler.class);

    // TODO (itaseski): Added here not to cause conflicts with other global exception handlers in the application.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception ex) {
        log.error("Exception during test execution: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}
