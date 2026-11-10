package com.idet.pos.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        // Matching property names and getters/setters allow automatic DTO/entity mapping.
        // Only CustomerService uses this bean in this demo; other services retain manual mapping.
        return new ModelMapper();
    }
}
