package com.cssms.dto;

import com.cssms.enums.DocStatus;
import com.cssms.enums.PaymentMode;
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

public final class SalesDto {

    private SalesDto() {
    }

    public record ItemRequest(
            @NotNull Long materialId,
            @NotNull @DecimalMin(value = "0.001", message = "Quantity must be greater than 0") BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.00", message = "Rate cannot be negative") BigDecimal rate,
            @DecimalMin("0.00") BigDecimal discount,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal gstPercentage) {
    }

    public record Request(
            @NotNull LocalDate orderDate,
            @NotNull Long customerId,
            @Size(max = 500) String deliveryAddress,
            @Size(max = 20) String vehicleNumber,
            @DecimalMin("0.00") BigDecimal advanceAmount,
            PaymentMode advancePaymentMode,
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
            BigDecimal rate,
            BigDecimal discount,
            BigDecimal gstPercentage,
            BigDecimal amount,
            BigDecimal gstAmount,
            BigDecimal totalAmount) {
    }

    public record Response(
            Long id,
            String salesOrderNumber,
            String invoiceNumber,
            LocalDate orderDate,
            Long customerId,
            String customerName,
            String deliveryAddress,
            String vehicleNumber,
            String materialSummary,
            BigDecimal totalQuantity,
            BigDecimal subTotal,
            BigDecimal discountTotal,
            BigDecimal gstAmount,
            BigDecimal totalAmount,
            BigDecimal advanceAmount,
            BigDecimal receivedAmount,
            BigDecimal pendingAmount,
            PaymentStatus paymentStatus,
            DocStatus status,
            String remarks,
            List<ItemResponse> items,
            LocalDateTime createdAt) {
    }
}
