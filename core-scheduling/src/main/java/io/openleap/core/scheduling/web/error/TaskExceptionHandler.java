package io.openleap.core.scheduling.web.error;

import io.openleap.core.web.error.ErrorCode;
import io.openleap.core.web.error.ErrorResponse;
import io.openleap.core.scheduling.api.exception.*;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class TaskExceptionHandler {

    private static final String TRACE_ID = "traceId";

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFoundException(TaskNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.NOT_FOUND.name(),
                ErrorCode.NOT_FOUND.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TaskHandlerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskHandlerNotFoundException(TaskHandlerNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.NOT_FOUND.name(),
                ErrorCode.NOT_FOUND.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TaskAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTaskAlreadyExistsException(TaskAlreadyExistsException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.CONFLICT.name(),
                ErrorCode.CONFLICT.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TaskNotCancellableException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotCancellableException(TaskNotCancellableException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.CONFLICT.name(),
                ErrorCode.CONFLICT.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TaskSerializationException.class)
    public ResponseEntity<ErrorResponse> handleTaskSerializationException(TaskSerializationException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.BAD_REQUEST.name(),
                ErrorCode.BAD_REQUEST.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TaskQueueFullException.class)
    public ResponseEntity<ErrorResponse> handleTaskQueueFullException(TaskQueueFullException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.CONFLICT.name(),
                ErrorCode.CONFLICT.message(),
                ex.getMessage(),
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(TaskExecutionException.class)
    public ResponseEntity<ErrorResponse> handleTaskExecutionException(TaskExecutionException ex) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                ErrorCode.INTERNAL_ERROR.message(),
                "Task execution failed. Please contact support if the problem persists.",
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // TODO (itaseski): Consider removing it since its already available in the GlobalExceptionHandler
    @Deprecated(forRemoval = true)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse body = new ErrorResponse(
                ErrorCode.BAD_REQUEST.name(),
                ErrorCode.BAD_REQUEST.message(),
                detail,
                MDC.get(TRACE_ID)
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

}
