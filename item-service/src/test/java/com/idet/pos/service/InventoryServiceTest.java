package com.idet.pos.service;

import com.idet.pos.dto.StockLineDTO;
import com.idet.pos.entity.Inventory;
import com.idet.pos.entity.Item;
import com.idet.pos.exception.InsufficientStockException;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.repository.InventoryRepository;
import com.idet.pos.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void reserveStock_whenEnoughStock_decrementsInventory() {
        Item pen = new Item("I001", "Pen", 100.0);
        Inventory inventory = new Inventory("I001", 10, pen);

        when(itemRepository.findById("I001")).thenReturn(Optional.of(pen));
        when(inventoryRepository.findByItemCodeForUpdate("I001")).thenReturn(Optional.of(inventory));

        inventoryService.reserveStock(List.of(new StockLineDTO("I001", 3)));

        assertEquals(7, inventory.getQty());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveStock_mergesDuplicateItemCodesBeforeCheckingStock() {
        Item pen = new Item("I001", "Pen", 100.0);
        Inventory inventory = new Inventory("I001", 5, pen);

        when(itemRepository.findById("I001")).thenReturn(Optional.of(pen));
        when(inventoryRepository.findByItemCodeForUpdate("I001")).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveStock(List.of(
                new StockLineDTO("I001", 3),
                new StockLineDTO("I001", 3)
        )));

        assertEquals(5, inventory.getQty());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void reserveStock_whenOneItemLacksStock_decrementsNothing() {
        Item pen = new Item("I001", "Pen", 100.0);
        Item book = new Item("I002", "Book", 500.0);
        Inventory penStock = new Inventory("I001", 10, pen);
        Inventory bookStock = new Inventory("I002", 1, book);

        when(itemRepository.findById("I001")).thenReturn(Optional.of(pen));
        when(itemRepository.findById("I002")).thenReturn(Optional.of(book));
        when(inventoryRepository.findByItemCodeForUpdate("I001")).thenReturn(Optional.of(penStock));
        when(inventoryRepository.findByItemCodeForUpdate("I002")).thenReturn(Optional.of(bookStock));

        assertThrows(InsufficientStockException.class, () -> inventoryService.reserveStock(List.of(
                new StockLineDTO("I001", 2),
                new StockLineDTO("I002", 2)
        )));

        assertEquals(10, penStock.getQty());
        assertEquals(1, bookStock.getQty());
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void reserveStock_whenItemMissing_throwsResourceNotFoundException() {
        when(itemRepository.findById("I999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> inventoryService.reserveStock(List.of(new StockLineDTO("I999", 1))));

        verify(inventoryRepository, never()).findByItemCodeForUpdate(any(String.class));
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void reserveStock_whenNoLines_throwsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () -> inventoryService.reserveStock(List.of()));
    }

    @Test
    void releaseStock_addsQuantityBack() {
        Inventory inventory = new Inventory("I001", 4, new Item("I001", "Pen", 100.0));

        when(inventoryRepository.findByItemCodeForUpdate("I001")).thenReturn(Optional.of(inventory));

        inventoryService.releaseStock(List.of(new StockLineDTO("I001", 2), new StockLineDTO("I001", 1)));

        assertEquals(7, inventory.getQty());
        verify(inventoryRepository).save(inventory);
    }
}
