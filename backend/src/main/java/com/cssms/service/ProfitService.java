package com.cssms.service;

import com.cssms.enums.ExpenseType;
import com.cssms.repository.ExpenseRepository;
import com.cssms.repository.PurchaseOrderRepository;
import com.cssms.repository.SalesOrderRepository;
import com.cssms.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Profit and loss maths shared by the P&amp;L report and the dashboard.
 * Gross Profit = Sales Revenue - Purchase Cost; Net Profit = Gross Profit - all expenses (all excluding GST).
 */
@Service
@RequiredArgsConstructor
public class ProfitService {

    private final SalesOrderRepository salesRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final ExpenseRepository expenseRepository;

    public record ProfitSummary(BigDecimal revenue, BigDecimal purchaseCost, Map<ExpenseType, BigDecimal> expenses,
                                BigDecimal totalExpenses, BigDecimal grossProfit, BigDecimal netProfit) {
    }

    @Transactional(readOnly = true)
    public ProfitSummary compute(LocalDate from, LocalDate to) {
        BigDecimal revenue = Money.round(salesRepository.sumRevenueBetween(from, to));
        BigDecimal purchaseCost = Money.round(purchaseRepository.sumAmountBetween(from, to));
        Map<ExpenseType, BigDecimal> expenses = new EnumMap<>(ExpenseType.class);
        for (ExpenseType type : ExpenseType.values()) {
            expenses.put(type, BigDecimal.ZERO.setScale(2));
        }
        for (Object[] row : expenseRepository.totalsByType(from, to)) {
            expenses.put((ExpenseType) row[0], Money.round((BigDecimal) row[1]));
        }
        BigDecimal totalExpenses = expenses.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal gross = revenue.subtract(purchaseCost);
        return new ProfitSummary(revenue, purchaseCost, expenses, totalExpenses, gross, gross.subtract(totalExpenses));
    }
}
