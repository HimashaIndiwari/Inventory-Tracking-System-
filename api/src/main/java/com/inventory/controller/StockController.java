package com.inventory.controller;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.dto.PageResponse;
import com.inventory.dto.StockMovementRequest;
import com.inventory.dto.StockTransactionResponse;
import com.inventory.entity.TransactionType;
import com.inventory.security.AuthenticatedUser;
import com.inventory.service.StockService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Stock movements are available to every authenticated user (USER and ADMIN). */
@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @PostMapping("/in")
    @ResponseStatus(HttpStatus.CREATED)
    public StockTransactionResponse stockIn(@Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.stockIn(request, user);
    }

    @PostMapping("/out")
    @ResponseStatus(HttpStatus.CREATED)
    public StockTransactionResponse stockOut(@Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return service.stockOut(request, user);
    }

    @GetMapping("/history")
    public PageResponse<StockTransactionResponse> history(@RequestParam(required = false) Long productId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(productId, type, search, from, to, page, size);
    }
}
