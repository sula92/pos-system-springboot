package com.idet.pos.repository;

import com.idet.pos.dto.OrderSummaryDTO;
import com.idet.pos.entity.Order;
import com.idet.pos.projection.OrderSummary;
import com.idet.pos.projection.OrderSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Order
 * Provides CRUD operations and custom queries for Order entities.
 * Hibernate handles SQL generation automatically.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    // JpaRepository provides: save, update, delete, findById, findAll automatically
    // The generic parameter <Order, String> indicates String is the primary key type

    // Uses a named entity graph to fetch orderDetails with the order in one query.
    @EntityGraph(value = "Order.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithDetailsGraph(@Param("orderId") String orderId);

    // Paging step 1: page over order ids only, so the database applies LIMIT/OFFSET and the count is exact.
    // Collection fetches (JOIN FETCH / entity graphs) are never combined with a Pageable.
    @Query(value = "SELECT o.orderId FROM Order o",
            countQuery = "SELECT COUNT(o) FROM Order o")
    Page<String> findOrderIds(Pageable pageable);

    @Query(value = "SELECT o.orderId FROM Order o WHERE o.customerId = :customerId",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.customerId = :customerId")
    Page<String> findOrderIdsByCustomerId(@Param("customerId") String customerId, Pageable pageable);

    // Native variant with a fixed newest-first order; callers pass an unsorted Pageable.
    @Query(value = "SELECT order_id FROM orders ORDER BY date DESC, order_id",
            countQuery = "SELECT COUNT(*) FROM orders",
            nativeQuery = true)
    Page<String> findOrderIdsNativeOrderByDateDesc(Pageable pageable);

    // Paging step 2: load one page of orders with their details in a single query.
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails WHERE o.orderId IN :orderIds")
    List<Order> findAllWithDetailsByIdIn(@Param("orderIds") Collection<String> orderIds);

    // Attribute-path graph variant of paging step 2.
    @EntityGraph(attributePaths = {"orderDetails"})
    @Query("SELECT o FROM Order o WHERE o.orderId IN :orderIds")
    List<Order> findAllWithDetailsGraphByIdIn(@Param("orderIds") Collection<String> orderIds);

    //when you want to fetch order details with the order in one query.
    // JOIN FETCH removes N+1 selects by loading orders and their details in one query.
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails WHERE o.orderId = :orderId")
    Optional<Order> findByIdJoinFetch(@Param("orderId") String orderId);

    // Native SQL by primary key; useful when you need vendor-specific SQL debugging.
    @Query(value = "SELECT * FROM orders WHERE order_id = :orderId", nativeQuery = true)
    Optional<Order> findByIdNative(@Param("orderId") String orderId);

    // Native aggregate query to quickly count orders for a customer.
    @Query(value = "SELECT COUNT(*) FROM orders WHERE customer_id = :customerId", nativeQuery = true)
    long countByCustomerIdNative(@Param("customerId") String customerId);

    /*@EntityGraph(value = "Order.withDetails", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT o FROM Order WHERE o.id = :orderId")
    List<Order> findByIdWithDetailsGraph(@Param("orderId") String id);*/

    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    List<Order> findByIdWithDetails(@Param("orderId") String orderId);


    @EntityGraph(attributePaths = {"orderDetails"})
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    List<Order> findByIdWithDetailsGraph2(@Param("orderId") String orderId);

    // Custom projection using constructor expression
    @Query(value = "SELECT new com.idet.pos.projection.OrderSummary(o.orderId, o.date, COALESCE(c.name, o.customerId)) " +
            "FROM Order o LEFT JOIN Customer c ON c.id = o.customerId " +
            "ORDER BY o.date DESC, o.orderId",
            countQuery = "SELECT COUNT(o) FROM Order o")
    Page<OrderSummary> findOrderSummariesWithConstructor(Pageable pageable);

    // JOIN FETCH with WHERE clause - Eagerly loads customer for a specific order
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    List<Order> findByIdWithCustomer(@Param("orderId") String orderId);

    // Projection query that returns a report-style shape instead of Order entities.
    @Query(value = "SELECT o.orderId AS orderId, " +
            "o.date AS orderDate, " +
            "o.customerId AS customerId, " +
            "COUNT(od) AS lineCount, " +
            "COALESCE(SUM(od.qty), 0) AS totalQty, " +
            "COALESCE(SUM(od.qty * od.unitPrice), 0.0) AS grandTotal " +
            "FROM Order o LEFT JOIN o.orderDetails od " +
            "GROUP BY o.orderId, o.date, o.customerId " +
            "ORDER BY o.date DESC, o.orderId",
            countQuery = "SELECT COUNT(o) FROM Order o")
    Page<OrderSummaryProjection> findOrderSummaries(Pageable pageable);

    // Constructor DTO projection alternative to interface-based projection.
    @Query(value = "SELECT new com.idet.pos.dto.OrderSummaryDTO(" +
            "o.orderId, " +
            "o.date, " +
            "o.customerId, " +
            "CAST(COUNT(od) AS long), " +
            "CAST(COALESCE(SUM(od.qty), 0) AS long), " +
            "COALESCE(SUM(od.qty * od.unitPrice), 0.0)) " +
            "FROM Order o LEFT JOIN o.orderDetails od " +
            "GROUP BY o.orderId, o.date, o.customerId " +
            "ORDER BY o.date DESC, o.orderId",
            countQuery = "SELECT COUNT(o) FROM Order o")
    Page<OrderSummaryDTO> findOrderSummariesDto(Pageable pageable);
}
