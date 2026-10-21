package com.idet.pos.dto;

public class CustomerOrderCountDTO {
    private final String id;
    private final String name;
    private final long orderCount;

    public CustomerOrderCountDTO(String id, String name, long orderCount) {
        this.id = id;
        this.name = name;
        this.orderCount = orderCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public long getOrderCount() { return orderCount; }
}
