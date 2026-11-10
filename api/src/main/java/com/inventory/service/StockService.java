package com.inventory.service;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.dto.PageResponse;
import com.inventory.dto.StockAdjustmentRequest;
import com.inventory.dto.StockMovementRequest;
import com.inventory.dto.StockTransactionResponse;
import com.inventory.dto.StockTransactionResponse.ProductRef;
import com.inventory.dto.StockTransactionResponse.UserRef;
import com.inventory.entity.AppUser;
import com.inventory.entity.Product;
import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.RequestValidationException;
import com.inventory.repository.AppUserRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;
import com.inventory.security.AuthenticatedUser;
import com.inventory.util.LikePatterns;

/**
 * Stock movements (spec §3.6): the product row is locked (SELECT ... FOR UPDATE) so concurrent movements on the
 * same product serialise; the quantity change and its transaction row commit or roll back together.
 */
@Service
public class StockService {

    private final ProductRepository products;
    private final StockTransactionRepository transactions;
    private final AppUserRepository users;

    public StockService(ProductRepository products, StockTransactionRepository transactions,
            AppUserRepository users) {
        this.products = products;
        this.transactions = transactions;
        this.users = users;
    }

    @Transactional
    public StockTransactionResponse stockIn(StockMovementRequest request, AuthenticatedUser actor) {
        Product product = lock(request.productId());
        product.changeQuantity(Math.addExact(product.getQuantity(), request.quantity()));
        return record(product, actor, TransactionType.IN, request.quantity(), request.note());
    }

    @Transactional
    public StockTransactionResponse stockOut(StockMovementRequest request, AuthenticatedUser actor) {
        Product product = lock(request.productId());
        if (request.quantity() > product.getQuantity()) {
            throw new InsufficientStockException(request.quantity(), product.getQuantity());
        }
        product.changeQuantity(product.getQuantity() - request.quantity());
        return record(product, actor, TransactionType.OUT, request.quantity(), request.note());
    }

    /**
     * ADJUST (ADMIN only, enforced in SecurityConfig): the delta is signed, so a positive value adds stock and a
     * negative value removes it. The lock and the non-negative check happen before anything is written, so a
     * rejected adjustment changes neither the quantity nor the ledger.
     */
    @Transactional
    public StockTransactionResponse adjust(StockAdjustmentRequest request, AuthenticatedUser actor) {
        if (request.delta() == 0) {
            throw new RequestValidationException("delta", "must not be zero");
        }
        Product product = lock(request.productId());
        long newQuantity = Math.addExact(product.getQuantity(), request.delta());
        if (newQuantity < 0) {
            throw new InsufficientStockException("Insufficient stock for adjustment: requested decrease "
                    + -request.delta() + ", available " + product.getQuantity());
        }
        product.changeQuantity(newQuantity);
        return record(product, actor, TransactionType.ADJUST, request.delta(), request.note());
    }

    /** from is inclusive, to is exclusive; both optional. */
    @Transactional(readOnly = true)
    public PageResponse<StockTransactionResponse> history(Long productId, TransactionType type, String search,
            Instant from, Instant to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new RequestValidationException("from", "must not be after 'to'");
        }
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(
                transactions.history(productId, type, LikePatterns.contains(search), from, to, pageable),
                StockService::toResponse);
    }

    private Product lock(Long productId) {
        return products.findActiveByIdForUpdate(productId)
                .orElseThrow(() -> new NotFoundException("Product not found."));
    }

    private StockTransactionResponse record(Product product, AuthenticatedUser actor, TransactionType type,
            int quantity, String note) {
        AppUser user = users.getReferenceById(actor.userId());
        StockTransaction saved = transactions.saveAndFlush(new StockTransaction(product, user, type,
                quantity, product.getQuantity(), Text.trimToNull(note)));
        return toResponse(saved);
    }

    public static StockTransactionResponse toResponse(StockTransaction t) {
        Product p = t.getProduct();
        AppUser u = t.getUser();
        return new StockTransactionResponse(t.getId(), t.getType(), t.getQuantity(), t.getQuantityAfter(),
                t.getNote(), new ProductRef(p.getId(), p.getSku(), p.getName()),
                new UserRef(u.getId(), u.getDisplayName(), u.getEmail()), t.getCreatedAt());
    }
}
