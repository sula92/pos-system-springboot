package com.idet.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class InventoryDTO {
    @Schema(description = "Item code", example = "I001", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^I\\d{3}$")
    @NotBlank(message = "Item code is required")
    @Pattern(regexp = "^I\\d{3}$", message = "Item code must match I followed by 3 digits (e.g. I001)")
    private String itemCode;

    @Schema(description = "Quantity on hand", example = "120", minimum = "0")
    @Min(value = 0, message = "Quantity must be zero or positive")
    private int qty;

    public InventoryDTO() {}

    public InventoryDTO(String itemCode, int qty) {
        this.itemCode = itemCode;
        this.qty = qty;
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}

