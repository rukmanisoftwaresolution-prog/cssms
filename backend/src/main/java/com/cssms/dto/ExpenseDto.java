package com.cssms.dto;

import com.cssms.enums.ExpenseType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class ExpenseDto {

    private ExpenseDto() {
    }

    public record Request(
            @NotNull LocalDate expenseDate,
            @NotNull ExpenseType expenseType,
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
            @Size(max = 500) String description) {
    }

    public record Response(
            Long id,
            LocalDate expenseDate,
            ExpenseType expenseType,
            BigDecimal amount,
            String description,
            String attachmentName,
            boolean hasAttachment) {
    }
}
