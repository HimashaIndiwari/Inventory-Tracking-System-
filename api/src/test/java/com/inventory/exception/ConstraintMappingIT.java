package com.inventory.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.Fixtures;

/** Data-writing (gate G2). Records what Hibernate 7.4 + Connector/J 9.7 actually report on MySQL (risk R9). */
class ConstraintMappingIT extends DataWritingIntegrationSupport {

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private ProductRepository products;

    @Test
    void realDuplicateCategoryIsDuplicate() {
        categories.saveAndFlush(new Category("Electronics", null));

        Throwable error = catchThrowable(() -> categories.saveAndFlush(new Category("ELECTRONICS", null)));

        assertThat(ConstraintViolationClassifier.classify(error).category())
                .isEqualTo(ConstraintViolationClassifier.Category.DUPLICATE);
    }

    @Test
    void realDeleteOfReferencedCategoryIsStillReferenced() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Furniture");
        f.product("FUR-1", "Chair", cat, null, 0, 0, false);

        Throwable error = catchThrowable(() -> {
            categories.deleteById(cat);
            categories.flush();
        });

        assertThat(ConstraintViolationClassifier.classify(error).category())
                .isEqualTo(ConstraintViolationClassifier.Category.STILL_REFERENCED);
    }

    @Test
    void realInsertWithMissingCategoryIsParentMissing() {
        Category missing = categories.getReferenceById(999_999_999L);

        Throwable error = catchThrowable(() -> products.saveAndFlush(
                new Product("ORPHAN-1", "Orphan", null, missing, null, BigDecimal.ONE, 0)));

        assertThat(ConstraintViolationClassifier.classify(error).category())
                .isEqualTo(ConstraintViolationClassifier.Category.PARENT_MISSING);
    }
}
