package com.idet.pos.service;

import com.idet.pos.dto.OrderDTO;
import com.idet.pos.dto.OrderDetailDTO;
import com.idet.pos.entity.Customer;
import com.idet.pos.entity.Inventory;
import com.idet.pos.entity.Item;
import com.idet.pos.entity.Order;
import com.idet.pos.entity.OrderDetail;
import com.idet.pos.exception.InsufficientStockException;
import com.idet.pos.exception.InvalidRequestException;
import com.idet.pos.exception.ResourceNotFoundException;
import com.idet.pos.repository.CustomerRepository;
import com.idet.pos.repository.InventoryRepository;
import com.idet.pos.repository.ItemRepository;
import com.idet.pos.repository.OrderDetailRepository;
import com.idet.pos.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private ItemRepository itemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_whenRequestInvalid_throwsInvalidRequestException() {
        OrderDTO invalid = new OrderDTO(null, LocalDate.now(), "", List.of());

        assertThrows(InvalidRequestException.class, () -> orderService.placeOrder(invalid));

        verify(customerRepository, never()).findById(any(String.class));
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

        when(customerRepository.findById("C999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(request));

        verify(customerRepository).findById("C999");
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

        when(customerRepository.findById("C001"))
                .thenReturn(Optional.of(new Customer("C001", "Nimal", "Colombo", "nimal@example.com")));
        when(itemRepository.findById("I001"))
                .thenReturn(Optional.of(new Item("I001", "Pen", 100.0)));
        when(inventoryRepository.findByItemCodeForUpdate("I001"))
                .thenReturn(Optional.of(new Inventory("I001", 3, new Item("I001", "Pen", 100.0))));

        assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(request));

        verify(inventoryRepository).findByItemCodeForUpdate("I001");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_whenValid_savesOrderDetailsAndUpdatesStock() {
        OrderDTO request = new OrderDTO(
                null,
                LocalDate.of(2026, 10, 18),
                "C001",
                List.of(new OrderDetailDTO(null, "I001", 2, 100.0))
        );

        Customer customer = new Customer("C001", "Nimal", "Colombo", "nimal@example.com");
        Item item = new Item("I001", "Pen", 100.0);
        Inventory inventory = new Inventory("I001", 10, item);
        Order savedOrder = new Order("O2", LocalDate.of(2026, 10, 18), "C001");

        when(customerRepository.findById("C001")).thenReturn(Optional.of(customer));
        when(itemRepository.findById("I001")).thenReturn(Optional.of(item));
        when(inventoryRepository.findByItemCodeForUpdate("I001")).thenReturn(Optional.of(inventory));
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

        verify(orderRepository).save(any(Order.class));
        verify(orderDetailRepository).save(any(OrderDetail.class));

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertEquals(8, inventoryCaptor.getValue().getQty());
    }
}

