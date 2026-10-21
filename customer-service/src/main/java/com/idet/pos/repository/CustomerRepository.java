package com.idet.pos.repository;

import com.idet.pos.entity.Customer;
import com.idet.pos.projection.CustomerSummary;
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
    List<Customer> findByNameContainingIgnoreCase(String namePart);

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Customer c WHERE c.name = :name")
    List<Customer> findByName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE c.email = :email")
    List<Customer> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Customer c WHERE c.address LIKE %:address%")
    List<Customer> findByAddress(@Param("address") String address);

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name%")
    List<Customer> searchByName(@Param("name") String name);

    @Query("SELECT c FROM Customer c WHERE c.email LIKE %:email%")
    List<Customer> searchByEmail(@Param("email") String email);

    @Query("SELECT c FROM Customer c WHERE c.address LIKE %:address%")
    List<Customer> searchByAddress(@Param("address") String address);

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name% OR c.email LIKE %:email% OR c.address LIKE %:address%")
    List<Customer> searchByNameOrEmailOrAddress(
            @Param("name") String name,
            @Param("email") String email,
            @Param("address") String address
    );

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name% OR c.email LIKE %:email% OR c.address LIKE %:address% OR c.id LIKE %:id%")
    List<Customer> searchByNameOrEmailOrAddressOrId(
            @Param("name") String name,
            @Param("email") String email,
            @Param("address") String address,
            @Param("id") String id
    );

    @Query(value = "SELECT * FROM customers", nativeQuery = true)
    List<Customer> findAllNative();

    @Query(value = "SELECT * FROM customers WHERE name = :name", nativeQuery = true)
    List<Customer> findByNameNative(@Param("name") String name);

    @Query("SELECT new com.idet.pos.projection.CustomerSummary(c.id, c.name, c.email) FROM Customer c")
    List<CustomerSummary> findCustomerSummaries();
}
