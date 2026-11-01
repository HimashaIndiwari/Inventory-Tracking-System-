package com.inventory.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventory.dto.CategoryStock;
import com.inventory.entity.Product;

import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Active products only; pattern is null or a LikePatterns value escaped with '!'. */
    @Query("""
            select p from Product p
            where p.active = true
              and (:categoryId is null or p.category.id = :categoryId)
              and (:lowStockOnly = false or p.quantity <= p.reorderLevel)
              and (:pattern is null or p.name like :pattern escape '!' or p.sku like :pattern escape '!')
            """)
    Page<Product> search(@Param("pattern") String pattern, @Param("categoryId") Long categoryId,
            @Param("lowStockOnly") boolean lowStockOnly, Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.active = true")
    Optional<Product> findActiveByIdForUpdate(@Param("id") Long id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /** Includes archived products, which still block deletion. */
    boolean existsByCategoryId(Long categoryId);

    /** Includes archived products, which still block deletion. */
    boolean existsBySupplierId(Long supplierId);

    @Query("select new com.inventory.repository.NameCount(p.category.id, count(p)) from Product p group by p.category.id")
    List<NameCount> countByCategory();

    @Query("""
            select new com.inventory.repository.NameCount(p.supplier.id, count(p))
            from Product p where p.supplier is not null group by p.supplier.id
            """)
    List<NameCount> countBySupplier();

    long countByActiveTrue();

    @Query("select count(p) from Product p where p.active = true and p.quantity <= p.reorderLevel")
    long countActiveLowStock();

    @Query("""
            select p from Product p where p.active = true and p.quantity <= p.reorderLevel
            order by p.quantity asc, p.name asc, p.id asc
            """)
    List<Product> findActiveLowStock(Pageable pageable);

    default List<Product> findTop5ActiveLowStock() {
        return findActiveLowStock(PageRequest.of(0, 5));
    }

    /**
     * Native query with explicit DECIMAL(65,0): a plain CAST(... AS DECIMAL) on MySQL means DECIMAL(10,0)
     * and would clip large totals. COALESCE gives 0 for an empty inventory.
     */
    @Query(nativeQuery = true, value =
            "SELECT CAST(COALESCE(SUM(quantity), 0) AS DECIMAL(65,0)) FROM products WHERE active = TRUE")
    java.math.BigDecimal sumActiveQuantityRaw();

    default BigInteger sumActiveQuantity() {
        return sumActiveQuantityRaw().toBigIntegerExact();
    }

    @Query(nativeQuery = true, value = """
            SELECT c.name AS categoryName, CAST(COALESCE(SUM(p.quantity), 0) AS DECIMAL(65,0)) AS totalQuantity
            FROM categories c
            LEFT JOIN products p ON p.category_id = c.id AND p.active = TRUE
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<CategoryStockRow> stockByCategoryRaw();

    default List<CategoryStock> stockByCategory() {
        return stockByCategoryRaw().stream()
                .map(row -> new CategoryStock(row.getCategoryName(), row.getTotalQuantity().toBigIntegerExact()))
                .toList();
    }
}
