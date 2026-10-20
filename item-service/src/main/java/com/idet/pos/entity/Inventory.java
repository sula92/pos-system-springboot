package com.idet.pos.entity;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

/**
 * Inventory Entity
 * Tracks stock levels for each item.
 */
@Entity
@Table(name = "inventory")
@NamedQueries({
        @NamedQuery(
                name = "Inventory.findInStock",
                query = "SELECT i FROM Inventory i WHERE i.qty > 0 ORDER BY i.itemCode"
        ),
        @NamedQuery(
                name = "Inventory.findByItemCode",
                query = "SELECT i FROM Inventory i WHERE i.itemCode = :itemCode"
        )
})
@NamedNativeQueries({
        @NamedNativeQuery(
                name = "Inventory.findInStockNative",
                query = "SELECT * FROM inventory WHERE qty_on_hand > 0 ORDER BY item_code",
                resultClass = Inventory.class
        ),
        @NamedNativeQuery(
                name = "Inventory.findByItemCodeNative",
                query = "SELECT * FROM inventory WHERE item_code = :itemCode",
                resultClass = Inventory.class
        )
})
public class Inventory implements Persistable<String> {

    @Id
    @Column(name = "item_code", length = 10)
    private String itemCode;

    @Column(name = "qty_on_hand", nullable = false)
    private int qty;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code")
    private Item item;

    // New rows must be persisted, not merged: merging an entity whose id comes from @MapsId fails in Hibernate 7.
    @Transient
    private boolean isNew = true;

    public Inventory() {}

    public Inventory(String itemCode, int qty, Item item) {
        this.itemCode = itemCode;
        this.qty = qty;
        this.item = item;
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    @Override
    public String getId() { return itemCode; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }
}

