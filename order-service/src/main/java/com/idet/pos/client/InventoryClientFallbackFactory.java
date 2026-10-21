package com.idet.pos.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {
            @Override
            public void reserve(List<StockLine> lines) {
                throw RemoteCallFailures.toException("item-service", cause);
            }

            @Override
            public void release(List<StockLine> lines) {
                throw RemoteCallFailures.toException("item-service", cause);
            }
        };
    }
}
