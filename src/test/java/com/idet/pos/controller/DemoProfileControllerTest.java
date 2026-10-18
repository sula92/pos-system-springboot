package com.idet.pos.controller;

import com.idet.pos.config.AppProfileMessageProvider;
import com.idet.pos.config.DemoEnvironmentBean;
import com.idet.pos.config.PosConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoProfileControllerTest {

    @Test
    void getProfileDemo_returnsActiveProfileAndMessage() {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        AppProfileMessageProvider messageProvider = () -> "Running in DEV profile";
        DemoEnvironmentBean commonBean = new DemoEnvironmentBean("This bean is available in all environments");

        PosConfigurationProperties properties = new PosConfigurationProperties();
        properties.setStoreName("IDT POS Demo Store (DEV)");

        DemoProfileController controller = new DemoProfileController(environment, messageProvider, commonBean, properties);
        ReflectionTestUtils.setField(controller, "configuredEnvironment", "dev");

        Map<String, Object> body = controller.getProfileDemo().getBody();

        assertNotNull(body);
        assertEquals(List.of("dev"), body.get("activeProfiles"));
        assertEquals("dev", body.get("configuredEnvironment"));
        assertEquals("Running in DEV profile", body.get("profileMessage"));
        assertEquals("This bean is available in all environments", body.get("commonBeanMessage"));
        assertEquals("IDT POS Demo Store (DEV)", body.get("storeName"));
    }
}

