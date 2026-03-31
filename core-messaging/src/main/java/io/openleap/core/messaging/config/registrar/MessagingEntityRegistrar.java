package io.openleap.core.messaging.config.registrar;

import io.openleap.core.messaging.entity.OutboxEvent;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

public class MessagingEntityRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        EntityScanPackages.register(registry, OutboxEvent.class.getPackageName());
    }

}