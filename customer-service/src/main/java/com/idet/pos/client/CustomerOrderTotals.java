package com.idet.pos.client;

/**
 * Order count and amount spent for one customer, as returned by order-service.
 */
public record CustomerOrderTotals(String customerId, long orderCount, double totalAmount) {
}
