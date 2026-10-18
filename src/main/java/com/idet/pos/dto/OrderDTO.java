package com.idet.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;

public class OrderDTO {
    @Schema(description = "Order ID", example = "O001", pattern = "^O\\d{3}$")
    private String orderId;

    @Schema(description = "Order date", example = "2026-10-17", format = "date")
    private LocalDate date;

    @Schema(description = "Customer ID", example = "C001", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^C\\d{3}$")
    @NotBlank(message = "Customer ID is required")
    @Pattern(regexp = "^C\\d{3}$", message = "Customer ID must match C followed by 3 digits (e.g. C001)")
    private String customerId;

    @Schema(description = "Order line items", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Order must contain at least one order detail")
    @Valid
    private List<OrderDetailDTO> orderDetails;

    public OrderDTO() {}

    public OrderDTO(String orderId, LocalDate date, String customerId, List<OrderDetailDTO> orderDetails) {
        this.orderId = orderId;
        this.date = date;
        this.customerId = customerId;
        this.orderDetails = orderDetails;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public List<OrderDetailDTO> getOrderDetails() { return orderDetails; }
    public void setOrderDetails(List<OrderDetailDTO> orderDetails) { this.orderDetails = orderDetails; }
}

