package com.idet.pos.batch;

/**
 * One line of the item import CSV: code,description,unitPrice,qty.
 */
public record ItemCsvRow(String code, String description, double unitPrice, int qty) {
}
