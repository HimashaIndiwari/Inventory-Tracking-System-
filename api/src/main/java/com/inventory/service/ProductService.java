package com.inventory.service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.IdName;
import com.inventory.dto.PageResponse;
import com.inventory.dto.ProductRequest;
import com.inventory.dto.ProductResponse;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Supplier;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.ProductHasStockException;
import com.inventory.exception.RequestValidationException;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;
import com.inventory.util.LikePatterns;

/** Product rules (spec FR-PROD): normalised unique SKU, quantity starts at 0, archive only at zero stock. */
@Service
public class ProductService {

    private static final Pattern STORED_SKU = Pattern.compile("^[A-Z0-9-]{3,50}$");
    private static final String DUPLICATE_SKU = "A product with this SKU already exists.";

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final SupplierRepository suppliers;

    public ProductService(ProductRepository products, CategoryRepository categories, SupplierRepository suppliers) {
        this.products = products;
        this.categories = categories;
        this.suppliers = suppliers;
    }

    /** strip() removes every whitespace the request pattern accepts as padding, so the result is the stored form. */
    public static String normaliseSku(String raw) {
        String sku = raw.strip().toUpperCase(Locale.ROOT);
        if (!STORED_SKU.matcher(sku).matches()) {
            throw new RequestValidationException("sku", "must be 3-50 characters: letters, digits and hyphens");
        }
        return sku;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, Long categoryId, boolean lowStock, int page,
            int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name", "id"));
        return PageResponse.of(products.search(LikePatterns.contains(search), categoryId, lowStock, pageable),
                ProductService::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = normaliseSku(request.sku());
        if (products.existsBySku(sku)) {
            throw new DuplicateResourceException(DUPLICATE_SKU);
        }
        Product product = new Product(sku, request.name().trim(), Text.trimToNull(request.description()),
                category(request.categoryId()), supplier(request.supplierId()), request.price(),
                request.reorderLevel());
        return toResponse(products.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        String sku = normaliseSku(request.sku());
        if (products.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateResourceException(DUPLICATE_SKU);
        }
        product.updateDetails(sku, request.name().trim(), Text.trimToNull(request.description()),
                category(request.categoryId()), supplier(request.supplierId()), request.price(),
                request.reorderLevel());
        return toResponse(products.saveAndFlush(product));
    }

    @Transactional
    public void archive(Long id) {
        Product product = find(id);
        if (product.getQuantity() > 0) {
            throw new ProductHasStockException(product.getQuantity());
        }
        product.archive();
        products.saveAndFlush(product);
    }

    private Product find(Long id) {
        return products.findByIdAndActiveTrue(id).orElseThrow(() -> new NotFoundException("Product not found."));
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found."));
    }

    private Supplier supplier(Long id) {
        if (id == null) {
            return null;
        }
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found."));
    }

    private static ProductResponse toResponse(Product p) {
        Supplier s = p.getSupplier();
        return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                new IdName(p.getCategory().getId(), p.getCategory().getName()),
                s == null ? null : new IdName(s.getId(), s.getName()), p.getPrice(), p.getQuantity(),
                p.getReorderLevel(), p.isLowStock(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
