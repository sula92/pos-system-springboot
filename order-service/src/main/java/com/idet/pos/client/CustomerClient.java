package com.idet.pos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for customer-service, resolved through Eureka and protected by a
 * Resilience4j circuit breaker.
 */
@FeignClient(name = "customer-service", path = "/pos-system", fallbackFactory = CustomerClientFallbackFactory.class)
public interface CustomerClient {

    @GetMapping("/customer")
    CustomerResponse getCustomer(@RequestParam("id") String id);
}
