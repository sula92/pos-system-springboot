package com.idet.pos.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Circuit-breaker fallback for gateway routes.
 * Returns 503 with the same body shape as the services' GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", service + " is currently unavailable. Please try again later.");
        return Mono.just(ResponseEntity.status(status).body(response));
    }
}
