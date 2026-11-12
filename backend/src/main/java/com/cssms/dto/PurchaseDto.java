package com.cssms.dto;

import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class PurchaseDto {

    private PurchaseDto() {
    }

    public record ItemRequest(
            @NotNull Long materialId,
            @NotNull @DecimalMin(value = "0.001", message = "Quantity must be greater than 0") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.00", message = "Rate cannot be negative") BigDecimal purchaseRatePerTon,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal gstPercentage) {
    }

    public record Request(
            @NotNull LocalDate poDate,
            @NotNull Long supplierId,
            @Size(max = 20) String vehicleNumber,
            @Size(max = 500) String remarks,
            @NotEmpty @Valid List<ItemRequest> items) {
    }

    public record CancelRequest(@Size(max = 500) String reason) {
    }

    public record ItemResponse(
            Long id,
            Long materialId,
            String materialName,
            BigDecimal quantity,
            BigDecimal purchaseRatePerTon,
            BigDecimal gstPercentage,
            BigDecimal amount,
            BigDecimal gstAmount,
            BigDecimal totalAmount) {
    }

    public record Response(
            Long id,
            String poNumber,
            LocalDate poDate,
            Long supplierId,
            String supplierName,
            String vehicleNumber,
            String materialSummary,
            BigDecimal totalQuantity,
            BigDecimal amount,
            BigDecimal gstAmount,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal balanceAmount,
            PaymentStatus paymentStatus,
            DocStatus status,
            String remarks,
            List<ItemResponse> items,
            LocalDateTime createdAt) {
    }
}
