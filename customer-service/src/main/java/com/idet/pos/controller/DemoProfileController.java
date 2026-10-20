package com.idet.pos.controller;

import com.idet.pos.config.AppProfileMessageProvider;
import com.idet.pos.config.DemoEnvironmentBean;
import com.idet.pos.config.PosConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/demo")
public class DemoProfileController {

    private final Environment environment;
    private final AppProfileMessageProvider profileMessageProvider;
    private final DemoEnvironmentBean demoEnvironmentBean;
    private final PosConfigurationProperties posConfigurationProperties;

    @Value("${app.demo.environment:default}")
    private String configuredEnvironment;

    public DemoProfileController(
            Environment environment,
            AppProfileMessageProvider profileMessageProvider,
            DemoEnvironmentBean demoEnvironmentBean,
            PosConfigurationProperties posConfigurationProperties
    ) {
        this.environment = environment;
        this.profileMessageProvider = profileMessageProvider;
        this.demoEnvironmentBean = demoEnvironmentBean;
        this.posConfigurationProperties = posConfigurationProperties;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfileDemo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
        response.put("configuredEnvironment", configuredEnvironment);
        response.put("profileMessage", profileMessageProvider.getMessage());
        response.put("commonBeanMessage", demoEnvironmentBean.getMessage());
        response.put("storeName", posConfigurationProperties.getStoreName());
        return ResponseEntity.ok(response);
    }
}

