package com.cssms.dto;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDto {

    private DashboardDto() {
    }

    public record Cards(
            BigDecimal todaysPurchase,
            BigDecimal todaysSales,
            BigDecimal todaysCollection,
            BigDecimal todaysSupplierPayment,
            BigDecimal currentInventory,
            BigDecimal receivables,
            BigDecimal payables,
            BigDecimal monthlyProfit,
            BigDecimal monthlyLoss) {
    }

    public record Point(String label, BigDecimal value) {
    }

    public record Response(
            Cards cards,
            List<Point> purchaseTrend,
            List<Point> salesTrend,
            List<Point> profitTrend,
            List<Point> topCustomers,
            List<Point> topSuppliers,
            List<Point> materialWiseSales) {
    }
}
