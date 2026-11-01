package com.inventory.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;

/** Read and append only: no update or delete operations are used for stock transactions. */
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    /** Includes archived products. from is inclusive, to is exclusive. */
    @EntityGraph(attributePaths = {"product", "user"})
    @Query("""
            select t from StockTransaction t
            where (:productId is null or t.product.id = :productId)
              and (:type is null or t.type = :type)
              and (:pattern is null or t.product.name like :pattern escape '!'
                   or t.product.sku like :pattern escape '!')
              and (:from is null or t.createdAt >= :from)
              and (:to is null or t.createdAt < :to)
            """)
    Page<StockTransaction> history(@Param("productId") Long productId, @Param("type") TransactionType type,
            @Param("pattern") String pattern, @Param("from") Instant from, @Param("to") Instant to,
            Pageable pageable);

    @EntityGraph(attributePaths = {"product", "user"})
    List<StockTransaction> findTop10ByOrderByCreatedAtDescIdDesc();
}
