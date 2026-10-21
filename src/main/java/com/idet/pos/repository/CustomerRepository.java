package com.idet.pos.repository;

import com.idet.pos.dto.CustomerPurchaseStatsDTO;
import com.idet.pos.entity.Customer;
import com.idet.pos.projection.CustomerBasicProjection;
import com.idet.pos.projection.CustomerOrderCountProjection;
import com.idet.pos.projection.CustomerPurchaseStatsProjection;
import com.idet.pos.projection.CustomerSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Customer
 * Provides CRUD operations and custom queries for Customer entities.
 * Hibernate handles SQL generation automatically.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    // JpaRepository provides: save, update, delete, findById, findAll automatically
    // Additional custom queries can be added here using @Query annotations if needed

    // Derived query examples (no @Query annotation required).
    Page<Customer> findByNameContainingIgnoreCase(String namePart, Pageable pageable);

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Customer c WHERE c.name = :name")
    List<Customer> findByName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE c.email = :email")
    List<Customer> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Customer c WHERE c.address LIKE %:address%")
    Page<Customer> findByAddress(@Param("address") String address, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name%")
    Page<Customer> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.email LIKE %:email%")
    Page<Customer> searchByEmail(@Param("email") String email, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.address LIKE %:address%")
    Page<Customer> searchByAddress(@Param("address") String address, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name% OR c.email LIKE %:email% OR c.address LIKE %:address%")
    Page<Customer> searchByNameOrEmailOrAddress(
            @Param("name") String name,
            @Param("email") String email,
            @Param("address") String address,
            Pageable pageable
    );

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name% OR c.email LIKE %:email% OR c.address LIKE %:address% OR c.id LIKE %:id%")
    Page<Customer> searchByNameOrEmailOrAddressOrId(
            @Param("name") String name,
            @Param("email") String email,
            @Param("address") String address,
            @Param("id") String id,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM customers ORDER BY id",
            countQuery = "SELECT COUNT(*) FROM customers",
            nativeQuery = true)
    Page<Customer> findAllNative(Pageable pageable);

    @Query(value = "SELECT * FROM customers WHERE name = :name", nativeQuery = true)
    List<Customer> findByNameNative(@Param("name") String name);

    @Query(value = "SELECT c.id AS id, c.name AS name, c.email AS email, c.address AS address " +
            "FROM Customer c JOIN Order o ON o.customerId = c.id " +
            "GROUP BY c.id, c.name, c.email, c.address " +
            "ORDER BY c.id",
            countQuery = "SELECT COUNT(DISTINCT c.id) FROM Customer c JOIN Order o ON o.customerId = c.id")
    Page<CustomerBasicProjection> findCustomersWithOrders(Pageable pageable);

    @Query(value = "SELECT c.id AS id, c.name AS name, COUNT(o.orderId) AS orderCount " +
            "FROM Customer c LEFT JOIN Order o ON o.customerId = c.id " +
            "GROUP BY c.id, c.name " +
            "ORDER BY c.id",
            countQuery = "SELECT COUNT(c) FROM Customer c")
    Page<CustomerOrderCountProjection> findCustomersWithOrderCount(Pageable pageable);

    @Query(value = "SELECT new com.idet.pos.projection.CustomerSummary(c.id, c.name, c.email) FROM Customer c ORDER BY c.id",
            countQuery = "SELECT COUNT(c) FROM Customer c")
    Page<CustomerSummary> findCustomerSummaries(Pageable pageable);

    @Query(value = "SELECT c.id AS customerId, " +
            "c.name AS customerName, " +
            "c.email AS email, " +
            "COUNT(DISTINCT o.order_id) AS totalOrders, " +
            "COALESCE(SUM(od.qty * od.unit_price), 0) AS totalSpent " +
            "FROM customers c " +
            "LEFT JOIN orders o ON o.customer_id = c.id " +
            "LEFT JOIN order_details od ON od.order_id = o.order_id " +
            "GROUP BY c.id, c.name, c.email " +
            "ORDER BY c.name, c.id",
            countQuery = "SELECT COUNT(*) FROM customers",
            nativeQuery = true)
    Page<CustomerPurchaseStatsProjection> findCustomerPurchaseStats(Pageable pageable);

    default Page<CustomerPurchaseStatsDTO> findCustomerPurchaseStatsDto(Pageable pageable) {
        return findCustomerPurchaseStats(pageable)
                .map(row -> new CustomerPurchaseStatsDTO(
                        row.getCustomerId(),
                        row.getCustomerName(),
                        row.getEmail(),
                        row.getTotalOrders() != null ? row.getTotalOrders() : 0L,
                        row.getTotalSpent() != null ? row.getTotalSpent() : 0.0
                ));
    }
}
