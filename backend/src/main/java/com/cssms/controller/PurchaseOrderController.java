package com.cssms.controller;

import com.cssms.dto.PageResponse;
import com.cssms.dto.PurchaseDto.CancelRequest;
import com.cssms.dto.PurchaseDto.Request;
import com.cssms.dto.PurchaseDto.Response;
import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentStatus;
import com.cssms.security.Access;
import com.cssms.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Purchase Orders")
@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize(Access.ADMIN_MANAGER)
public class PurchaseOrderController {

    private final PurchaseOrderService service;

    @GetMapping
    public PageResponse<Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) DocStatus status,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return service.search(search, supplierId, materialId, paymentStatus, status, pendingOnly, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public Response cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest request) {
        return service.cancel(id, request);
    }
}
