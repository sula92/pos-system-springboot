package com.idet.pos.service;

import com.idet.pos.dto.InventoryDTO;
import com.idet.pos.dto.InventoryStockValueView;
import com.idet.pos.entity.Inventory;
import com.idet.pos.entity.Item;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.projection.InventoryStockViewProjection;
import com.idet.pos.repository.InventoryRepository;
import com.idet.pos.repository.ItemRepository;
import com.idet.pos.util.PageableUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
@Transactional
public class InventoryService {

    private static final Logger logger = Logger.getLogger(InventoryService.class.getName());

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public InventoryDTO findInventory(String itemCode) {
        Inventory inventory = inventoryRepository.findById(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for item: " + itemCode));
        return toDTO(inventory);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDTO> findAllInventories(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::toDTO);
    }


    @Transactional(readOnly = true)
    public Page<InventoryStockViewProjection> findInventoryStockViews(Pageable pageable) {
        return inventoryRepository.findInventoryStockViews(PageableUtils.withoutSort(pageable));
    }

    @Transactional(readOnly = true)
    public Page<InventoryStockValueView> findInventoryStockValueViews(Pageable pageable) {
        return inventoryRepository.findInventoryStockValueViews(PageableUtils.withoutSort(pageable));
    }

    @Transactional(readOnly = true)
    public Page<InventoryDTO> findLowStock(int maxQty, Pageable pageable) {
        return inventoryRepository.findByQtyLessThanEqualOrderByQtyAsc(maxQty, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDTO> findInStockAbove(int minQty, Pageable pageable) {
        return inventoryRepository.findByQtyGreaterThanOrderByQtyDesc(minQty, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<InventoryDTO> findInStockNative(Pageable pageable) {
        return inventoryRepository.findInStockNative(PageableUtils.withoutSort(pageable)).map(this::toDTO);
    }

    public InventoryDTO findInventoryForUpdate(String itemCode) {
        Inventory inventory = inventoryRepository.findByItemCodeForUpdate(itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for item: " + itemCode));
        return toDTO(inventory);
    }

    public InventoryDTO createInventory(InventoryDTO dto) {
        if (inventoryRepository.existsById(dto.getItemCode())) {
            throw new InvalidRequestException("Inventory already exists for item: " + dto.getItemCode());
        }

        Item item = itemRepository.findById(dto.getItemCode())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + dto.getItemCode()));

        Inventory saved = inventoryRepository.save(new Inventory(dto.getItemCode(), dto.getQty(), item));
        logger.info("Service: Inventory created for item: " + dto.getItemCode());
        return toDTO(saved);
    }

    public InventoryDTO updateInventory(InventoryDTO dto) {
        Inventory inventory = inventoryRepository.findById(dto.getItemCode())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for item: " + dto.getItemCode()));

        inventory.setQty(dto.getQty());
        Inventory updated = inventoryRepository.save(inventory);
        logger.info("Service: Inventory updated for item: " + dto.getItemCode());
        return toDTO(updated);
    }


    private InventoryDTO toDTO(Inventory inventory) {
        return new InventoryDTO(inventory.getItemCode(), inventory.getQty());
    }
}

