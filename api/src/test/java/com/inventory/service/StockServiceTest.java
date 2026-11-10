package com.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.dto.StockAdjustmentRequest;
import com.inventory.dto.StockMovementRequest;
import com.inventory.dto.StockTransactionResponse;
import com.inventory.entity.AppUser;
import com.inventory.entity.Category;
import com.inventory.entity.Product;
import com.inventory.entity.Role;
import com.inventory.entity.StockTransaction;
import com.inventory.entity.TransactionType;
import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.NotFoundException;
import com.inventory.exception.RequestValidationException;
import com.inventory.repository.AppUserRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.StockTransactionRepository;
import com.inventory.security.AuthenticatedUser;

class StockServiceTest {

    private static final AuthenticatedUser ACTOR =
            new AuthenticatedUser(1L, "u-1", "user@example.com", "User One", Role.USER);

    private ProductRepository products;
    private StockTransactionRepository transactions;
    private StockService service;
    private Product product;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        transactions = mock(StockTransactionRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        service = new StockService(products, transactions, users);

        AppUser user = new AppUser("u-1", "user@example.com", "User One", Role.USER);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(users.getReferenceById(1L)).thenReturn(user);

        product = new Product("ELEC-001", "Mouse", null, new Category("Electronics", null), null, BigDecimal.TEN, 5);
        ReflectionTestUtils.setField(product, "id", 9L);
        product.changeQuantity(5);
        when(products.findActiveByIdForUpdate(9L)).thenReturn(Optional.of(product));
        when(transactions.saveAndFlush(any(StockTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void stockInAddsAndRecordsQuantityAfter() {
        StockTransactionResponse response = service.stockIn(new StockMovementRequest(9L, 3, "Delivery"), ACTOR);

        assertThat(product.getQuantity()).isEqualTo(8);
        assertThat(response.type()).isEqualTo(TransactionType.IN);
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.quantityAfter()).isEqualTo(8);
        assertThat(response.product().sku()).isEqualTo("ELEC-001");
        assertThat(response.user().email()).isEqualTo("user@example.com");
    }

    @Test
    void stockOutSubtracts() {
        StockTransactionResponse response = service.stockOut(new StockMovementRequest(9L, 5, null), ACTOR);

        assertThat(product.getQuantity()).isZero();
        assertThat(response.type()).isEqualTo(TransactionType.OUT);
        assertThat(response.quantityAfter()).isZero();
    }

    @Test
    void stockOutAboveAvailableIs409() {
        InsufficientStockException e = assertThrows(InsufficientStockException.class,
                () -> service.stockOut(new StockMovementRequest(9L, 8, null), ACTOR));

        assertThat(e.getMessage()).isEqualTo("Insufficient stock: requested 8, available 5.");
        assertThat(product.getQuantity()).isEqualTo(5);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void archivedOrMissingProductIs404() {
        when(products.findActiveByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.stockIn(new StockMovementRequest(9L, 1, null), ACTOR));
        assertThrows(NotFoundException.class, () -> service.stockOut(new StockMovementRequest(9L, 1, null), ACTOR));
    }

    @Test
    void blankNoteStoredAsNull() {
        ArgumentCaptor<StockTransaction> saved = ArgumentCaptor.forClass(StockTransaction.class);

        service.stockIn(new StockMovementRequest(9L, 1, "   "), ACTOR);

        verify(transactions).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getNote()).isNull();
    }

    @Test
    void fromAfterToIsValidationError() {
        Instant to = Instant.parse("2026-01-01T00:00:00Z");

        RequestValidationException e = assertThrows(RequestValidationException.class,
                () -> service.history(null, null, null, to.plusSeconds(1), to, 0, 20));

        assertThat(e.getMessage()).contains("must not be after 'to'");
        verifyNoInteractions(transactions);
    }

    @Test
    void adjustPositiveIncreasesAndRecordsSignedDeltaAndTrimmedNote() {
        StockTransactionResponse response = service.adjust(
                new StockAdjustmentRequest(9L, 4, "  Stocktake found  "), ACTOR);

        assertThat(product.getQuantity()).isEqualTo(9);
        assertThat(response.type()).isEqualTo(TransactionType.ADJUST);
        assertThat(response.quantity()).isEqualTo(4);
        assertThat(response.quantityAfter()).isEqualTo(9);
        assertThat(response.note()).isEqualTo("Stocktake found");
    }

    @Test
    void adjustNegativeDecreasesAndRecordsNegativeDelta() {
        StockTransactionResponse response = service.adjust(new StockAdjustmentRequest(9L, -3, "Damaged"), ACTOR);

        assertThat(product.getQuantity()).isEqualTo(2);
        assertThat(response.quantity()).isEqualTo(-3);
        assertThat(response.quantityAfter()).isEqualTo(2);
    }

    @Test
    void adjustDownToZeroIsAllowed() {
        service.adjust(new StockAdjustmentRequest(9L, -5, "Written off"), ACTOR);

        assertThat(product.getQuantity()).isZero();
    }

    @Test
    void adjustBelowZeroIs409AndLeavesStockUnchanged() {
        InsufficientStockException e = assertThrows(InsufficientStockException.class,
                () -> service.adjust(new StockAdjustmentRequest(9L, -6, "Count error"), ACTOR));

        assertThat(e.getMessage()).isEqualTo("Insufficient stock for adjustment: requested decrease 6, available 5");
        assertThat(product.getQuantity()).isEqualTo(5);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void adjustZeroIsValidationErrorOnDelta() {
        RequestValidationException e = assertThrows(RequestValidationException.class,
                () -> service.adjust(new StockAdjustmentRequest(9L, 0, "No change"), ACTOR));

        assertThat(e.getMessage()).contains("must not be zero");
        assertThat(product.getQuantity()).isEqualTo(5);
        verify(transactions, never()).saveAndFlush(any());
    }

    @Test
    void adjustArchivedOrMissingProductIs404() {
        when(products.findActiveByIdForUpdate(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.adjust(new StockAdjustmentRequest(9L, 1, "Found"), ACTOR));
    }
}
