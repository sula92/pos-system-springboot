package com.idet.pos.projection;

public interface CustomerOrderTotalsProjection {
    String getCustomerId();
    Long getOrderCount();
    Double getTotalAmount();
}
