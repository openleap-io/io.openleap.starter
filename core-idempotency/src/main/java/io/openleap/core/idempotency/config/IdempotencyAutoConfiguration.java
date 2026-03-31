package io.openleap.core.idempotency.config;

import io.openleap.core.idempotency.IdempotencyRecordRepository;
import io.openleap.core.idempotency.IdempotencyRecordService;
import io.openleap.core.idempotency.aspect.IdempotentAspect;
import io.openleap.core.idempotency.config.registrar.IdempotencyEntityRegistrar;
import io.openleap.core.idempotency.config.registrar.IdempotencyRepositoryRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfiguration
@ConditionalOnProperty(prefix = "ol.idempotency", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({IdempotencyEntityRegistrar.class, IdempotencyRepositoryRegistrar.class})
public class IdempotencyAutoConfiguration {

    @Bean
    IdempotencyRecordService idempotencyRecordService(IdempotencyRecordRepository repository) {
        return new IdempotencyRecordService(repository);
    }

    @Bean
    IdempotentAspect idempotentAspect(IdempotencyRecordService idempotencyRecordService) {
        return new IdempotentAspect(idempotencyRecordService);
    }

}
