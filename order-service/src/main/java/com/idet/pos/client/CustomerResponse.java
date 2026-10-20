package com.idet.pos.client;

/**
 * Customer data returned by customer-service. Order-service keeps its own copy of the
 * shape it needs instead of sharing customer-service DTOs.
 */
public record CustomerResponse(String id, String name, String address, String email) {
}
