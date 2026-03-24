package io.openleap.core.idempotency.config.registrar;

import io.openleap.core.idempotency.IdempotencyRecordEntity;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class IdempotencyEntityRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        EntityScanPackages.register(registry, IdempotencyRecordEntity.class.getPackageName());
    }

}
