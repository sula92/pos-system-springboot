package com.idet.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One item/quantity pair used by the internal stock reservation endpoints.
 */
public class StockLineDTO {
    @Schema(description = "Item code", example = "I001")
    private String itemCode;

    @Schema(description = "Quantity to reserve or release", example = "2", minimum = "1")
    private int qty;

    public StockLineDTO() {}

    public StockLineDTO(String itemCode, int qty) {
        this.itemCode = itemCode;
        this.qty = qty;
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }
}
