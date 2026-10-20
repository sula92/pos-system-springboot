package com.idet.pos.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI posSystemOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("POS Order Service API")
                        .description("REST APIs for order and order-detail management.")
                        .version("v1")
                        .contact(new Contact()
                                .name("POS Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Internal Use")
                                .url("https://example.com/license"))
                );
    }
}

