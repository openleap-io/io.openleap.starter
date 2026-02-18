package io.openleap.common.idempotency.aspect;

import io.openleap.common.idempotency.IdempotencyRecordService;
import io.openleap.common.idempotency.exception.DuplicateCommandException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    private final IdempotencyRecordService idempotencyRecordService;

    private final ParameterNameDiscoverer parameterNameDiscoverer;

    public IdempotentAspect(IdempotencyRecordService idempotencyRecordService) {
        this.idempotencyRecordService = idempotencyRecordService;
        this.parameterNameDiscoverer = new StandardReflectionParameterNameDiscoverer();
    }

    @Around(value = "@annotation(idempotent)", argNames = "pjp,idempotent")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {

        String commandId = resolveKey(idempotent.key(), idempotent.keyExpression(), pjp);

        if (commandId == null || commandId.isBlank()) {
            log.warn("Idempotency key resolved to blank, skipping check for {}", pjp.getSignature());
            return pjp.proceed();
        }

        if (idempotencyRecordService.alreadyProcessed(commandId)) {
            if (idempotent.failOnDuplicateExecution()) {
                throw new DuplicateCommandException("Command already processed with key: " + commandId);
            }
            log.info("Duplicate command detected, skipping execution. key={}", commandId);
            return null;
        }

        Object result = pjp.proceed();

        String purpose = idempotent.purpose().isBlank()
                ? pjp.getSignature().toShortString()
                : idempotent.purpose();

        idempotencyRecordService.markProcessed(commandId, purpose, null);

        return result;

    }

    private String resolveKey(String key, String keyExpression, ProceedingJoinPoint pjp) {
        // SpEL takes priority if provided
        if (!keyExpression.isBlank()) {
            return parseKeyExpression(pjp, keyExpression);
        }

        if (!key.isBlank()) {
            return key;
        }

        return null;
    }

    private String parseKeyExpression(ProceedingJoinPoint pjp, String keyExpression) {
        EvaluationContext context = new MethodBasedEvaluationContext(pjp, getMethod(pjp), pjp.getArgs(), parameterNameDiscoverer);
        SpelExpressionParser parser = new SpelExpressionParser();
        return parser.parseExpression(keyExpression).getValue(context, String.class);
    }

    private Method getMethod(ProceedingJoinPoint pjp) {
        return ((MethodSignature) pjp.getSignature()).getMethod();
    }
}