package com.idet.pos.controller;

import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.service.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/order-detail")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Order Detail", description = "Order line-item lookup and analytics APIs")
@Validated
public class OrderDetailController {

    @Autowired
    private OrderDetailService orderDetailService;

    @GetMapping("/by-order")
    @Operation(summary = "Get order details by order id", description = "Fetch all order lines for a given order id")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderId(
            @Parameter(example = "O001")
            @RequestParam("orderId")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid or missing order ID. Expected format: O followed by 3 digits (e.g. O001)")
            String orderId
    ) {
        return ResponseEntity.ok(orderDetailService.findByOrderId(orderId));
    }

    @GetMapping("/by-order-graph")
    @Operation(summary = "Get order details by order id (entity graph)", description = "Fetch order lines and load parent order via entity graph")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderIdWithOrderGraph(
            @Parameter(example = "O001")
            @RequestParam("orderId")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid or missing order ID. Expected format: O followed by 3 digits (e.g. O001)")
            String orderId
    ) {
        return ResponseEntity.ok(orderDetailService.findByOrderIdWithOrderGraph(orderId));
    }

    @GetMapping("/by-order-join-fetch")
    @Operation(summary = "Get order details by order id (join fetch)", description = "Fetch order lines and parent order using JPQL join fetch")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderIdJoinFetchOrder(
            @Parameter(example = "O001")
            @RequestParam("orderId")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid or missing order ID. Expected format: O followed by 3 digits (e.g. O001)")
            String orderId
    ) {
        return ResponseEntity.ok(orderDetailService.findByOrderIdJoinFetchOrder(orderId));
    }

    @GetMapping("/by-order-native")
    @Operation(summary = "Get order details by order id (native)", description = "Fetch order lines for an order id using native SQL")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderIdNative(
            @Parameter(example = "O001")
            @RequestParam("orderId")
            @Pattern(regexp = "^O\\d{3}$", message = "Invalid or missing order ID. Expected format: O followed by 3 digits (e.g. O001)")
            String orderId
    ) {
        return ResponseEntity.ok(orderDetailService.findByOrderIdNative(orderId));
    }

    @GetMapping("/sum-qty-native")
    @Operation(summary = "Sum ordered quantity by item", description = "Return total ordered quantity for one item code using native SQL")
    @ApiResponse(responseCode = "200", description = "Total ordered quantity fetched")
    public ResponseEntity<Integer> sumOrderedQtyByItemCodeNative(
            @Parameter(example = "I001")
            @RequestParam("itemCode")
            @Pattern(regexp = "^I\\d{3}$", message = "Invalid or missing item code. Expected format: I followed by 3 digits (e.g. I001)")
            String itemCode
    ) {
        return ResponseEntity.ok(orderDetailService.sumOrderedQtyByItemCodeNative(itemCode));
    }
}

