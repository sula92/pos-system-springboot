package com.idet.pos.client;

/**
 * Item/quantity pair sent to item-service when reserving or releasing stock.
 */
public record StockLine(String itemCode, int qty) {
}
