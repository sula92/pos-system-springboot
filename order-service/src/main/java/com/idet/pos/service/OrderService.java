package com.idet.pos.service;

import com.idet.pos.client.CustomerClient;
import com.idet.pos.client.CustomerResponse;
import com.idet.pos.client.InventoryClient;
import com.idet.pos.client.StockLine;
import com.idet.pos.dto.CustomerOrderTotalsDTO;
import com.idet.pos.dto.OrderDTO;
import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.dto.OrderSummaryDTO;
import com.idet.pos.entity.Order;
import com.idet.pos.entity.OrderDetail;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.projection.OrderSummary;
import com.idet.pos.projection.OrderSummaryProjection;
import com.idet.pos.repository.OrderDetailRepository;
import com.idet.pos.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * OrderService - Business Logic Layer
 * Handles all order-related operations. Local reads and writes are transactional;
 * customer and stock checks are remote calls to customer-service and item-service.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = Logger.getLogger(OrderService.class.getName());

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CustomerClient customerClient;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Place an order across services.
     * 1. The customer is checked in customer-service (Feign).
     * 2. Stock is checked and reserved atomically in item-service (Feign).
     * 3. The order and its details are saved in a local transaction.
     *
     * There is no shared transaction any more: if step 3 fails, the reserved stock is
     * released again (compensation). Remote calls run outside the database transaction.
     * @param dto Order DTO containing order details
     * @return OrderDTO with generated order ID
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public OrderDTO placeOrder(OrderDTO dto) {
        logger.info("Service: Attempting to place order for customer: " + dto.getCustomerId());

        if (!isValidOrderRequest(dto)) {
            logger.warning("Service: Invalid order request");
            throw new InvalidRequestException("Invalid order request");
        }

        List<StockLine> stockLines = new ArrayList<>();
        for (OrderDetailDTO detail : dto.getOrderDetails()) {
            if (!isValidOrderDetail(detail)) {
                throw new InvalidRequestException("Invalid order detail for item: " + detail.getItemCode());
            }
            stockLines.add(new StockLine(detail.getItemCode(), detail.getQty()));
        }

        CustomerResponse customer = customerClient.getCustomer(dto.getCustomerId());
        logger.info("Service: Customer found: " + customer.id());

        inventoryClient.reserve(stockLines);
        logger.info("Service: All items validated and stock reserved");

        try {
            return transactionTemplate.execute(status -> saveOrder(dto));
        } catch (RuntimeException ex) {
            logger.warning("Service: Saving order failed, releasing reserved stock: " + ex.getMessage());
            releaseReservedStock(stockLines);
            throw ex;
        }
    }

    private OrderDTO saveOrder(OrderDTO dto) {
        Order orderEntity = new Order(
                generateOrderId(),
                dto.getDate() != null ? dto.getDate() : LocalDate.now(),
                dto.getCustomerId()
        );
        logger.info("Service: Generated order ID: " + orderEntity.getOrderId());

        Order savedOrder = orderRepository.save(orderEntity);
        logger.info("Service: Order record saved: " + savedOrder.getOrderId());

        for (OrderDetailDTO detail : dto.getOrderDetails()) {
            detail.setOrderId(savedOrder.getOrderId());

            OrderDetail detailEntity = new OrderDetail(
                    savedOrder.getOrderId(), detail.getItemCode(), detail.getQty(), detail.getUnitPrice());
            orderDetailRepository.save(detailEntity);
            logger.info("Service: Order detail saved: " + savedOrder.getOrderId() + " - " + detail.getItemCode());
        }

        logger.info("Service: Order transaction committed successfully for order: " + savedOrder.getOrderId());

        List<OrderDetailDTO> detailDTOs = new ArrayList<>();
        for (OrderDetail d : orderDetailRepository.findByOrderId(savedOrder.getOrderId())) {
            detailDTOs.add(new OrderDetailDTO(d.getOrderId(), d.getItemCode(), d.getQty(), d.getUnitPrice()));
        }
        return new OrderDTO(savedOrder.getOrderId(), savedOrder.getDate(), savedOrder.getCustomerId(), detailDTOs);
    }

    private void releaseReservedStock(List<StockLine> stockLines) {
        try {
            inventoryClient.release(stockLines);
            logger.info("Service: Reserved stock released");
        } catch (RuntimeException releaseEx) {
            logger.severe("Service: Failed to release reserved stock " + stockLines + ": " + releaseEx.getMessage());
        }
    }

    /**
     * Find a specific order with all its details
     */
    @Transactional(readOnly = true)
    public OrderDTO findOrder(String id) {
        Order orderEntity = orderRepository.findByIdJoinFetch(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));

        List<OrderDetailDTO> detailDTOs = new ArrayList<>();
        for (OrderDetail d : orderEntity.getOrderDetails()) {
            detailDTOs.add(new OrderDetailDTO(d.getOrderId(), d.getItemCode(), d.getQty(), d.getUnitPrice()));
        }
        return new OrderDTO(orderEntity.getOrderId(), orderEntity.getDate(), orderEntity.getCustomerId(), detailDTOs);
    }

    /**
     * Find all orders with their details
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> findAllOrders() {
        List<OrderDTO> dtos = new ArrayList<>();
        for (Order orderEntity : orderRepository.findAllJoinFetch()) {
            List<OrderDetailDTO> detailDTOs = new ArrayList<>();
            for (OrderDetail d : orderEntity.getOrderDetails()) {
                detailDTOs.add(new OrderDetailDTO(d.getOrderId(), d.getItemCode(), d.getQty(), d.getUnitPrice()));
            }
            dtos.add(new OrderDTO(orderEntity.getOrderId(), orderEntity.getDate(), orderEntity.getCustomerId(), detailDTOs));
        }
        return dtos;
    }


    @Transactional(readOnly = true)
    public List<OrderSummaryProjection> findOrderSummaries() {
        return orderRepository.findOrderSummaries();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryDTO> findOrderSummariesDto() {
        return orderRepository.findOrderSummariesDto();
    }

    @Transactional(readOnly = true)
    public OrderDTO findOrderByDetailsGraph(String orderId) {
        Order order = orderRepository.findByIdWithDetailsGraph(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAllOrdersByDetailsGraph() {
        return toDTOList(orderRepository.findAllWithDetailsGraph());
    }

    @Transactional(readOnly = true)
    public OrderDTO findOrderByIdNative(String orderId) {
        Order order = orderRepository.findByIdNative(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        return toDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAllOrdersNativeOrderByDateDesc() {
        return toDTOList(orderRepository.findAllNativeOrderByDateDesc());
    }

    @Transactional(readOnly = true)
    public long countOrdersByCustomerIdNative(String customerId) {
        return orderRepository.countByCustomerIdNative(customerId);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findOrdersByCustomerId(String customerId) {
        return toDTOList(orderRepository.findByCustomerId(customerId));
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findOrdersByIdWithDetails(String orderId) {
        return toDTOList(orderRepository.findByIdWithDetails(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findOrdersByIdWithDetailsGraph2(String orderId) {
        return toDTOList(orderRepository.findByIdWithDetailsGraph2(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderSummary> findOrderSummariesWithConstructor() {
        return orderRepository.findOrderSummariesWithConstructor();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderTotalsDTO> findCustomerOrderTotals() {
        List<CustomerOrderTotalsDTO> totals = new ArrayList<>();
        orderRepository.findCustomerOrderTotals().forEach(row -> totals.add(new CustomerOrderTotalsDTO(
                row.getCustomerId(),
                row.getOrderCount() != null ? row.getOrderCount() : 0L,
                row.getTotalAmount() != null ? row.getTotalAmount() : 0.0
        )));
        return totals;
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAllOrdersWithCustomer() {
        return toDTOList(orderRepository.findAllWithCustomer());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findAllOrdersWithCustomerAndDetails() {
        return toDTOList(orderRepository.findAllWithCustomerAndDetails());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> findOrdersByIdWithCustomer(String orderId) {
        return toDTOList(orderRepository.findByIdWithCustomer(orderId));
    }

    /**
     * Validate the order request DTO
     */
    private boolean isValidOrderRequest(OrderDTO dto) {
        return dto != null
                && dto.getCustomerId() != null
                && !dto.getCustomerId().isEmpty()
                && dto.getOrderDetails() != null
                && !dto.getOrderDetails().isEmpty();
    }

    /**
     * Validate individual order detail
     */
    private boolean isValidOrderDetail(OrderDetailDTO detail) {
        return detail != null
                && detail.getItemCode() != null
                && !detail.getItemCode().isEmpty()
                && detail.getQty() > 0
                && detail.getUnitPrice() >= 0;
    }

    /**
     * Generate a new order ID (simple sequential format: O1, O2, O3, etc.)
     * In a real system, this would use database sequences or UUID
     */
    private String generateOrderId() {
        List<Order> allOrders = orderRepository.findAll();
        return "O" + (allOrders.size() + 1);
    }

    private List<OrderDTO> toDTOList(List<Order> orders) {
        List<OrderDTO> dtos = new ArrayList<>();
        for (Order order : orders) {
            dtos.add(toDTO(order));
        }
        return dtos;
    }

    private OrderDTO toDTO(Order orderEntity) {
        List<OrderDetailDTO> detailDTOs = new ArrayList<>();

        if (orderEntity.getOrderDetails() != null && !orderEntity.getOrderDetails().isEmpty()) {
            for (OrderDetail d : orderEntity.getOrderDetails()) {
                detailDTOs.add(new OrderDetailDTO(d.getOrderId(), d.getItemCode(), d.getQty(), d.getUnitPrice()));
            }
        } else {
            for (OrderDetail d : orderDetailRepository.findByOrderId(orderEntity.getOrderId())) {
                detailDTOs.add(new OrderDetailDTO(d.getOrderId(), d.getItemCode(), d.getQty(), d.getUnitPrice()));
            }
        }

        return new OrderDTO(orderEntity.getOrderId(), orderEntity.getDate(), orderEntity.getCustomerId(), detailDTOs);
    }
}

