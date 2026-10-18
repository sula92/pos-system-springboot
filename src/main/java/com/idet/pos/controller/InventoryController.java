package com.idet.pos.controller;

import com.idet.pos.dto.InventoryDTO;
import com.idet.pos.dto.InventoryStockValueView;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.projection.InventoryStockViewProjection;
import com.idet.pos.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.logging.Logger;

@RestController
@RequestMapping("/inventory")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Inventory", description = "Inventory stock management and reporting APIs")
public class InventoryController {

    private static final Logger logger = Logger.getLogger(InventoryController.class.getName());
    private static final Pattern CODE_PATTERN = Pattern.compile("^I\\d{3}$");

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get inventory", description = "Fetch all inventory rows or a single row by item code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory data fetched"),
            @ApiResponse(responseCode = "400", description = "Invalid item code format")
    })
    public ResponseEntity<?> getInventory(@Parameter(example = "I001") @RequestParam(name = "code", required = false) String code) {
        if (code != null) {
            if (!CODE_PATTERN.matcher(code).matches()) {
                throw new InvalidRequestException("Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)");
            }
            return ResponseEntity.ok(inventoryService.findInventory(code));
        }

        List<InventoryDTO> all = inventoryService.findAllInventories();
        return ResponseEntity.ok(all);
    }

    @PostMapping
    @Operation(summary = "Create inventory", description = "Create inventory for an item")
    @ApiResponse(responseCode = "201", description = "Inventory created")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Inventory payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "CreateInventory",
                            value = "{\n  \"itemCode\": \"I001\",\n  \"qty\": 100\n}"
                    )
            )
    )
    public ResponseEntity<?> createInventory(@Valid @RequestBody InventoryDTO dto) {
        validateCode(dto.getItemCode());
        InventoryDTO saved = inventoryService.createInventory(dto);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Inventory created successfully");
        response.put("data", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    @Operation(summary = "Update inventory", description = "Update inventory quantity for an item")
    @ApiResponse(responseCode = "200", description = "Inventory updated")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Inventory payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "UpdateInventory",
                            value = "{\n  \"itemCode\": \"I001\",\n  \"qty\": 85\n}"
                    )
            )
    )
    public ResponseEntity<?> updateInventory(@Valid @RequestBody InventoryDTO dto) {
        validateCode(dto.getItemCode());
        InventoryDTO updated = inventoryService.updateInventory(dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/stock-view")
    @Operation(summary = "Inventory stock view", description = "Get inventory stock with item details using interface projection")
    @ApiResponse(responseCode = "200", description = "Inventory stock view fetched")
    public ResponseEntity<List<InventoryStockViewProjection>> getInventoryStockView() {
        return ResponseEntity.ok(inventoryService.findInventoryStockViews());
    }

    @GetMapping("/stock-value-view")
    @Operation(summary = "Inventory stock value view", description = "Get inventory stock with value totals using DTO projection")
    @ApiResponse(responseCode = "200", description = "Inventory stock value view fetched")
    public ResponseEntity<List<InventoryStockValueView>> getInventoryStockValueView() {
        return ResponseEntity.ok(inventoryService.findInventoryStockValueViews());
    }


    @GetMapping("/low-stock")
    @Operation(summary = "Low stock items", description = "Get inventory entries with quantity less than or equal to maxQty")
    @ApiResponse(responseCode = "200", description = "Low stock entries fetched")
    public ResponseEntity<List<InventoryDTO>> getLowStock(@Parameter(example = "10") @RequestParam("maxQty") int maxQty) {
        if (maxQty < 0) {
            throw new InvalidRequestException("maxQty cannot be negative");
        }
        return ResponseEntity.ok(inventoryService.findLowStock(maxQty));
    }

    @GetMapping("/in-stock")
    @Operation(summary = "In-stock items", description = "Get inventory entries with quantity greater than minQty")
    @ApiResponse(responseCode = "200", description = "In-stock entries fetched")
    public ResponseEntity<List<InventoryDTO>> getInStockAbove(@Parameter(example = "0") @RequestParam("minQty") int minQty) {
        if (minQty < 0) {
            throw new InvalidRequestException("minQty cannot be negative");
        }
        return ResponseEntity.ok(inventoryService.findInStockAbove(minQty));
    }

    @GetMapping("/in-stock-native")
    @Operation(summary = "In-stock items (native)", description = "Get in-stock inventory entries via native SQL")
    @ApiResponse(responseCode = "200", description = "In-stock entries fetched")
    public ResponseEntity<List<InventoryDTO>> getInStockNative() {
        return ResponseEntity.ok(inventoryService.findInStockNative());
    }

    @GetMapping("/for-update")
    @Operation(summary = "Lock inventory row", description = "Fetch one inventory row with a pessimistic write lock")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory row locked and fetched"),
            @ApiResponse(responseCode = "404", description = "Inventory not found")
    })
    public ResponseEntity<InventoryDTO> getInventoryForUpdate(@Parameter(example = "I001") @RequestParam("code") String code) {
        validateCode(code);
        return ResponseEntity.ok(inventoryService.findInventoryForUpdate(code));
    }

    private void validateCode(String code) {
        logger.info("Validating inventory code: " + code);
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new InvalidRequestException("Invalid or missing item code. Expected format: I followed by 3 digits (e.g. I001)");
        }
    }
}

