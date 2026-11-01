package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.inventory.exception.ResourceInUseException;
import com.inventory.support.DataWritingIntegrationSupport;
import com.inventory.support.Fixtures;

/** Data-writing (gate G4). */
class CatalogDeletionIT extends DataWritingIntegrationSupport {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SupplierService supplierService;

    @Test
    void categoryReferencedOnlyByArchivedProductCannotBeDeleted() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Old stock");
        f.product("OLD-1", "Archived", cat, null, 0, 0, false);

        assertThrows(ResourceInUseException.class, () -> categoryService.delete(cat));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM categories WHERE id = ?", Long.class, cat)).isEqualTo(1);
    }

    @Test
    void supplierReferencedOnlyByArchivedProductCannotBeDeleted() {
        Fixtures f = new Fixtures(jdbc);
        long cat = f.category("Any");
        long sup = f.supplier("Old supplier");
        f.product("OLD-2", "Archived", cat, sup, 0, 0, false);

        assertThrows(ResourceInUseException.class, () -> supplierService.delete(sup));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM suppliers WHERE id = ?", Long.class, sup)).isEqualTo(1);
    }
}
