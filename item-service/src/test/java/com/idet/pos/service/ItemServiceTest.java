package com.idet.pos.service;

import com.idet.pos.dto.ItemDTO;
import com.idet.pos.entity.Inventory;
import com.idet.pos.entity.Item;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.repository.InventoryRepository;
import com.idet.pos.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void saveItem_whenInventoryMissing_createsInventoryRow() {
        ItemDTO input = new ItemDTO("I001", "Pen", 100.0);
        Item savedItem = new Item("I001", "Pen", 100.0);

        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);
        when(inventoryRepository.existsById("I001")).thenReturn(false);

        ItemDTO result = itemService.saveItem(input);

        assertEquals("I001", result.getCode());
        assertEquals("Pen", result.getDescription());
        assertEquals(100.0, result.getUnitPrice());

        verify(itemRepository).save(any(Item.class));
        verify(inventoryRepository).existsById("I001");

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertEquals("I001", inventoryCaptor.getValue().getItemCode());
        assertEquals(0, inventoryCaptor.getValue().getQty());
    }

    @Test
    void saveItem_whenInventoryExists_doesNotCreateInventoryRow() {
        ItemDTO input = new ItemDTO("I002", "Book", 500.0);
        Item savedItem = new Item("I002", "Book", 500.0);

        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);
        when(inventoryRepository.existsById("I002")).thenReturn(true);

        ItemDTO result = itemService.saveItem(input);

        assertEquals("I002", result.getCode());
        verify(inventoryRepository).existsById("I002");
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void updateItem_whenFound_updatesAndReturnsDto() {
        Item existing = new Item("I003", "Pencil", 80.0);
        Item updated = new Item("I003", "HB Pencil", 90.0);
        ItemDTO request = new ItemDTO("I003", "HB Pencil", 90.0);

        when(itemRepository.findById("I003")).thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(updated);

        ItemDTO result = itemService.updateItem(request);

        assertEquals("I003", result.getCode());
        assertEquals("HB Pencil", result.getDescription());
        assertEquals(90.0, result.getUnitPrice());
        verify(itemRepository).findById("I003");
        verify(itemRepository).save(existing);
    }

    @Test
    void updateItem_whenMissing_throwsResourceNotFoundException() {
        when(itemRepository.findById("I404")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> itemService.updateItem(new ItemDTO("I404", "Missing", 10.0)));

        verify(itemRepository).findById("I404");
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void deleteItem_whenExists_deletesInventoryThenItem() {
        when(itemRepository.existsById("I010")).thenReturn(true);
        when(inventoryRepository.existsById("I010")).thenReturn(true);

        itemService.deleteItem("I010");

        verify(itemRepository).existsById("I010");
        verify(inventoryRepository).existsById("I010");
        verify(inventoryRepository).deleteById("I010");
        verify(itemRepository).deleteById("I010");
    }

    @Test
    void deleteItem_whenMissing_throwsResourceNotFoundException() {
        when(itemRepository.existsById("I404")).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> itemService.deleteItem("I404"));

        verify(itemRepository).existsById("I404");
        verify(inventoryRepository, never()).deleteById(any(String.class));
    }
}
