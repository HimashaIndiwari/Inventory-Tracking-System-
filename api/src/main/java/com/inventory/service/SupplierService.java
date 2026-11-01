package com.inventory.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.SupplierRequest;
import com.inventory.dto.SupplierResponse;
import com.inventory.entity.Supplier;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.ResourceInUseException;
import com.inventory.repository.NameCount;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.SupplierRepository;

/** Supplier rules (spec §3.5): unique name, no deletion while products (incl. archived) reference it. */
@Service
public class SupplierService {

    private final SupplierRepository suppliers;
    private final ProductRepository products;

    public SupplierService(SupplierRepository suppliers, ProductRepository products) {
        this.suppliers = suppliers;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list() {
        Map<Long, Long> counts = products.countBySupplier().stream()
                .collect(Collectors.toMap(NameCount::id, NameCount::count));
        return suppliers.findAllByOrderByNameAsc().stream()
                .map(s -> toResponse(s, counts.getOrDefault(s.getId(), 0L)))
                .toList();
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String name = request.name().trim();
        if (suppliers.existsByName(name)) {
            throw new DuplicateResourceException("A supplier with this name already exists.");
        }
        Supplier saved = suppliers.save(new Supplier(name, Text.trimToNull(request.contactPerson()),
                Text.trimToNull(request.email()), Text.trimToNull(request.phone()), Text.trimToNull(request.address())));
        return toResponse(saved, 0);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = find(id);
        String name = request.name().trim();
        if (suppliers.existsByNameAndIdNot(name, id)) {
            throw new DuplicateResourceException("A supplier with this name already exists.");
        }
        supplier.update(name, Text.trimToNull(request.contactPerson()), Text.trimToNull(request.email()),
                Text.trimToNull(request.phone()), Text.trimToNull(request.address()));
        Supplier saved = suppliers.saveAndFlush(supplier);
        return toResponse(saved, products.countBySupplier().stream()
                .filter(c -> c.id().equals(id)).mapToLong(NameCount::count).sum());
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = find(id);
        if (products.existsBySupplierId(id)) {
            throw new ResourceInUseException("This supplier is used by one or more products.");
        }
        suppliers.delete(supplier);
    }

    private Supplier find(Long id) {
        return suppliers.findById(id).orElseThrow(() -> new NotFoundException("Supplier not found."));
    }

    private static SupplierResponse toResponse(Supplier s, long productCount) {
        return new SupplierResponse(s.getId(), s.getName(), s.getContactPerson(), s.getEmail(), s.getPhone(),
                s.getAddress(), productCount, s.getCreatedAt(), s.getUpdatedAt());
    }
}
