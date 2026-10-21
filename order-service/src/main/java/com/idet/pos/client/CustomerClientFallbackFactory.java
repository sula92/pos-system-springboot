package com.idet.pos.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CustomerClientFallbackFactory implements FallbackFactory<CustomerClient> {

    @Override
    public CustomerClient create(Throwable cause) {
        return id -> {
            throw RemoteCallFailures.toException("customer-service", cause);
        };
    }
}
