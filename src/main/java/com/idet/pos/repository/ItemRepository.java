package com.idet.pos.repository;

import com.idet.pos.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Item
 * Provides CRUD operations and custom queries for Item entities.
 * Hibernate handles SQL generation automatically.
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, String> {
    // JpaRepository provides: save, update, delete, findById, findAll automatically
    // The generic parameter <Item, String> indicates String is the primary key type


    // Derived query examples (no @Query annotation required).
    Page<Item> findByDescriptionContainingIgnoreCase(String keyword, Pageable pageable);

    Page<Item> findByUnitPriceGreaterThanEqual(double minPrice, Pageable pageable);

    boolean existsByDescriptionIgnoreCase(String description);

    Page<Item> findByCodeContainingIgnoreCase(String codePart, Pageable pageable);

    Page<Item> findByUnitPriceBetween(double minPrice, double maxPrice, Pageable pageable);

    List<Item> findByCode(String code);
}
