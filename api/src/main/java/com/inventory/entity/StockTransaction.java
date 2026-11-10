package com.inventory.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Append-only audit record: no setters, no updated_at. */
@Entity
@Table(name = "stock_transactions")
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 6, updatable = false)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "quantity_after", nullable = false, updatable = false)
    private long quantityAfter;

    @Column(length = 255, updatable = false)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockTransaction() {
    }

    public StockTransaction(Product product, AppUser user, TransactionType type, int quantity, long quantityAfter,
            String note) {
        this.product = product;
        this.user = user;
        this.type = type;
        this.quantity = quantity;
        this.quantityAfter = quantityAfter;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public AppUser getUser() {
        return user;
    }

    public TransactionType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getQuantityAfter() {
        return quantityAfter;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
