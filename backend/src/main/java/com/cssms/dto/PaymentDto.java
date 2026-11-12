package com.cssms.dto;

import com.cssms.enums.PaymentMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class PaymentDto {

    private PaymentDto() {
    }

    public record CustomerRequest(
            @NotNull LocalDate paymentDate,
            @NotNull Long customerId,
            @NotNull Long salesOrderId,
            @NotNull PaymentMode paymentMode,
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amountReceived,
            @Size(max = 100) String referenceNumber,
            @Size(max = 500) String remarks) {
    }

    public record CustomerResponse(
            Long id,
            LocalDate paymentDate,
            Long customerId,
            String customerName,
            Long salesOrderId,
            String invoiceNumber,
            PaymentMode paymentMode,
            BigDecimal amountReceived,
            String referenceNumber,
            String remarks,
            boolean advance) {
    }

    public record SupplierRequest(
            @NotNull LocalDate paymentDate,
            @NotNull Long supplierId,
            @NotNull Long purchaseOrderId,
            @NotNull PaymentMode paymentMode,
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amountPaid,
            @Size(max = 100) String referenceNumber,
            @Size(max = 500) String remarks) {
    }

    public record SupplierResponse(
            Long id,
            LocalDate paymentDate,
            Long supplierId,
            String supplierName,
            Long purchaseOrderId,
            String poNumber,
            PaymentMode paymentMode,
            BigDecimal amountPaid,
            String referenceNumber,
            String remarks) {
    }
}
