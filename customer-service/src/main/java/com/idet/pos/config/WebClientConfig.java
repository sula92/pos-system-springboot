package com.idet.pos.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * The builder is created by hand, so it does not get Boot's observation setup.
     * Setting the ObservationRegistry makes WebClient calls create client spans and send the trace headers.
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder(ObjectProvider<ObservationRegistry> observationRegistry) {
        return WebClient.builder()
                .observationRegistry(observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP));
    }
}
