package com.idet.pos.service;

import com.idet.pos.dto.InventoryDTO;
import com.idet.pos.dto.InventoryStockValueView;
import com.idet.pos.dto.StockLineDTO;
import com.idet.pos.entity.Inventory;
import com.idet.pos.entity.Item;
import com.idet.pos.exception.InsufficientStockException;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.projection.InventoryStockViewProjection;
import com.idet.pos.repository.InventoryRepository;
import com.idet.pos.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
    public List<InventoryDTO> findAllInventories() {
        List<InventoryDTO> dtos = new ArrayList<>();
        for (Inventory inventory : inventoryRepository.findAllOrderByItemCode()) {
            dtos.add(toDTO(inventory));
        }
        return dtos;
    }


    @Transactional(readOnly = true)
    public List<InventoryStockViewProjection> findInventoryStockViews() {
        return inventoryRepository.findInventoryStockViews();
    }

    @Transactional(readOnly = true)
    public List<InventoryStockValueView> findInventoryStockValueViews() {
        return inventoryRepository.findInventoryStockValueViews();
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findLowStock(int maxQty) {
        List<InventoryDTO> dtos = new ArrayList<>();
        for (Inventory inventory : inventoryRepository.findByQtyLessThanEqualOrderByQtyAsc(maxQty)) {
            dtos.add(toDTO(inventory));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findInStockAbove(int minQty) {
        List<InventoryDTO> dtos = new ArrayList<>();
        for (Inventory inventory : inventoryRepository.findByQtyGreaterThanOrderByQtyDesc(minQty)) {
            dtos.add(toDTO(inventory));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> findInStockNative() {
        List<InventoryDTO> dtos = new ArrayList<>();
        for (Inventory inventory : inventoryRepository.findInStockNative()) {
            dtos.add(toDTO(inventory));
        }
        return dtos;
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

    /**
     * Reserve stock for an order in one transaction.
     * Duplicate item codes are merged, every inventory row is locked (in item-code order),
     * and nothing is decremented unless every item has enough stock.
     */
    public void reserveStock(List<StockLineDTO> lines) {
        Map<String, Integer> requestedQtyByItem = mergeLines(lines);

        Map<String, Inventory> lockedInventory = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> requested : requestedQtyByItem.entrySet()) {
            String itemCode = requested.getKey();

            itemRepository.findById(itemCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemCode));

            Inventory inventory = inventoryRepository.findByItemCodeForUpdate(itemCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for item: " + itemCode));

            if (inventory.getQty() < requested.getValue()) {
                throw new InsufficientStockException("Insufficient stock for item: " + itemCode);
            }
            lockedInventory.put(itemCode, inventory);
        }

        for (Map.Entry<String, Inventory> entry : lockedInventory.entrySet()) {
            Inventory inventory = entry.getValue();
            inventory.setQty(inventory.getQty() - requestedQtyByItem.get(entry.getKey()));
            inventoryRepository.save(inventory);
        }
        logger.info("Service: Stock reserved for items: " + requestedQtyByItem.keySet());
    }

    /**
     * Release previously reserved stock (compensation when an order cannot be saved).
     */
    public void releaseStock(List<StockLineDTO> lines) {
        Map<String, Integer> releasedQtyByItem = mergeLines(lines);

        for (Map.Entry<String, Integer> released : releasedQtyByItem.entrySet()) {
            String itemCode = released.getKey();
            Inventory inventory = inventoryRepository.findByItemCodeForUpdate(itemCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for item: " + itemCode));
            inventory.setQty(inventory.getQty() + released.getValue());
            inventoryRepository.save(inventory);
        }
        logger.info("Service: Stock released for items: " + releasedQtyByItem.keySet());
    }

    private Map<String, Integer> mergeLines(List<StockLineDTO> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new InvalidRequestException("At least one stock line is required");
        }

        Map<String, Integer> qtyByItem = new TreeMap<>();
        for (StockLineDTO line : lines) {
            if (line == null || line.getItemCode() == null || line.getItemCode().isBlank() || line.getQty() <= 0) {
                throw new InvalidRequestException("Invalid stock line");
            }
            qtyByItem.merge(line.getItemCode(), line.getQty(), Integer::sum);
        }
        return qtyByItem;
    }

    private InventoryDTO toDTO(Inventory inventory) {
        return new InventoryDTO(inventory.getItemCode(), inventory.getQty());
    }
}

