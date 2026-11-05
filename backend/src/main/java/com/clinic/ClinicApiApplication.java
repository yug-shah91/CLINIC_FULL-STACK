package com.clinic;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "Clinic Management API",
        version = "1.0",
        description = "REST API for managing a single-doctor clinic — patients, appointments, prescriptions"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Paste your JWT token here (without 'Bearer ' prefix)"
)
public class ClinicApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicApiApplication.class, args);
    }
}
