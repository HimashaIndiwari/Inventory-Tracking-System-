package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.dto.CategoryRequest;
import com.inventory.dto.CategoryResponse;
import com.inventory.entity.Category;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.ResourceInUseException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.NameCount;
import com.inventory.repository.ProductRepository;

class CategoryServiceTest {

    private CategoryRepository categories;
    private ProductRepository products;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        categories = mock(CategoryRepository.class);
        products = mock(ProductRepository.class);
        when(categories.save(any(Category.class))).thenAnswer(inv -> withId(inv.getArgument(0), 7L));
        when(categories.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new CategoryService(categories, products);
    }

    private static Category withId(Category category, long id) {
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    @Test
    void createTrimsName() {
        CategoryResponse created = service.create(new CategoryRequest("  Electronics  ", " "));

        assertThat(created.name()).isEqualTo("Electronics");
        assertThat(created.description()).isNull();
        assertThat(created.productCount()).isZero();
    }

    @Test
    void createRejectsCaseInsensitiveDuplicate() {
        when(categories.existsByName("electronics")).thenReturn(true); // collation makes it match "Electronics"

        assertThrows(DuplicateResourceException.class, () -> service.create(new CategoryRequest("electronics", null)));
        verify(categories, never()).save(any());
    }

    @Test
    void updateAllowsCaseChangeOfOwnName() {
        Category stored = withId(new Category("electronics", null), 3L);
        when(categories.findById(3L)).thenReturn(Optional.of(stored));
        when(categories.existsByNameAndIdNot("Electronics", 3L)).thenReturn(false);

        assertThat(service.update(3L, new CategoryRequest("Electronics", null)).name()).isEqualTo("Electronics");
    }

    @Test
    void updateRejectsNameOfAnotherCategory() {
        when(categories.findById(3L)).thenReturn(Optional.of(withId(new Category("Office", null), 3L)));
        when(categories.existsByNameAndIdNot("Furniture", 3L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.update(3L, new CategoryRequest("Furniture", null)));
    }

    @Test
    void updateMissingIs404() {
        when(categories.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.update(9L, new CategoryRequest("X", null)));
    }

    @Test
    void deleteBlockedWhenOnlyArchivedProductsReferToIt() {
        when(categories.findById(3L)).thenReturn(Optional.of(withId(new Category("Old", null), 3L)));
        when(products.existsByCategoryId(3L)).thenReturn(true); // counts archived products too

        assertThrows(ResourceInUseException.class, () -> service.delete(3L));
        verify(categories, never()).delete(any());
    }

    @Test
    void deleteUnusedCategory() {
        Category stored = withId(new Category("Unused", null), 3L);
        when(categories.findById(3L)).thenReturn(Optional.of(stored));
        when(products.existsByCategoryId(3L)).thenReturn(false);

        service.delete(3L);

        verify(categories).delete(stored);
    }

    @Test
    void listIncludesProductCount() {
        when(categories.findAllByOrderByNameAsc())
                .thenReturn(List.of(withId(new Category("A", null), 1L), withId(new Category("B", null), 2L)));
        when(products.countByCategory()).thenReturn(List.of(new NameCount(1L, 3)));

        List<CategoryResponse> list = service.list();

        assertThat(list).extracting(CategoryResponse::productCount).containsExactly(3L, 0L);
    }
}
