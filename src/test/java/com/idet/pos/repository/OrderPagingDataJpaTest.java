package com.idet.pos.repository;

import com.idet.pos.dto.OrderDTO;
import com.idet.pos.dto.OrderSummaryDTO;
import com.idet.pos.entity.Customer;
import com.idet.pos.entity.Order;
import com.idet.pos.entity.OrderDetail;
import com.idet.pos.projection.CustomerBasicProjection;
import com.idet.pos.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Runs the paginated order and report queries against a real (H2) database with
 * hibernate.query.fail_on_pagination_over_collection_fetch enabled, so any in-memory
 * collection pagination would fail these tests.
 */
@DataJpaTest
@Import(OrderService.class)
class OrderPagingDataJpaTest {

    private static final Pageable NEWEST_FIRST_TWO =
            PageRequest.of(0, 2, Sort.by(Sort.Order.desc("date"), Sort.Order.asc("orderId")));

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        entityManager.persist(new Customer("C001", "Nimal", "Colombo", "nimal@example.com"));
        entityManager.persist(new Customer("C002", "Kamal", "Galle", "kamal@example.com"));
        entityManager.persist(new Customer("C003", "Sunil", "Kandy", "sunil@example.com"));

        persistOrder("O001", LocalDate.of(2026, 10, 1), "C001");
        persistOrder("O002", LocalDate.of(2026, 10, 2), "C001");
        persistOrder("O003", LocalDate.of(2026, 10, 3), "C002");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findAllOrders_returnsOnePageWithAllDetailsAndExactTotal() {
        Page<OrderDTO> page = orderService.findAllOrders(NEWEST_FIRST_TWO);

        assertEquals(List.of("O003", "O002"), page.getContent().stream().map(OrderDTO::getOrderId).toList());
        assertEquals(2, page.getContent().get(0).getOrderDetails().size());
        assertEquals(2, page.getContent().get(1).getOrderDetails().size());
        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void findAllOrdersByDetailsGraph_secondPageHoldsRemainingOrder() {
        Page<OrderDTO> page = orderService.findAllOrdersByDetailsGraph(NEWEST_FIRST_TWO.next());

        assertEquals(List.of("O001"), page.getContent().stream().map(OrderDTO::getOrderId).toList());
        assertEquals(2, page.getContent().get(0).getOrderDetails().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void findAllOrdersNative_usesFixedNewestFirstOrder() {
        Page<OrderDTO> page = orderService.findAllOrdersNativeOrderByDateDesc(PageRequest.of(0, 2, Sort.by("orderId")));

        assertEquals(List.of("O003", "O002"), page.getContent().stream().map(OrderDTO::getOrderId).toList());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void findOrdersByCustomerId_countsOnlyThatCustomersOrders() {
        Page<OrderDTO> page = orderService.findOrdersByCustomerId("C001", PageRequest.of(0, 1, Sort.by("orderId")));

        assertEquals(List.of("O001"), page.getContent().stream().map(OrderDTO::getOrderId).toList());
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findOrderSummariesDto_groupByReportHasOneRowPerOrderInTotal() {
        Page<OrderSummaryDTO> page = orderService.findOrderSummariesDto(PageRequest.of(0, 2));

        assertEquals(2, page.getContent().size());
        assertEquals("O003", page.getContent().get(0).getOrderId());
        assertEquals(2L, page.getContent().get(0).getLineCount());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    void findCustomersWithOrders_countsDistinctCustomers() {
        Page<CustomerBasicProjection> page = customerRepository.findCustomersWithOrders(PageRequest.of(0, 1));

        assertEquals(1, page.getContent().size());
        assertEquals("C001", page.getContent().get(0).getId());
        assertEquals(2, page.getTotalElements());
    }

    private void persistOrder(String orderId, LocalDate date, String customerId) {
        Order order = entityManager.persist(new Order(orderId, date, customerId));
        for (String itemCode : List.of("I001", "I002")) {
            OrderDetail detail = new OrderDetail(orderId, itemCode, 2, 100.0);
            detail.setOrder(order);
            entityManager.persist(detail);
        }
    }
}
