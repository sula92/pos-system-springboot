package com.idet.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ItemDTO {
    @Schema(description = "Item code", example = "I001", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^I\\d{3}$")
    @NotBlank(message = "Item code is required")
    @Pattern(regexp = "^I\\d{3}$", message = "Item code must match I followed by 3 digits (e.g. I001)")
    private String code;

    @Schema(description = "Item description", example = "Rice 1kg", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
    @NotBlank(message = "Item description is required")
    @Size(max = 255, message = "Item description must be at most 255 characters")
    private String description;

    @Schema(description = "Unit price", example = "250.00", minimum = "0")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be zero or positive")
    private double unitPrice;

    public ItemDTO() {}

    public ItemDTO(String code, String description, double unitPrice) {
        this.code = code;
        this.description = description;
        this.unitPrice = unitPrice;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
}

