package com.recrutech.recrutechnotification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RecruTech Notification Service API")
                        .version("0.0.1")
                        .description("API documentation for the RecruTech Notification Service. This service handles email notifications and message processing via Kafka.")
                        .contact(new Contact()
                                .name("RecruTech Team")
                                .email("support@recrutech.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
