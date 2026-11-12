package com.cssms.dto;

import com.cssms.enums.ExpenseType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/** Query parameters shared by all reports; each report uses the subset it needs. */
public record ReportParams(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Long supplierId,
        Long customerId,
        Long materialId,
        String poNumber,
        String invoiceNumber,
        ExpenseType expenseType) {
}
