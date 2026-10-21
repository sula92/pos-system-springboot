package com.idet.pos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ProfileDemoConfig {

    @Bean
    public DemoEnvironmentBean commonDemoEnvironmentBean() {
        return new DemoEnvironmentBean("This bean is available in all environments");
    }

    @Bean
    @Profile("dev")
    public AppProfileMessageProvider devProfileMessageProvider() {
        return () -> "Running in DEV profile";
    }

    @Bean
    @Profile("qa")
    public AppProfileMessageProvider qaProfileMessageProvider() {
        return () -> "Running in QA profile";
    }

    @Bean
    @Profile("staging")
    public AppProfileMessageProvider stagingProfileMessageProvider() {
        return () -> "Running in STAGING profile";
    }

    @Bean
    @Profile("prod")
    public AppProfileMessageProvider prodProfileMessageProvider() {
        return () -> "Running in PROD profile";
    }

    @Bean
    @Profile("!dev & !qa & !staging & !prod")
    public AppProfileMessageProvider defaultProfileMessageProvider() {
        return () -> "Running in DEFAULT profile";
    }
}

