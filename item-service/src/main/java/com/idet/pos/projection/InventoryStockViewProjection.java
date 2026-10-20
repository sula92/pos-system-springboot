package com.idet.pos.projection;

public interface InventoryStockViewProjection {
    String getItemCode();
    String getDescription();
    Double getUnitPrice();
    Integer getQty();
    Double getInventoryValue();
}

