package com.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.CategoryRequest;
import com.inventory.dto.CategoryResponse;
import com.inventory.entity.Category;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.ResourceInUseException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.NameCount;
import com.inventory.repository.ProductRepository;

/** Category rules (spec §3.4): unique name (case/accent-insensitive), no deletion while products reference it. */
@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ProductRepository products;

    public CategoryService(CategoryRepository categories, ProductRepository products) {
        this.categories = categories;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        Map<Long, Long> counts = products.countByCategory().stream()
                .collect(Collectors.toMap(NameCount::id, NameCount::count));
        return categories.findAllByOrderByNameAsc().stream()
                .map(c -> toResponse(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categories.existsByName(name)) {
            throw new DuplicateResourceException("A category with this name already exists.");
        }
        Category saved = categories.save(new Category(name, Text.trimToNull(request.description())));
        return toResponse(saved, 0);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        String name = request.name().trim();
        if (categories.existsByNameAndIdNot(name, id)) {
            throw new DuplicateResourceException("A category with this name already exists.");
        }
        category.setName(name);
        category.setDescription(Text.trimToNull(request.description()));
        Category saved = categories.saveAndFlush(category);
        return toResponse(saved, products.countByCategory().stream()
                .filter(c -> c.id().equals(id)).mapToLong(NameCount::count).sum());
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);
        if (products.existsByCategoryId(id)) {
            throw new ResourceInUseException("This category is used by one or more products.");
        }
        categories.delete(category);
    }

    private Category find(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found."));
    }

    private static CategoryResponse toResponse(Category c, long productCount) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), productCount, c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
