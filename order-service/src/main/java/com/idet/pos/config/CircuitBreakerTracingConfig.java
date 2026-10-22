package com.idet.pos.config;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * Feign calls guarded by a Resilience4j circuit breaker with a time limiter run on the breaker's own
 * thread pool. Wrapping that pool copies the caller's trace context (and log MDC) to the worker thread,
 * so the Feign call stays in the same trace. The time limiter keeps working.
 */
@Configuration
public class CircuitBreakerTracingConfig {

    @Bean
    public Customizer<Resilience4JCircuitBreakerFactory> contextPropagatingCircuitBreakerExecutor() {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();
        return factory -> {
            factory.configureExecutorService(
                    ContextExecutorService.wrap(Executors.newCachedThreadPool(), snapshotFactory));
            factory.configureGroupExecutorService(group ->
                    ContextExecutorService.wrap(Executors.newCachedThreadPool(), snapshotFactory));
        };
    }
}
