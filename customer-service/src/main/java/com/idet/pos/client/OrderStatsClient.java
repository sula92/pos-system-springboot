package com.idet.pos.client;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.logging.Logger;

/**
 * Reactive (WebFlux) client for order-service.
 * The call goes through a load-balanced WebClient (service name resolved via Eureka) and is
 * wrapped in a Resilience4j reactive circuit breaker. When order-service is slow or down the
 * fallback returns no totals, so callers degrade to "zero orders" instead of failing.
 */
@Component
public class OrderStatsClient {

    private static final Logger logger = Logger.getLogger(OrderStatsClient.class.getName());

    private static final String CUSTOMER_TOTALS_URI = "http://order-service/pos-system/order/internal/customer-totals";

    private final WebClient webClient;
    private final ReactiveCircuitBreaker circuitBreaker;

    public OrderStatsClient(@LoadBalanced WebClient.Builder webClientBuilder,
                            ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.webClient = webClientBuilder.build();
        this.circuitBreaker = circuitBreakerFactory.create("orderStats");
    }

    public Flux<CustomerOrderTotals> fetchCustomerTotals() {
        Flux<CustomerOrderTotals> call = webClient.get()
                .uri(CUSTOMER_TOTALS_URI)
                .retrieve()
                .bodyToFlux(CustomerOrderTotals.class);

        return circuitBreaker.run(call, throwable -> {
            logger.warning("order-service unavailable, using empty order totals: " + throwable.getMessage());
            return Flux.empty();
        });
    }
}
