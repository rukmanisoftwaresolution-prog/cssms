package com.cssms.service;

import com.cssms.dto.DashboardDto.Cards;
import com.cssms.dto.DashboardDto.Point;
import com.cssms.dto.DashboardDto.Response;
import com.cssms.repository.CustomerPaymentRepository;
import com.cssms.repository.InventoryRepository;
import com.cssms.repository.PurchaseOrderRepository;
import com.cssms.repository.SalesOrderItemRepository;
import com.cssms.repository.SalesOrderRepository;
import com.cssms.repository.SupplierPaymentRepository;
import com.cssms.util.Money;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int TREND_MONTHS = 6;
    private static final int TOP_N = 5;

    private final PurchaseOrderRepository purchaseRepository;
    private final SalesOrderRepository salesRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final InventoryRepository inventoryRepository;
    private final SalesOrderItemRepository salesItemRepository;
    private final ProfitService profitService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Response get() {
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);
        BigDecimal netThisMonth = profitService.compute(month.atDay(1), month.atEndOfMonth()).netProfit();

        Cards cards = new Cards(
                Money.round(purchaseRepository.sumTotalBetween(today, today)),
                Money.round(salesRepository.sumTotalBetween(today, today)),
                Money.round(customerPaymentRepository.sumBetween(today, today)),
                Money.round(supplierPaymentRepository.sumBetween(today, today)),
                inventoryRepository.totalCurrentStock(),
                Money.round(salesRepository.totalPending()),
                Money.round(purchaseRepository.totalOutstanding()),
                netThisMonth.signum() > 0 ? netThisMonth : BigDecimal.ZERO.setScale(2),
                netThisMonth.signum() < 0 ? netThisMonth.negate() : BigDecimal.ZERO.setScale(2));

        List<Point> purchaseTrend = new ArrayList<>();
        List<Point> salesTrend = new ArrayList<>();
        List<Point> profitTrend = new ArrayList<>();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth ym = month.minusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            String label = ym.toString();
            purchaseTrend.add(new Point(label, Money.round(purchaseRepository.sumTotalBetween(start, end))));
            salesTrend.add(new Point(label, Money.round(salesRepository.sumTotalBetween(start, end))));
            profitTrend.add(new Point(label, profitService.compute(start, end).netProfit()));
        }

        return new Response(cards, purchaseTrend, salesTrend, profitTrend,
                points(salesRepository.topCustomers(PageRequest.of(0, TOP_N))),
                points(purchaseRepository.topSuppliers(PageRequest.of(0, TOP_N))),
                points(salesItemRepository.salesByMaterialName()));
    }

    private List<Point> points(List<Object[]> rows) {
        return rows.stream().map(r -> new Point((String) r[0], Money.round((BigDecimal) r[1]))).toList();
    }
}
