package com.idet.pos.projection;

import java.time.LocalDate;

public class OrderSummary {
    private String orderId;
    private LocalDate orderDate;
    private String customerName;

    public OrderSummary(String orderId, LocalDate orderDate, String customerName) {
        this.orderId = orderId;
        this.orderDate = orderDate;
        this.customerName = customerName;
    }

    public String getOrderId() {
        return orderId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public String getCustomerName() {
        return customerName;
    }
}
