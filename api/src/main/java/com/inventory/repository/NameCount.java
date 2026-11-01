package com.inventory.repository;

/** Number of products (active and archived) referencing a category or supplier id. */
public record NameCount(Long id, long count) {
}
