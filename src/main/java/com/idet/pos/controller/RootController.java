package com.idet.pos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        // Minimal root response so GET /pos-system does not fall through to error handling.
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "POS System API is running");
        response.put("swagger", "/swagger-ui.html");
        response.put("customers", "/customer");
        response.put("items", "/item");
        response.put("orders", "/order");
        return response;
    }
}

