package com.inventory.exception;

import java.sql.SQLException;
import java.util.Map;

/**
 * Classifies database integrity errors from structured data (SQLSTATE, MySQL vendor code and Hibernate's
 * extracted constraint name), never by parsing the message text. Only SQLSTATE class 23 is classified.
 */
public final class ConstraintViolationClassifier {

    public enum Category { DUPLICATE, STILL_REFERENCED, PARENT_MISSING, OTHER }

    public record Classification(Category category, String constraintName, String sqlState, Integer vendorCode) {
    }

    private static final Map<String, String> MESSAGES = Map.of(
            "uk_products_sku", "A product with this SKU already exists.",
            "uk_categories_name", "A category with this name already exists.",
            "uk_suppliers_name", "A supplier with this name already exists.",
            "fk_products_category", "This category is used by one or more products.",
            "fk_products_supplier", "This supplier is used by one or more products.");

    private ConstraintViolationClassifier() {
    }

    public static Classification classify(Throwable error) {
        String constraintName = null;
        SQLException sql = null;
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (constraintName == null && t instanceof org.hibernate.exception.ConstraintViolationException cve) {
                constraintName = cve.getConstraintName();
            }
            if (sql == null && t instanceof SQLException s) {
                sql = s;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        if (sql == null || sql.getSQLState() == null || !sql.getSQLState().startsWith("23")) {
            return new Classification(Category.OTHER, constraintName,
                    sql == null ? null : sql.getSQLState(), sql == null ? null : sql.getErrorCode());
        }
        Category category = switch (sql.getErrorCode()) {
            case 1062 -> Category.DUPLICATE;
            case 1451, 1217 -> Category.STILL_REFERENCED;
            case 1452, 1216 -> Category.PARENT_MISSING;
            default -> Category.OTHER;
        };
        return new Classification(category, constraintName, sql.getSQLState(), sql.getErrorCode());
    }

    /** Client-safe message; never contains the constraint name. */
    public static String clientMessage(Classification classification) {
        String known = classification.constraintName() == null ? null : MESSAGES.get(classification.constraintName());
        if (known != null) {
            return known;
        }
        return switch (classification.category()) {
            case DUPLICATE -> "A resource with the same unique value already exists.";
            case STILL_REFERENCED -> "The resource is still in use and cannot be deleted.";
            case PARENT_MISSING -> "A referenced resource does not exist.";
            case OTHER -> "An unexpected error occurred.";
        };
    }
}
