package com.idet.pos.projection;

import java.time.LocalDate;

public interface OrderSummaryProjection {
    String getOrderId();
    LocalDate getOrderDate();
    String getCustomerId();
    Long getLineCount();
    Long getTotalQty();
    Double getGrandTotal();
}

