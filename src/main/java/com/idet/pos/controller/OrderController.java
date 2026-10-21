package com.idet.pos.controller;

import com.idet.pos.service.OrderService;
import com.idet.pos.dto.OrderDTO;
import com.idet.pos.dto.OrderSummaryDTO;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.projection.OrderSummary;
import com.idet.pos.projection.OrderSummaryProjection;
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
import com.idet.pos.util.PageableUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@RestController
@RequestMapping("/order")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Order", description = "Order placement, retrieval, and order reporting APIs")
@Validated
public class OrderController {

    private static final Logger logger = Logger.getLogger(OrderController.class.getName());
    private static final Set<String> SORTABLE_PROPERTIES = Set.of("orderId", "date", "customerId");
    @Autowired
    private OrderService orderService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @GetMapping
    @Operation(summary = "Get orders", description = "Fetch one page of orders, or one order by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders fetched successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order id format")
    })
    public ResponseEntity<?> getOrders(
            @Parameter(example = "O001")
            @RequestParam(name = "id", required = false)
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
            String id,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "orderId")
            }) Pageable pageable
    ) {
        if (id != null) {
            validateOrderId(id);
            return ResponseEntity.ok(orderService.findOrder(id));
        }

        logger.info("Fetching orders page " + pageable);
        return ResponseEntity.ok(new PagedModel<>(orderService.findAllOrders(sortable(pageable))));
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Place a new order with order details and stock deduction")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "409", description = "Insufficient stock")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Order payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "CreateOrder",
                            value = "{\n  \"orderId\": \"O001\",\n  \"date\": \"2026-10-17\",\n  \"customerId\": \"C001\",\n  \"orderDetails\": [\n    {\n      \"orderId\": \"O001\",\n      \"itemCode\": \"I001\",\n      \"qty\": 2,\n      \"unitPrice\": 250.00\n    },\n    {\n      \"orderId\": \"O001\",\n      \"itemCode\": \"I002\",\n      \"qty\": 1,\n      \"unitPrice\": 450.00\n    }\n  ]\n}"
                    )
            )
    )
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderDTO dto) {
        logger.info("Attempting to place new order");
        // Set default date if not provided
        if (dto.getDate() == null) {
            dto.setDate(LocalDate.now());
        }
        OrderDTO placed = orderService.placeOrder(dto);
        logger.info("Order placed successfully with ID: " + placed.getOrderId());
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "Order placed successfully");
        resp.put("data", placed);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/summary")
    @Operation(summary = "Order summary projection", description = "Get aggregated order summaries using interface projection")
    @ApiResponse(responseCode = "200", description = "Order summaries fetched")
    public ResponseEntity<PagedModel<OrderSummaryProjection>> getOrderSummaries(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findOrderSummaries(pageable)));
    }

    @GetMapping("/summary-dto")
    @Operation(summary = "Order summary DTO", description = "Get aggregated order summaries as DTO objects")
    @ApiResponse(responseCode = "200", description = "Order summary DTOs fetched")
    public ResponseEntity<PagedModel<OrderSummaryDTO>> getOrderSummariesDto(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findOrderSummariesDto(pageable)));
    }


    @GetMapping("/details-graph")
    @Operation(summary = "Order by details graph", description = "Fetch a single order by id using entity graph")
    @ApiResponse(responseCode = "200", description = "Order fetched")
    public ResponseEntity<OrderDTO> getOrderByDetailsGraph(
            @Parameter(example = "O001")
            @RequestParam("id")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
            String id
    ) {
        validateOrderId(id);
        return ResponseEntity.ok(orderService.findOrderByDetailsGraph(id));
    }

    @GetMapping("/all-details-graph")
    @Operation(summary = "All orders by details graph", description = "Fetch all orders using entity graph for details")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<PagedModel<OrderDTO>> getAllOrdersByDetailsGraph(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "orderId")
            }) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findAllOrdersByDetailsGraph(sortable(pageable))));
    }

    @GetMapping("/native")
    @Operation(summary = "Order by native SQL", description = "Fetch a single order by id using native SQL")
    @ApiResponse(responseCode = "200", description = "Order fetched")
    public ResponseEntity<OrderDTO> getOrderByNative(
            @Parameter(example = "O001")
            @RequestParam("id")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
            String id
    ) {
        validateOrderId(id);
        return ResponseEntity.ok(orderService.findOrderByIdNative(id));
    }

    @GetMapping("/native-all")
    @Operation(summary = "All orders by native SQL", description = "Fetch all orders ordered by date using native SQL")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<PagedModel<OrderDTO>> getAllOrdersNative(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findAllOrdersNativeOrderByDateDesc(pageable)));
    }

    @GetMapping("/count-by-customer-native")
    @Operation(summary = "Count orders by customer", description = "Get native SQL order count for a customer id")
    @ApiResponse(responseCode = "200", description = "Order count fetched")
    public ResponseEntity<Map<String, Object>> countByCustomerNative(
            @Parameter(example = "C001")
            @RequestParam("customerId")
            @NotBlank(message = "customerId is required")
            String customerId
    ) {
        validateCustomerIdRequired(customerId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("customerId", customerId);
        response.put("orderCount", orderService.countOrdersByCustomerIdNative(customerId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-customer")
    @Operation(summary = "Get orders by customer", description = "Fetch orders for a given customer id")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<PagedModel<OrderDTO>> getOrdersByCustomer(
            @Parameter(example = "C001")
            @RequestParam("customerId")
            @NotBlank(message = "customerId is required")
            String customerId,
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "orderId")
            }) Pageable pageable
    ) {
        validateCustomerIdRequired(customerId);
        return ResponseEntity.ok(new PagedModel<>(orderService.findOrdersByCustomerId(customerId, sortable(pageable))));
    }

    @GetMapping("/by-id-with-details")
    @Operation(summary = "Order by id with details", description = "Fetch orders by id with details via JPQL query")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<List<OrderDTO>> getOrdersByIdWithDetails(
            @Parameter(example = "O001")
            @RequestParam("id")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
            String id
    ) {
        validateOrderId(id);
        return ResponseEntity.ok(orderService.findOrdersByIdWithDetails(id));
    }

    @GetMapping("/by-id-with-details-graph2")
    @Operation(summary = "Order by id with details graph 2", description = "Fetch orders by id with alternate details graph query")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<List<OrderDTO>> getOrdersByIdWithDetailsGraph2(
            @Parameter(example = "O001")
            @RequestParam("id")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
            String id
    ) {
        validateOrderId(id);
        return ResponseEntity.ok(orderService.findOrdersByIdWithDetailsGraph2(id));
    }

    @GetMapping("/summary-constructor")
    @Operation(summary = "Order summary constructor projection", description = "Get order summaries built by constructor projection")
    @ApiResponse(responseCode = "200", description = "Order summaries fetched")
    public ResponseEntity<PagedModel<OrderSummary>> getOrderSummariesWithConstructor(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findOrderSummariesWithConstructor(pageable)));
    }

    @GetMapping("/all-with-customer")
    @Operation(summary = "All orders with customer", description = "Fetch orders with customer data query variant")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<PagedModel<OrderDTO>> getAllWithCustomer(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "orderId")
            }) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findAllOrdersWithCustomer(sortable(pageable))));
    }

    @GetMapping("/all-with-customer-details")
    @Operation(summary = "All orders with customer and details", description = "Fetch orders with customer/details query variant")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<PagedModel<OrderDTO>> getAllWithCustomerAndDetails(
            @ParameterObject @PageableDefault(size = PageableUtils.DEFAULT_PAGE_SIZE)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "date", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "orderId")
            }) Pageable pageable
    ) {
        return ResponseEntity.ok(new PagedModel<>(orderService.findAllOrdersWithCustomerAndDetails(sortable(pageable))));
    }

    @GetMapping("/by-id-with-customer")
    @Operation(summary = "Order by id with customer", description = "Fetch orders by id with customer query variant")
    @ApiResponse(responseCode = "200", description = "Orders fetched")
    public ResponseEntity<List<OrderDTO>> getByIdWithCustomer(
            @Parameter(example = "O001")
            @RequestParam("id")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
            String id
    ) {
        validateOrderId(id);
        return ResponseEntity.ok(orderService.findOrdersByIdWithCustomer(id));
    }

    private Pageable sortable(Pageable pageable) {
        return PageableUtils.requireSortable(pageable, SORTABLE_PROPERTIES);
    }

    private void validateOrderId(String id) {
        Set<ConstraintViolation<OrderIdRequest>> violations = validator.validateValue(OrderIdRequest.class, "id", id);
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations.iterator().next().getMessage());
        }
    }

    private void validateCustomerIdRequired(String customerId) {
        Set<ConstraintViolation<CustomerIdRequest>> violations = validator.validateValue(CustomerIdRequest.class, "customerId", customerId);
        if (!violations.isEmpty()) {
            throw new InvalidRequestException(violations.iterator().next().getMessage());
        }
    }

    private static class OrderIdRequest {
        @Pattern(regexp = "^O\\d{3}$", message = "Invalid order ID format. Expected format: O followed by 3 digits (e.g. O001)")
        private String id;
    }

    private static class CustomerIdRequest {
        @NotBlank(message = "customerId is required")
        private String customerId;
    }

}
