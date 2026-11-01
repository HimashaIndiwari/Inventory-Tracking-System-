package com.inventory.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.inventory.entity.Category;
import com.inventory.support.DataWritingIntegrationSupport;

/** Data-writing (gate G1). */
class CategoryRepositoryIT extends DataWritingIntegrationSupport {

    @Autowired
    private CategoryRepository categories;

    @Test
    void caseVariantNameViolatesUniqueIndex() {
        categories.saveAndFlush(new Category("Electronics", null));

        assertThatThrownBy(() -> categories.saveAndFlush(new Category("electronics", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
