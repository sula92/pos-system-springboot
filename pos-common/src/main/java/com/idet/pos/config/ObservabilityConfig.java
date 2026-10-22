package com.idet.pos.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Keeps health checks and Eureka registry traffic out of the traces, so Zipkin shows business requests only.
 */
@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservationPredicate skipInfrastructureObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext server && server.getCarrier() != null) {
                return !server.getCarrier().getRequestURI().contains("/actuator");
            }
            if (context instanceof ClientRequestObservationContext client && client.getCarrier() != null) {
                return !client.getCarrier().getURI().getPath().contains("/eureka/");
            }
            return true;
        };
    }
}
