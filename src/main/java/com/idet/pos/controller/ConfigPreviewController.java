package com.idet.pos.controller;

import com.idet.pos.config.PosConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigPreviewController {

    private final PosConfigurationProperties posConfigurationProperties;

    public ConfigPreviewController(PosConfigurationProperties posConfigurationProperties) {
        this.posConfigurationProperties = posConfigurationProperties;
    }

    @GetMapping
    public Map<String, Object> getPosConfiguration() {
        return Map.of(
                "storeName", posConfigurationProperties.getStoreName(),
                "currency", posConfigurationProperties.getCurrency(),
                "maxItemsPerOrder", posConfigurationProperties.getMaxItemsPerOrder());
    }
}

