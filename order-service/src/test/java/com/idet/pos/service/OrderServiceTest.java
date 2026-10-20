package com.idet.pos.service;

import com.idet.pos.client.CustomerClient;
import com.idet.pos.client.CustomerResponse;
import com.idet.pos.client.InventoryClient;
import com.idet.pos.client.StockLine;
import com.idet.pos.dto.OrderDTO;
import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.entity.Order;
import com.idet.pos.entity.OrderDetail;
import com.idet.pos.exception.InsufficientStockException;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.repository.OrderDetailRepository;
import com.idet.pos.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private OrderService orderService;

    private static final CustomerResponse CUSTOMER =
            new CustomerResponse("C001", "Nimal", "Colombo", "nimal@example.com");

    @Test
    void placeOrder_whenRequestInvalid_throwsInvalidRequestException() {
        OrderDTO invalid = new OrderDTO(null, LocalDate.now(), "", List.of());

        assertThrows(InvalidRequestException.class, () -> orderService.placeOrder(invalid));

        verify(customerClient, never()).getCustomer(any(String.class));
        verify(inventoryClient, never()).reserve(anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_whenCustomerMissing_throwsResourceNotFoundException() {
        OrderDTO request = new OrderDTO(
                null,
                LocalDate.of(2026, 10, 18),
                "C999",
                List.of(new OrderDetailDTO(null, "I001", 1, 100.0))
        );

        when(customerClient.getCustomer("C999")).thenThrow(new ResourceNotFoundException("Customer not found: C999"));

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(request));

        verify(customerClient).getCustomer("C999");
        verify(inventoryClient, never()).reserve(anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_whenStockInsufficient_throwsInsufficientStockException() {
        OrderDTO request = new OrderDTO(
                null,
                LocalDate.of(2026, 10, 18),
                "C001",
                List.of(new OrderDetailDTO(null, "I001", 5, 100.0))
        );

        when(customerClient.getCustomer("C001")).thenReturn(CUSTOMER);
        doThrow(new InsufficientStockException("Insufficient stock for item: I001"))
                .when(inventoryClient).reserve(List.of(new StockLine("I001", 5)));

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(request));

        verify(inventoryClient).reserve(List.of(new StockLine("I001", 5)));
        verify(inventoryClient, never()).release(anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_whenValid_reservesStockAndSavesOrderDetails() {
        OrderDTO request = new OrderDTO(
                null,
                LocalDate.of(2026, 10, 18),
                "C001",
                List.of(new OrderDetailDTO(null, "I001", 2, 100.0))
        );

        Order savedOrder = new Order("O2", LocalDate.of(2026, 10, 18), "C001");

        when(customerClient.getCustomer("C001")).thenReturn(CUSTOMER);
        runTransactionCallbacksDirectly();
        when(orderRepository.findAll()).thenReturn(List.of(new Order("O1", LocalDate.of(2026, 10, 17), "C001")));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderDetailRepository.findByOrderId("O2"))
                .thenReturn(List.of(new OrderDetail("O2", "I001", 2, 100.0)));

        OrderDTO result = orderService.placeOrder(request);

        assertEquals("O2", result.getOrderId());
        assertEquals("C001", result.getCustomerId());
        assertEquals(1, result.getOrderDetails().size());
        assertEquals("I001", result.getOrderDetails().getFirst().getItemCode());
        assertEquals(2, result.getOrderDetails().getFirst().getQty());

        verify(inventoryClient).reserve(List.of(new StockLine("I001", 2)));
        verify(orderRepository).save(any(Order.class));
        verify(orderDetailRepository).save(any(OrderDetail.class));
        verify(inventoryClient, never()).release(anyList());
    }

    @Test
    void placeOrder_whenSavingOrderFails_releasesReservedStockAndRethrows() {
        OrderDTO request = new OrderDTO(
                null,
                LocalDate.of(2026, 10, 18),
                "C001",
                List.of(new OrderDetailDTO(null, "I001", 2, 100.0))
        );
        RuntimeException saveFailure = new RuntimeException("database unavailable");

        when(customerClient.getCustomer("C001")).thenReturn(CUSTOMER);
        runTransactionCallbacksDirectly();
        when(orderRepository.findAll()).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenThrow(saveFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> orderService.placeOrder(request));

        assertSame(saveFailure, thrown);
        verify(inventoryClient).reserve(List.of(new StockLine("I001", 2)));
        verify(inventoryClient).release(List.of(new StockLine("I001", 2)));
    }

    @SuppressWarnings("unchecked")
    private void runTransactionCallbacksDirectly() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                ((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
    }
}
