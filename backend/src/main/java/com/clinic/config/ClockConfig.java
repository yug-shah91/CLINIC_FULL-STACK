package com.clinic.config;

import jakarta.validation.ClockProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

/**
 * One Clock in the clinic's timezone.
 * Injected across services and explicitly configured into Spring MVC's
 * LocalValidatorFactoryBean and Jakarta Validation ClockProvider,
 * ensuring @Future and @FutureOrPresent constraints behave identically
 * across all deployment environments regardless of the server host timezone.
 */
@Configuration
public class ClockConfig implements WebMvcConfigurer {

    @Value("${clinic.timezone:Asia/Kolkata}")
    private String timezone;

    @Bean
    public Clock clinicClock() {
        return Clock.system(ZoneId.of(timezone));
    }

    @Bean
    public ClockProvider validationClockProvider(Clock clinicClock) {
        return () -> clinicClock;
    }

    @Bean
    @Primary
    public LocalValidatorFactoryBean validator(Clock clinicClock) {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        factoryBean.setConfigurationInitializer(configuration -> configuration.clockProvider(() -> clinicClock));
        return factoryBean;
    }

    @Override
    public org.springframework.validation.Validator getValidator() {
        return validator(clinicClock());
    }
}
