package com.idet.pos.controller;

import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.service.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/order-detail")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Order Detail", description = "Order line-item lookup and analytics APIs")
public class OrderDetailController {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^O\\d{3}$");
    private static final Pattern ITEM_CODE_PATTERN = Pattern.compile("^I\\d{3}$");

    @Autowired
    private OrderDetailService orderDetailService;

    @GetMapping("/by-order")
    @Operation(summary = "Get order details by order id", description = "Fetch all order lines for a given order id")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderId(@Parameter(example = "O001") @RequestParam("orderId") String orderId) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderDetailService.findByOrderId(orderId));
    }

    @GetMapping("/by-order-graph")
    @Operation(summary = "Get order details by order id (entity graph)", description = "Fetch order lines and load parent order via entity graph")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderIdWithOrderGraph(@Parameter(example = "O001") @RequestParam("orderId") String orderId) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderDetailService.findByOrderIdWithOrderGraph(orderId));
    }

    @GetMapping("/by-order-join-fetch")
    @Operation(summary = "Get order details by order id (join fetch)", description = "Fetch order lines and parent order using JPQL join fetch")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderIdJoinFetchOrder(@Parameter(example = "O001") @RequestParam("orderId") String orderId) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderDetailService.findByOrderIdJoinFetchOrder(orderId));
    }

    @GetMapping("/by-order-native")
    @Operation(summary = "Get order details by order id (native)", description = "Fetch order lines for an order id using native SQL")
    @ApiResponse(responseCode = "200", description = "Order detail rows fetched")
    public ResponseEntity<List<OrderDetailDTO>> findByOrderIdNative(@Parameter(example = "O001") @RequestParam("orderId") String orderId) {
        validateOrderId(orderId);
        return ResponseEntity.ok(orderDetailService.findByOrderIdNative(orderId));
    }

    @GetMapping("/sum-qty-native")
    @Operation(summary = "Sum ordered quantity by item", description = "Return total ordered quantity for one item code using native SQL")
    @ApiResponse(responseCode = "200", description = "Total ordered quantity fetched")
    public ResponseEntity<Integer> sumOrderedQtyByItemCodeNative(@Parameter(example = "I001") @RequestParam("itemCode") String itemCode) {
        validateItemCode(itemCode);
        return ResponseEntity.ok(orderDetailService.sumOrderedQtyByItemCodeNative(itemCode));
    }


    private void validateOrderId(String orderId) {
        if (orderId == null || !ORDER_ID_PATTERN.matcher(orderId).matches()) {
            throw new InvalidRequestException("Invalid or missing order ID. Expected format: O followed by 3 digits (e.g. O001)");
        }
    }

    private void validateItemCode(String itemCode) {
        if (itemCode == null || !ITEM_CODE_PATTERN.matcher(itemCode).matches()) {
            throw new InvalidRequestException("Invalid or missing item code. Expected format: I followed by 3 digits (e.g. I001)");
        }
    }
}

