package com.idet.pos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * Feign client for the internal stock endpoints of item-service, resolved through Eureka
 * and protected by a Resilience4j circuit breaker.
 */
@FeignClient(name = "item-service", path = "/pos-system", fallbackFactory = InventoryClientFallbackFactory.class)
public interface InventoryClient {

    @PostMapping("/inventory/internal/reserve")
    void reserve(@RequestBody List<StockLine> lines);

    @PostMapping("/inventory/internal/release")
    void release(@RequestBody List<StockLine> lines);
}
