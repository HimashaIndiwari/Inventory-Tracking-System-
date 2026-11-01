package com.inventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventory.entity.Category;

/** Name comparisons are case- and accent-insensitive through the column collation (utf8mb4_0900_ai_ci). */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    List<Category> findAllByOrderByNameAsc();
}
