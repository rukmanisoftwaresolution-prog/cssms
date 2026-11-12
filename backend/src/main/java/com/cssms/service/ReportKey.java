package com.cssms.service;

import java.util.Arrays;
import java.util.Optional;

/** Report identifiers used in /api/reports/{key}. */
public enum ReportKey {
    STOCK_CURRENT("stock-current", false),
    STOCK_MATERIAL_WISE("stock-material-wise", false),
    STOCK_MOVEMENT("stock-movement", false),
    PURCHASE_STATEMENT("purchase-statement", false),
    SALES_STATEMENT("sales-statement", true),
    MATERIAL_PROFIT("material-profit", false),
    CUSTOMER_STATEMENT("customer-statement", true),
    SUPPLIER_STATEMENT("supplier-statement", false),
    RECEIVABLE("receivable", true),
    PAYABLE("payable", false),
    OUTSTANDING_BALANCE("outstanding-balance", false),
    CUSTOMER_PAYMENTS("customer-payments", false),
    SUPPLIER_PAYMENTS("supplier-payments", false),
    EXPENSE_REPORT("expense-report", false),
    PROFIT_LOSS("profit-loss", false);

    private final String path;
    private final boolean salesExecutiveAllowed;

    ReportKey(String path, boolean salesExecutiveAllowed) {
        this.path = path;
        this.salesExecutiveAllowed = salesExecutiveAllowed;
    }

    public String path() {
        return path;
    }

    public boolean salesExecutiveAllowed() {
        return salesExecutiveAllowed;
    }

    public static Optional<ReportKey> fromPath(String path) {
        return Arrays.stream(values()).filter(k -> k.path.equals(path)).findFirst();
    }
}
