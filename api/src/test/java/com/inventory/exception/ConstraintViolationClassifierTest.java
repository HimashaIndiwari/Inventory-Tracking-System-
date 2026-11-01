package com.inventory.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.inventory.exception.ConstraintViolationClassifier.Category;
import com.inventory.exception.ConstraintViolationClassifier.Classification;

class ConstraintViolationClassifierTest {

    static DataIntegrityViolationException chain(String sqlState, int vendor, String constraint) {
        SQLException sql = new SQLIntegrityConstraintViolationException("raw message", sqlState, vendor);
        return new DataIntegrityViolationException("x",
                new org.hibernate.exception.ConstraintViolationException("x", sql, constraint));
    }

    @Test
    void duplicateFromVendorCode() {
        Classification c = ConstraintViolationClassifier.classify(chain("23000", 1062, "uk_products_sku"));
        assertThat(c.category()).isEqualTo(Category.DUPLICATE);
        assertThat(c.constraintName()).isEqualTo("uk_products_sku");
    }

    @Test
    void stillReferenced1217And1451() {
        assertThat(ConstraintViolationClassifier.classify(chain("23000", 1217, null)).category())
                .isEqualTo(Category.STILL_REFERENCED);
        assertThat(ConstraintViolationClassifier.classify(chain("23000", 1451, "fk_products_category")).category())
                .isEqualTo(Category.STILL_REFERENCED);
    }

    @Test
    void parentMissing1216And1452() {
        assertThat(ConstraintViolationClassifier.classify(chain("23000", 1216, null)).category())
                .isEqualTo(Category.PARENT_MISSING);
        assertThat(ConstraintViolationClassifier.classify(chain("23000", 1452, "fk_products_category")).category())
                .isEqualTo(Category.PARENT_MISSING);
    }

    @Test
    void nullConstraintNameStillClassified() {
        assertThat(ConstraintViolationClassifier.classify(chain("23000", 1062, null)).category())
                .isEqualTo(Category.DUPLICATE);
    }

    @Test
    void notNullViolationIsOther() {
        assertThat(ConstraintViolationClassifier.classify(chain("23000", 1048, null)).category())
                .isEqualTo(Category.OTHER);
    }

    @Test
    void checkViolationIsOther() {
        assertThat(ConstraintViolationClassifier.classify(chain("HY000", 3819, "chk_products_price_non_negative"))
                .category()).isEqualTo(Category.OTHER);
    }

    @Test
    void exceptionWithoutSqlExceptionIsOther() {
        assertThat(ConstraintViolationClassifier.classify(new DataIntegrityViolationException("no cause")).category())
                .isEqualTo(Category.OTHER);
    }

    @Test
    void clientMessageNeverContainsConstraintName() {
        for (String name : new String[] {"uk_products_sku", "uk_categories_name", "fk_products_category", "fk_x"}) {
            for (int vendor : new int[] {1062, 1217, 1216}) {
                String message = ConstraintViolationClassifier.clientMessage(
                        ConstraintViolationClassifier.classify(chain("23000", vendor, name)));
                assertThat(message).doesNotContain(name).doesNotContain("uk_").doesNotContain("fk_");
            }
        }
    }

    @Test
    void knownConstraintsGetSpecificMessages() {
        assertThat(ConstraintViolationClassifier.clientMessage(
                ConstraintViolationClassifier.classify(chain("23000", 1062, "uk_products_sku"))))
                .isEqualTo("A product with this SKU already exists.");
        assertThat(ConstraintViolationClassifier.clientMessage(
                ConstraintViolationClassifier.classify(chain("23000", 1451, "fk_products_supplier"))))
                .isEqualTo("This supplier is used by one or more products.");
    }
}
