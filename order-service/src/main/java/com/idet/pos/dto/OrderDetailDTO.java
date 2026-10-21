package com.idet.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public class OrderDetailDTO {
    @Schema(description = "Order ID", example = "O001", pattern = "^O\\d{3}$")
    private String orderId;

    @Schema(description = "Item code", example = "I001", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^I\\d{3}$")
    @NotBlank(message = "Item code is required")
    @Pattern(regexp = "^I\\d{3}$", message = "Item code must match I followed by 3 digits (e.g. I001)")
    private String itemCode;

    @Schema(description = "Ordered quantity", example = "2", minimum = "1")
    @Positive(message = "Ordered quantity must be greater than zero")
    private int qty;

    @Schema(description = "Unit price at order time", example = "250.00", minimum = "0")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be zero or positive")
    private double unitPrice;

    public OrderDetailDTO() {}

    public OrderDetailDTO(String orderId, String itemCode, int qty, double unitPrice) {
        this.orderId = orderId;
        this.itemCode = itemCode;
        this.qty = qty;
        this.unitPrice = unitPrice;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}

