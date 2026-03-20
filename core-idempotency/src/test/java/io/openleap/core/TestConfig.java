package io.openleap.core;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.json.JsonMapper;

@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
public class TestConfig {

    @Bean
    public JsonMapper jsonMapper() {
        return new JsonMapper();
    }

}
