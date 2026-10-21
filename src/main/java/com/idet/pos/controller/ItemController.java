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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import com.idet.pos.util.PageableUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@RestController
@RequestMapping("/item")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Item", description = "Item catalog and price management APIs")
@Validated
public class ItemController {

    private static final Logger logger = Logger.getLogger(ItemController.class.getName());
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("code", "description", "unitPrice");
    @Autowired
    private ItemService itemService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @GetMapping
    @Operation(summary = "Get items", description = "Fetch one page of items, or a single item by code")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid item code format")
    })
    public ResponseEntity<?> getItems(
            @Parameter(example = "I001")
            @RequestParam(name = "code", required = false)
            @Pattern(regexp = "^I\\d{3}$", message = "Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)")
            String code,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable
    ) {
        if (code != null) {
            validateItemCode(code);
            return ResponseEntity.ok(itemService.findItem(code));
        }

        logger.info("Fetching items page " + pageable);
        return ResponseEntity.ok(new PagedModel<>(itemService.findAllItems(sortable(pageable))));
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
    public ResponseEntity<?> deleteItem(
            @Parameter(example = "I001")
            @RequestParam("code")
            @Pattern(regexp = "^I\\d{3}$", message = "Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)")
            String code
    ) {
        logger.info("Attempting to delete item: " + code);
        validateItemCode(code);
        itemService.deleteItem(code);
        logger.info("Item deleted successfully: " + code);
        return ResponseEntity.ok(successResponse("Item deleted successfully"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search items by description", description = "Search items by description keyword")
    @ApiResponse(responseCode = "200", description = "Matching items fetched")
    public ResponseEntity<PagedModel<ItemDTO>> searchItems(
            @Parameter(example = "rice")
            @RequestParam("keyword")
            @NotBlank(message = "Keyword is required for item search")
            String keyword,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(itemService.searchItemsByDescription(keyword, sortable(pageable))));
    }


    @GetMapping("/by-min-price")
    @Operation(summary = "Get items by minimum price", description = "Fetch items with unit price greater than or equal to minPrice")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<PagedModel<ItemDTO>> getItemsByMinPrice(
            @Parameter(example = "100.00")
            @RequestParam("minPrice")
            @PositiveOrZero(message = "minPrice cannot be negative")
            double minPrice,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(itemService.findItemsByMinPrice(minPrice, sortable(pageable))));
    }

    @GetMapping("/by-code-part")
    @Operation(summary = "Get items by code part", description = "Search items whose code contains the provided text")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<PagedModel<ItemDTO>> getItemsByCodePart(
            @Parameter(example = "I0")
            @RequestParam("codePart")
            @NotBlank(message = "codePart is required")
            String codePart,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(itemService.findItemsByCodeContains(codePart, sortable(pageable))));
    }

    @GetMapping("/by-price-range")
    @Operation(summary = "Get items by price range", description = "Fetch items within an inclusive min/max unit price range")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<PagedModel<ItemDTO>> getItemsByPriceRange(
            @Parameter(example = "100.00")
            @RequestParam("minPrice")
            @PositiveOrZero(message = "Invalid price range")
            double minPrice,
            @Parameter(example = "500.00")
            @RequestParam("maxPrice")
            @PositiveOrZero(message = "Invalid price range")
            double maxPrice,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE, sort = "code") Pageable pageable
    ) {
        if (minPrice > maxPrice) {
            throw new InvalidRequestException("Invalid price range");
        }
        return ResponseEntity.ok(new PagedModel<>(itemService.findItemsByPriceRange(minPrice, maxPrice, sortable(pageable))));
    }

    @GetMapping("/by-code-exact")
    @Operation(summary = "Get items by exact code", description = "Fetch items by exact code value")
    @ApiResponse(responseCode = "200", description = "Items fetched successfully")
    public ResponseEntity<List<ItemDTO>> getItemsByCodeExact(
            @Parameter(example = "I001")
            @RequestParam("code")
            @NotBlank(message = "code is required")
            String code
    ) {
        return ResponseEntity.ok(itemService.findItemsByCodeExact(code));
    }

    @GetMapping("/exists-by-description")
    @Operation(summary = "Check item description exists", description = "Return whether an item description already exists (case-insensitive)")
    @ApiResponse(responseCode = "200", description = "Existence status returned")
    public ResponseEntity<Map<String, Object>> existsByDescription(
            @Parameter(example = "Rice 1kg")
            @RequestParam("description")
            @NotBlank(message = "description is required")
            String description
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("exists", itemService.existsByDescriptionIgnoreCase(description));
        return ResponseEntity.ok(response);
    }

    private Pageable sortable(Pageable pageable) {
        return PageableUtils.requireSortable(pageable, SORTABLE_PROPERTIES);
    }

    private Map<String, Object> successResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        return response;
    }

    private void validateItemCode(String code) {
        Set<ConstraintViolation<ItemCodeRequest>> violations = validator.validateValue(ItemCodeRequest.class, "code", code);
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations.iterator().next().getMessage());
        }
    }

    private static class ItemCodeRequest {
        @Pattern(regexp = "^I\\d{3}$", message = "Invalid item code format. Expected format: I followed by 3 digits (e.g. I001)")
        private String code;
    }

}
