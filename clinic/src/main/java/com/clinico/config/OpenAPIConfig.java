package com.clinico.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI clinicoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CliniQ – Simple Doctor Appointment Booking API")
                        .description("RESTful backend API for doctor scheduling, slot management, and patient appointment bookings.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CliniQ Support")
                                .email("support@clinico.org"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
