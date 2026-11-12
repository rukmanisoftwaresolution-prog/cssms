package com.cssms.controller;

import com.cssms.dto.PageResponse;
import com.cssms.dto.SalesDto.CancelRequest;
import com.cssms.dto.SalesDto.Request;
import com.cssms.dto.SalesDto.Response;
import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentStatus;
import com.cssms.security.Access;
import com.cssms.service.InvoiceService;
import com.cssms.service.SalesOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

@Tag(name = "Sales Orders")
@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService service;
    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize(Access.ALL_ROLES)
    public PageResponse<Response> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) DocStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        return service.search(search, customerId, materialId, status, paymentStatus, pendingOnly, fromDate, toDate, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Access.ALL_ROLES)
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize(Access.ALL_ROLES)
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.ALL_ROLES)
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(Access.ADMIN_MANAGER)
    public Response cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest request) {
        return service.cancel(id, request);
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize(Access.ALL_ROLES)
    public ResponseEntity<byte[]> invoice(@PathVariable Long id) {
        InvoiceService.Invoice invoice = invoiceService.generate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(invoice.fileName()).build().toString())
                .body(invoice.content());
    }
}
