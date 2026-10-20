package com.idet.pos.dto;

/**
 * Order count and amount spent for one customer. Served to customer-service for its
 * purchase analytics endpoints.
 */
public class CustomerOrderTotalsDTO {
    private final String customerId;
    private final long orderCount;
    private final double totalAmount;

    public CustomerOrderTotalsDTO(String customerId, long orderCount, double totalAmount) {
        this.customerId = customerId;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

    public String getCustomerId() { return customerId; }
    public long getOrderCount() { return orderCount; }
    public double getTotalAmount() { return totalAmount; }
}
