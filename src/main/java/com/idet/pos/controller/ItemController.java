package com.idet.pos.controller;

import com.idet.pos.service.ItemService;
import com.idet.pos.dto.ItemDTO;
import com.idet.pos.exception.InvalidRequestException;
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
@RequestMapping("/item")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Item", description = "Item catalog and price management APIs")
public class ItemController {

    private static final Logger logger = Logger.getLogger(ItemController.class.getName());
    private static final Pattern CODE_PATTERN = Pattern.compile("^I\\d{3}$");

    @Autowired
    private ItemService itemService;

    @GetMapping
    @Operation(summary = "Get items", description = "Fetch all items or a single item by code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid item code format")
    })
    public ResponseEntity<?> getItems(@Parameter(example = "I001") @RequestParam(name = "code", required = false) String code) {
        if (code != null) {
            if (!CODE_PATTERN.matcher(code).matches()) {
                logger.warning("Invalid item code format: " + code);
                throw new InvalidRequestException("Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)");
            }
            return ResponseEntity.ok(itemService.findItem(code));
        }

        logger.info("Fetching all items");
        List<ItemDTO> all = itemService.findAllItems();
        return ResponseEntity.ok(all);
    }

    @PostMapping
    @Operation(summary = "Create item", description = "Create a new item")
    @ApiResponse(responseCode = "201", description = "Item created successfully")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Item payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "CreateItem",
                            value = "{\n  \"code\": \"I001\",\n  \"description\": \"Rice 1kg\",\n  \"unitPrice\": 250.00\n}"
                    )
            )
    )
    public ResponseEntity<?> createItem(@Valid @RequestBody ItemDTO dto) {
        logger.info("Attempting to save new item");
        ItemDTO saved = itemService.saveItem(dto);
        logger.info("Item saved successfully with code: " + saved.getCode());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "Item saved successfully");
        resp.put("data", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping
    @Operation(summary = "Update item", description = "Update an existing item")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Item payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "UpdateItem",
                            value = "{\n  \"code\": \"I001\",\n  \"description\": \"Rice 1kg Premium\",\n  \"unitPrice\": 275.00\n}"
                    )
            )
    )
    public ResponseEntity<?> updateItem(@Valid @RequestBody ItemDTO dto) {
        String code = dto.getCode();
        logger.info("Attempting to update item: " + code);
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            logger.warning("Invalid or missing item code for update: " + code);
            throw new InvalidRequestException("Invalid or missing item code. Expected format: I followed by 3 digits (e.g. I001)");
        }
        ItemDTO updated = itemService.updateItem(dto);
        logger.info("Item updated successfully: " + code);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping
    @Operation(summary = "Delete item", description = "Delete an item by code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<?> deleteItem(@Parameter(example = "I001") @RequestParam("code") String code) {
        logger.info("Attempting to delete item: " + code);
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            logger.warning("Invalid item code format for delete: " + code);
            throw new InvalidRequestException("Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)");
        }
        itemService.deleteItem(code);
        logger.info("Item deleted successfully: " + code);
        return ResponseEntity.ok(successResponse("Item deleted successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search items by description", description = "Search items by description keyword")
    @ApiResponse(responseCode = "200", description = "Matching items fetched")
    public ResponseEntity<List<ItemDTO>> searchItems(@Parameter(example = "rice") @RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new InvalidRequestException("Keyword is required for item search");
        }
        return ResponseEntity.ok(itemService.searchItemsByDescription(keyword));
    }


    @GetMapping("/by-min-price")
    @Operation(summary = "Get items by minimum price", description = "Fetch items with unit price greater than or equal to minPrice")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<List<ItemDTO>> getItemsByMinPrice(@Parameter(example = "100.00") @RequestParam("minPrice") double minPrice) {
        if (minPrice < 0) {
            throw new InvalidRequestException("minPrice cannot be negative");
        }
        return ResponseEntity.ok(itemService.findItemsByMinPrice(minPrice));
    }

    @GetMapping("/by-code-part")
    @Operation(summary = "Get items by code part", description = "Search items whose code contains the provided text")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<List<ItemDTO>> getItemsByCodePart(@Parameter(example = "I0") @RequestParam("codePart") String codePart) {
        if (codePart == null || codePart.trim().isEmpty()) {
            throw new InvalidRequestException("codePart is required");
        }
        return ResponseEntity.ok(itemService.findItemsByCodeContains(codePart));
    }

    @GetMapping("/by-price-range")
    @Operation(summary = "Get items by price range", description = "Fetch items within an inclusive min/max unit price range")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<List<ItemDTO>> getItemsByPriceRange(
            @Parameter(example = "100.00") @RequestParam("minPrice") double minPrice,
            @Parameter(example = "500.00") @RequestParam("maxPrice") double maxPrice
    ) {
        if (minPrice < 0 || maxPrice < 0 || minPrice > maxPrice) {
            throw new InvalidRequestException("Invalid price range");
        }
        return ResponseEntity.ok(itemService.findItemsByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/by-code-exact")
    @Operation(summary = "Get items by exact code", description = "Fetch items by exact code value")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<List<ItemDTO>> getItemsByCodeExact(@Parameter(example = "I001") @RequestParam("code") String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new InvalidRequestException("code is required");
        }
        return ResponseEntity.ok(itemService.findItemsByCodeExact(code));
    }

    @GetMapping("/exists-by-description")
    @Operation(summary = "Check item description exists", description = "Return whether an item description already exists (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Existence status returned")
    public ResponseEntity<Map<String, Object>> existsByDescription(@Parameter(example = "Rice 1kg") @RequestParam("description") String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new InvalidRequestException("description is required");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("exists", itemService.existsByDescriptionIgnoreCase(description));
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> successResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        return response;
    }
}
