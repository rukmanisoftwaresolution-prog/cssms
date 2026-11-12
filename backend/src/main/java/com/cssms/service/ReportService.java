package com.cssms.service;

import static com.cssms.dto.ReportResponse.Type.CURRENCY;
import static com.cssms.dto.ReportResponse.Type.DATE;
import static com.cssms.dto.ReportResponse.Type.NUMBER;
import static com.cssms.dto.ReportResponse.Type.TEXT;

import com.cssms.dto.ReportParams;
import com.cssms.dto.ReportResponse;
import com.cssms.dto.ReportResponse.Builder;
import com.cssms.entity.Customer;
import com.cssms.entity.CustomerLedger;
import com.cssms.entity.CustomerPayment;
import com.cssms.entity.Expense;
import com.cssms.entity.Inventory;
import com.cssms.entity.PurchaseOrder;
import com.cssms.entity.PurchaseOrderItem;
import com.cssms.entity.SalesOrder;
import com.cssms.entity.SalesOrderItem;
import com.cssms.entity.Supplier;
import com.cssms.entity.SupplierLedger;
import com.cssms.entity.SupplierPayment;
import com.cssms.enums.DocStatus;
import com.cssms.enums.ExpenseType;
import com.cssms.exception.BadRequestException;
import com.cssms.exception.ResourceNotFoundException;
import com.cssms.repository.CustomerLedgerRepository;
import com.cssms.repository.CustomerPaymentRepository;
import com.cssms.repository.CustomerRepository;
import com.cssms.repository.InventoryRepository;
import com.cssms.repository.PurchaseOrderItemRepository;
import com.cssms.repository.PurchaseOrderRepository;
import com.cssms.repository.SalesOrderItemRepository;
import com.cssms.repository.SalesOrderRepository;
import com.cssms.repository.SupplierLedgerRepository;
import com.cssms.repository.SupplierPaymentRepository;
import com.cssms.repository.SupplierRepository;
import com.cssms.repository.ExpenseRepository;
import com.cssms.service.ProfitService.ProfitSummary;
import com.cssms.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final LocalDate MIN_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2999, 12, 31);

    private final InventoryRepository inventoryRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final PurchaseOrderItemRepository purchaseItemRepository;
    private final SalesOrderRepository salesRepository;
    private final SalesOrderItemRepository salesItemRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerLedgerRepository customerLedgerRepository;
    private final SupplierLedgerRepository supplierLedgerRepository;
    private final CustomerPaymentRepository customerPaymentRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfitService profitService;
    private final Clock clock;

    public ReportResponse generate(ReportKey key, ReportParams p) {
        return switch (key) {
            case STOCK_CURRENT -> stockCurrent();
            case STOCK_MATERIAL_WISE -> stockMaterialWise(p);
            case STOCK_MOVEMENT -> stockMovement(p);
            case PURCHASE_STATEMENT -> purchaseStatement(p);
            case SALES_STATEMENT -> salesStatement(p);
            case MATERIAL_PROFIT -> materialProfit(p);
            case CUSTOMER_STATEMENT -> customerStatement(p);
            case SUPPLIER_STATEMENT -> supplierStatement(p);
            case RECEIVABLE -> receivable();
            case PAYABLE -> payable();
            case OUTSTANDING_BALANCE -> outstandingBalance();
            case CUSTOMER_PAYMENTS -> customerPayments(p);
            case SUPPLIER_PAYMENTS -> supplierPayments(p);
            case EXPENSE_REPORT -> expenseReport(p);
            case PROFIT_LOSS -> profitLoss(p);
        };
    }

    // ---------------------------------------------------------------- stock

    private ReportResponse stockCurrent() {
        Builder report = ReportResponse.builder("Current Stock Report", "As on " + today())
                .column("materialCode", "Code", TEXT)
                .column("materialName", "Material", TEXT)
                .column("unit", "Unit", TEXT)
                .column("purchasedStock", "Purchased Stock", NUMBER)
                .column("soldStock", "Sold Stock", NUMBER)
                .column("currentStock", "Current Stock", NUMBER);
        BigDecimal total = BigDecimal.ZERO;
        for (Inventory i : inventoryRepository.findAllWithMaterial()) {
            report.row(i.getMaterial().getMaterialCode(), i.getMaterial().getMaterialName(), i.getMaterial().getUnit(),
                    i.getPurchasedStock(), i.getSoldStock(), i.getCurrentStock());
            total = total.add(i.getCurrentStock());
        }
        return report.summary("Total Current Stock", total, NUMBER).build();
    }

    private ReportResponse stockMaterialWise(ReportParams p) {
        Map<Long, BigDecimal[]> purchases = totalsByMaterial(purchaseItemRepository.totalsByMaterial(MIN_DATE, MAX_DATE));
        Map<Long, BigDecimal[]> sales = totalsByMaterial(salesItemRepository.totalsByMaterial(MIN_DATE, MAX_DATE));
        Builder report = ReportResponse.builder("Material Wise Stock Report", "As on " + today())
                .column("materialName", "Material", TEXT)
                .column("purchasedStock", "Purchased Qty", NUMBER)
                .column("soldStock", "Sold Qty", NUMBER)
                .column("currentStock", "Current Stock", NUMBER)
                .column("purchaseValue", "Purchase Value", CURRENCY)
                .column("salesValue", "Sales Value", CURRENCY);
        BigDecimal purchaseValue = BigDecimal.ZERO;
        BigDecimal salesValue = BigDecimal.ZERO;
        for (Inventory i : inventoryRepository.findAllWithMaterial()) {
            Long materialId = i.getMaterial().getId();
            if (p.materialId() != null && !p.materialId().equals(materialId)) {
                continue;
            }
            BigDecimal pv = Money.round(purchases.getOrDefault(materialId, ZERO_PAIR)[1]);
            BigDecimal sv = Money.round(sales.getOrDefault(materialId, ZERO_PAIR)[1]);
            report.row(i.getMaterial().getMaterialName(), i.getPurchasedStock(), i.getSoldStock(), i.getCurrentStock(), pv, sv);
            purchaseValue = purchaseValue.add(pv);
            salesValue = salesValue.add(sv);
        }
        return report.summary("Total Purchase Value", purchaseValue, CURRENCY)
                .summary("Total Sales Value", salesValue, CURRENCY).build();
    }

    private ReportResponse stockMovement(ReportParams p) {
        record Movement(LocalDate date, String type, String reference, String party, String material, BigDecimal qty) {
        }
        List<Movement> movements = new ArrayList<>();
        var poSpec = PurchaseOrderService.filter(null, null, p.materialId(), null, DocStatus.ACTIVE, false,
                p.fromDate(), p.toDate(), null);
        for (PurchaseOrder po : purchaseRepository.findAll(poSpec)) {
            for (PurchaseOrderItem item : po.getItems()) {
                if (p.materialId() == null || p.materialId().equals(item.getMaterial().getId())) {
                    movements.add(new Movement(po.getPoDate(), "IN", po.getPoNumber(),
                            po.getSupplier().getSupplierName(), item.getMaterial().getMaterialName(), item.getQuantity()));
                }
            }
        }
        var soSpec = SalesOrderService.filter(null, null, p.materialId(), DocStatus.ACTIVE, null, false,
                p.fromDate(), p.toDate(), null);
        for (SalesOrder so : salesRepository.findAll(soSpec)) {
            for (SalesOrderItem item : so.getItems()) {
                if (p.materialId() == null || p.materialId().equals(item.getMaterial().getId())) {
                    movements.add(new Movement(so.getOrderDate(), "OUT", so.getInvoiceNumber(),
                            so.getCustomer().getCustomerName(), item.getMaterial().getMaterialName(), item.getQuantity()));
                }
            }
        }
        movements.sort(Comparator.comparing(Movement::date).thenComparing(Movement::reference));

        Builder report = ReportResponse.builder("Stock Movement Report", period(p))
                .column("date", "Date", DATE)
                .column("type", "Movement", TEXT)
                .column("reference", "Reference", TEXT)
                .column("party", "Supplier / Customer", TEXT)
                .column("material", "Material", TEXT)
                .column("quantity", "Quantity", NUMBER);
        BigDecimal in = BigDecimal.ZERO;
        BigDecimal out = BigDecimal.ZERO;
        for (Movement m : movements) {
            report.row(m.date(), m.type(), m.reference(), m.party(), m.material(), m.qty());
            if (m.type().equals("IN")) {
                in = in.add(m.qty());
            } else {
                out = out.add(m.qty());
            }
        }
        return report.summary("Total Stock In", in, NUMBER).summary("Total Stock Out", out, NUMBER)
                .summary("Net Movement", in.subtract(out), NUMBER).build();
    }

    // ---------------------------------------------------------------- purchase & sales statements

    private ReportResponse purchaseStatement(ReportParams p) {
        var spec = PurchaseOrderService.filter(null, p.supplierId(), p.materialId(), null, DocStatus.ACTIVE, false,
                p.fromDate(), p.toDate(), p.poNumber());
        Builder report = ReportResponse.builder("Purchase Order Statement", period(p))
                .column("poNumber", "PO Number", TEXT)
                .column("poDate", "PO Date", DATE)
                .column("supplierName", "Supplier Name", TEXT)
                .column("material", "Material", TEXT)
                .column("quantity", "Quantity", NUMBER)
                .column("rate", "Purchase Rate", CURRENCY)
                .column("amount", "Amount", CURRENCY)
                .column("gst", "GST", CURRENCY)
                .column("total", "Total Amount", CURRENCY)
                .column("paid", "Paid Amount", CURRENCY)
                .column("balance", "Balance Amount", CURRENCY)
                .column("status", "Status", TEXT);
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (PurchaseOrder po : purchaseRepository.findAll(spec, Sort.by("poDate", "id"))) {
            var dto = PurchaseOrderService.toResponse(po);
            report.row(po.getPoNumber(), po.getPoDate(), po.getSupplier().getSupplierName(), dto.materialSummary(),
                    dto.totalQuantity(), averageRate(po.getAmount(), dto.totalQuantity()), po.getAmount(),
                    po.getGstAmount(), po.getTotalAmount(), po.getPaidAmount(), po.getBalanceAmount(),
                    po.getPaymentStatus().name());
            amount = amount.add(po.getAmount());
            gst = gst.add(po.getGstAmount());
            total = total.add(po.getTotalAmount());
            paid = paid.add(po.getPaidAmount());
        }
        return report.summary("Amount", amount, CURRENCY).summary("GST", gst, CURRENCY)
                .summary("Total Amount", total, CURRENCY).summary("Paid Amount", paid, CURRENCY)
                .summary("Balance Amount", total.subtract(paid), CURRENCY).build();
    }

    private ReportResponse salesStatement(ReportParams p) {
        var spec = SalesOrderService.filter(null, p.customerId(), p.materialId(), DocStatus.ACTIVE, null, false,
                p.fromDate(), p.toDate(), p.invoiceNumber());
        Builder report = ReportResponse.builder("Sales Statement", period(p))
                .column("invoiceNumber", "Invoice Number", TEXT)
                .column("invoiceDate", "Invoice Date", DATE)
                .column("customerName", "Customer Name", TEXT)
                .column("material", "Material", TEXT)
                .column("quantity", "Quantity", NUMBER)
                .column("rate", "Sale Rate", CURRENCY)
                .column("amount", "Amount", CURRENCY)
                .column("gst", "GST", CURRENCY)
                .column("total", "Total Amount", CURRENCY)
                .column("received", "Received Amount", CURRENCY)
                .column("pending", "Pending Amount", CURRENCY);
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal gst = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        BigDecimal pending = BigDecimal.ZERO;
        for (SalesOrder so : salesRepository.findAll(spec, Sort.by("orderDate", "id"))) {
            var dto = SalesOrderService.toResponse(so);
            BigDecimal net = so.getSubTotal().subtract(so.getDiscountTotal());
            report.row(so.getInvoiceNumber(), so.getOrderDate(), so.getCustomer().getCustomerName(),
                    dto.materialSummary(), dto.totalQuantity(), averageRate(net, dto.totalQuantity()), net,
                    so.getGstAmount(), so.getTotalAmount(), so.getReceivedAmount(), so.getPendingAmount());
            amount = amount.add(net);
            gst = gst.add(so.getGstAmount());
            total = total.add(so.getTotalAmount());
            received = received.add(so.getReceivedAmount());
            pending = pending.add(so.getPendingAmount());
        }
        return report.summary("Amount", amount, CURRENCY).summary("GST", gst, CURRENCY)
                .summary("Total Amount", total, CURRENCY).summary("Received Amount", received, CURRENCY)
                .summary("Pending Amount", pending, CURRENCY).build();
    }

    private ReportResponse materialProfit(ReportParams p) {
        LocalDate from = p.fromDate() != null ? p.fromDate() : MIN_DATE;
        LocalDate to = p.toDate() != null ? p.toDate() : MAX_DATE;
        Map<Long, BigDecimal[]> purchasedInPeriod = totalsByMaterial(purchaseItemRepository.totalsByMaterial(from, to));
        Map<Long, BigDecimal[]> soldInPeriod = totalsByMaterial(salesItemRepository.totalsByMaterial(from, to));
        Map<Long, BigDecimal[]> purchasedAllTime = totalsByMaterial(purchaseItemRepository.totalsByMaterial(MIN_DATE, MAX_DATE));

        Builder report = ReportResponse.builder("Material Wise Profit Report", period(p))
                .column("material", "Material", TEXT)
                .column("purchasedQty", "Purchased Quantity", NUMBER)
                .column("soldQty", "Sold Quantity", NUMBER)
                .column("availableQty", "Available Quantity", NUMBER)
                .column("revenue", "Revenue", CURRENCY)
                .column("profit", "Profit", CURRENCY);
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;
        for (Inventory i : inventoryRepository.findAllWithMaterial()) {
            Long id = i.getMaterial().getId();
            BigDecimal purchasedQty = purchasedInPeriod.getOrDefault(id, ZERO_PAIR)[0];
            BigDecimal[] sold = soldInPeriod.getOrDefault(id, ZERO_PAIR);
            BigDecimal[] allTime = purchasedAllTime.getOrDefault(id, ZERO_PAIR);
            BigDecimal averageCost = allTime[0].signum() == 0 ? BigDecimal.ZERO
                    : allTime[1].divide(allTime[0], 6, RoundingMode.HALF_UP);
            BigDecimal revenue = Money.round(sold[1]);
            BigDecimal profit = Money.round(revenue.subtract(sold[0].multiply(averageCost)));
            report.row(i.getMaterial().getMaterialName(), purchasedQty, sold[0], i.getCurrentStock(), revenue, profit);
            totalRevenue = totalRevenue.add(revenue);
            totalProfit = totalProfit.add(profit);
        }
        return report.summary("Total Revenue", totalRevenue, CURRENCY)
                .summary("Total Profit", totalProfit, CURRENCY).build();
    }

    // ---------------------------------------------------------------- accounts

    private ReportResponse customerStatement(ReportParams p) {
        Customer customer = customerRepository.findById(requireId(p.customerId(), "customerId"))
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", p.customerId()));
        LocalDate from = p.fromDate() != null ? p.fromDate() : MIN_DATE;
        LocalDate to = p.toDate() != null ? p.toDate() : LocalDate.now(clock);
        BigDecimal opening = customerLedgerRepository.sumDebitBefore(customer.getId(), from)
                .subtract(customerLedgerRepository.sumCreditBefore(customer.getId(), from));

        Builder report = ReportResponse.builder("Customer Account Statement - " + customer.getCustomerName(), period(p))
                .column("date", "Date", DATE)
                .column("particular", "Particular", TEXT)
                .column("debit", "Debit", CURRENCY)
                .column("credit", "Credit", CURRENCY)
                .column("balance", "Balance", CURRENCY);
        report.row(null, "Opening Balance", null, null, opening);
        BigDecimal balance = opening;
        BigDecimal sales = BigDecimal.ZERO;
        BigDecimal collection = BigDecimal.ZERO;
        for (CustomerLedger l : customerLedgerRepository.findByCustomerIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(customer.getId(), from, to)) {
            balance = balance.add(l.getDebit()).subtract(l.getCredit());
            sales = sales.add(l.getDebit());
            collection = collection.add(l.getCredit());
            report.row(l.getTxnDate(), l.getParticular(), l.getDebit(), l.getCredit(), balance);
        }
        return report.summary("Opening Balance", opening, CURRENCY).summary("Total Sales", sales, CURRENCY)
                .summary("Total Collection", collection, CURRENCY).summary("Closing Balance", balance, CURRENCY).build();
    }

    private ReportResponse supplierStatement(ReportParams p) {
        Supplier supplier = supplierRepository.findById(requireId(p.supplierId(), "supplierId"))
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", p.supplierId()));
        LocalDate from = p.fromDate() != null ? p.fromDate() : MIN_DATE;
        LocalDate to = p.toDate() != null ? p.toDate() : LocalDate.now(clock);
        // Balance is what we owe the supplier: purchases (credit) minus payments (debit).
        BigDecimal opening = supplierLedgerRepository.sumCreditBefore(supplier.getId(), from)
                .subtract(supplierLedgerRepository.sumDebitBefore(supplier.getId(), from));

        Builder report = ReportResponse.builder("Supplier Account Statement - " + supplier.getSupplierName(), period(p))
                .column("date", "Date", DATE)
                .column("particular", "Particular", TEXT)
                .column("debit", "Debit", CURRENCY)
                .column("credit", "Credit", CURRENCY)
                .column("balance", "Balance", CURRENCY);
        report.row(null, "Opening Balance", null, null, opening);
        BigDecimal balance = opening;
        BigDecimal purchases = BigDecimal.ZERO;
        BigDecimal payments = BigDecimal.ZERO;
        for (SupplierLedger l : supplierLedgerRepository.findBySupplierIdAndTxnDateBetweenOrderByTxnDateAscIdAsc(supplier.getId(), from, to)) {
            balance = balance.add(l.getCredit()).subtract(l.getDebit());
            purchases = purchases.add(l.getCredit());
            payments = payments.add(l.getDebit());
            report.row(l.getTxnDate(), l.getParticular(), l.getDebit(), l.getCredit(), balance);
        }
        return report.summary("Opening Balance", opening, CURRENCY).summary("Purchase Amount", purchases, CURRENCY)
                .summary("Total Payment", payments, CURRENCY).summary("Closing Balance", balance, CURRENCY).build();
    }

    private ReportResponse receivable() {
        LocalDate today = LocalDate.now(clock);
        record Row(BigDecimal invoice, BigDecimal received, BigDecimal pending, LocalDate oldest) {
        }
        Map<String, Row> byCustomer = new LinkedHashMap<>();
        List<SalesOrder> orders = salesRepository.findAllActiveWithCustomer();
        orders.sort(Comparator.comparing(o -> o.getCustomer().getCustomerName(), String.CASE_INSENSITIVE_ORDER));
        for (SalesOrder so : orders) {
            if (so.getPendingAmount().signum() <= 0) {
                continue;
            }
            byCustomer.merge(so.getCustomer().getCustomerName(),
                    new Row(so.getTotalAmount(), so.getReceivedAmount(), so.getPendingAmount(), so.getOrderDate()),
                    (a, b) -> new Row(a.invoice().add(b.invoice()), a.received().add(b.received()),
                            a.pending().add(b.pending()), a.oldest().isBefore(b.oldest()) ? a.oldest() : b.oldest()));
        }
        Builder report = ReportResponse.builder("Receivable Report", "As on " + today)
                .column("customerName", "Customer Name", TEXT)
                .column("invoiceAmount", "Invoice Amount", CURRENCY)
                .column("receivedAmount", "Received Amount", CURRENCY)
                .column("pendingAmount", "Pending Amount", CURRENCY)
                .column("agingDays", "Aging Days", NUMBER);
        BigDecimal pendingTotal = BigDecimal.ZERO;
        for (var entry : byCustomer.entrySet()) {
            Row r = entry.getValue();
            report.row(entry.getKey(), r.invoice(), r.received(), r.pending(), ChronoUnit.DAYS.between(r.oldest(), today));
            pendingTotal = pendingTotal.add(r.pending());
        }
        return report.summary("Total Receivable", pendingTotal, CURRENCY).build();
    }

    private ReportResponse payable() {
        LocalDate today = LocalDate.now(clock);
        record Row(BigDecimal purchase, BigDecimal paid, BigDecimal balance, LocalDate oldest) {
        }
        Map<String, Row> bySupplier = new LinkedHashMap<>();
        List<PurchaseOrder> orders = purchaseRepository.findAllActiveWithSupplier();
        orders.sort(Comparator.comparing(o -> o.getSupplier().getSupplierName(), String.CASE_INSENSITIVE_ORDER));
        for (PurchaseOrder po : orders) {
            if (po.getBalanceAmount().signum() <= 0) {
                continue;
            }
            bySupplier.merge(po.getSupplier().getSupplierName(),
                    new Row(po.getTotalAmount(), po.getPaidAmount(), po.getBalanceAmount(), po.getPoDate()),
                    (a, b) -> new Row(a.purchase().add(b.purchase()), a.paid().add(b.paid()),
                            a.balance().add(b.balance()), a.oldest().isBefore(b.oldest()) ? a.oldest() : b.oldest()));
        }
        Builder report = ReportResponse.builder("Payable Report", "As on " + today)
                .column("supplierName", "Supplier Name", TEXT)
                .column("purchaseAmount", "Purchase Amount", CURRENCY)
                .column("paidAmount", "Paid Amount", CURRENCY)
                .column("balanceAmount", "Balance Amount", CURRENCY)
                .column("agingDays", "Aging Days", NUMBER);
        BigDecimal balanceTotal = BigDecimal.ZERO;
        for (var entry : bySupplier.entrySet()) {
            Row r = entry.getValue();
            report.row(entry.getKey(), r.purchase(), r.paid(), r.balance(), ChronoUnit.DAYS.between(r.oldest(), today));
            balanceTotal = balanceTotal.add(r.balance());
        }
        return report.summary("Total Payable", balanceTotal, CURRENCY).build();
    }

    private ReportResponse outstandingBalance() {
        Map<String, BigDecimal[]> bySupplier = new LinkedHashMap<>();
        List<PurchaseOrder> orders = purchaseRepository.findAllActiveWithSupplier();
        orders.sort(Comparator.comparing(o -> o.getSupplier().getSupplierName(), String.CASE_INSENSITIVE_ORDER));
        for (PurchaseOrder po : orders) {
            BigDecimal[] totals = bySupplier.computeIfAbsent(po.getSupplier().getSupplierName(),
                    k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            totals[0] = totals[0].add(po.getTotalAmount());
            totals[1] = totals[1].add(po.getPaidAmount());
        }
        Builder report = ReportResponse.builder("Supplier Outstanding Balance Report", "As on " + today())
                .column("supplierName", "Supplier Name", TEXT)
                .column("purchaseAmount", "Purchase Amount", CURRENCY)
                .column("paidAmount", "Paid Amount", CURRENCY)
                .column("balanceAmount", "Outstanding Balance", CURRENCY);
        BigDecimal outstanding = BigDecimal.ZERO;
        for (var entry : bySupplier.entrySet()) {
            BigDecimal balance = entry.getValue()[0].subtract(entry.getValue()[1]);
            report.row(entry.getKey(), entry.getValue()[0], entry.getValue()[1], balance);
            outstanding = outstanding.add(balance);
        }
        return report.summary("Total Outstanding", outstanding, CURRENCY).build();
    }

    // ---------------------------------------------------------------- payments, expenses, P&L

    private ReportResponse customerPayments(ReportParams p) {
        var spec = CustomerPaymentService.filter(null, p.customerId(), null, p.fromDate(), p.toDate());
        Builder report = ReportResponse.builder("Customer Payment Report", period(p))
                .column("date", "Payment Date", DATE)
                .column("customerName", "Customer", TEXT)
                .column("invoiceNumber", "Invoice Number", TEXT)
                .column("paymentMode", "Mode", TEXT)
                .column("referenceNumber", "Reference", TEXT)
                .column("amount", "Amount Received", CURRENCY);
        BigDecimal total = BigDecimal.ZERO;
        Map<String, BigDecimal> byMode = new LinkedHashMap<>();
        for (CustomerPayment pay : customerPaymentRepository.findAll(spec, Sort.by("paymentDate", "id"))) {
            report.row(pay.getPaymentDate(), pay.getCustomer().getCustomerName(), pay.getSalesOrder().getInvoiceNumber(),
                    pay.getPaymentMode().name(), pay.getReferenceNumber(), pay.getAmountReceived());
            total = total.add(pay.getAmountReceived());
            byMode.merge(pay.getPaymentMode().name(), pay.getAmountReceived(), BigDecimal::add);
        }
        byMode.forEach((mode, amount) -> report.summary("Total " + mode, amount, CURRENCY));
        return report.summary("Total Received", total, CURRENCY).build();
    }

    private ReportResponse supplierPayments(ReportParams p) {
        var spec = SupplierPaymentService.filter(null, p.supplierId(), null, p.fromDate(), p.toDate());
        Builder report = ReportResponse.builder("Supplier Payment Report", period(p))
                .column("date", "Payment Date", DATE)
                .column("supplierName", "Supplier", TEXT)
                .column("poNumber", "PO Number", TEXT)
                .column("paymentMode", "Mode", TEXT)
                .column("referenceNumber", "Reference", TEXT)
                .column("amount", "Amount Paid", CURRENCY);
        BigDecimal total = BigDecimal.ZERO;
        for (SupplierPayment pay : supplierPaymentRepository.findAll(spec, Sort.by("paymentDate", "id"))) {
            report.row(pay.getPaymentDate(), pay.getSupplier().getSupplierName(), pay.getPurchaseOrder().getPoNumber(),
                    pay.getPaymentMode().name(), pay.getReferenceNumber(), pay.getAmountPaid());
            total = total.add(pay.getAmountPaid());
        }
        return report.summary("Total Paid", total, CURRENCY).build();
    }

    private ReportResponse expenseReport(ReportParams p) {
        var spec = ExpenseService.filter(null, p.expenseType(), p.fromDate(), p.toDate());
        Builder report = ReportResponse.builder("Expense Report", period(p))
                .column("date", "Date", DATE)
                .column("expenseType", "Expense Type", TEXT)
                .column("description", "Description", TEXT)
                .column("amount", "Amount", CURRENCY);
        BigDecimal total = BigDecimal.ZERO;
        Map<ExpenseType, BigDecimal> byType = new LinkedHashMap<>();
        for (Expense e : expenseRepository.findAll(spec, Sort.by("expenseDate", "id"))) {
            report.row(e.getExpenseDate(), label(e.getExpenseType()), e.getDescription(), e.getAmount());
            total = total.add(e.getAmount());
            byType.merge(e.getExpenseType(), e.getAmount(), BigDecimal::add);
        }
        byType.forEach((type, amount) -> report.summary(label(type), amount, CURRENCY));
        return report.summary("Total Expenses", total, CURRENCY).build();
    }

    private ReportResponse profitLoss(ReportParams p) {
        if (p.fromDate() == null || p.toDate() == null) {
            throw new BadRequestException("fromDate and toDate are required");
        }
        ProfitSummary s = profitService.compute(p.fromDate(), p.toDate());
        Map<ExpenseType, BigDecimal> e = s.expenses();
        BigDecimal office = e.get(ExpenseType.OFFICE_EXPENSE).add(e.get(ExpenseType.ELECTRICITY)).add(e.get(ExpenseType.RENT));

        Builder report = ReportResponse.builder("Profit and Loss Report", period(p))
                .column("section", "Section", TEXT)
                .column("particular", "Particular", TEXT)
                .column("amount", "Amount", CURRENCY);
        report.row("Revenue", "Total Sales Revenue", s.revenue());
        report.row("Cost of Purchase", "Purchase Cost", s.purchaseCost());
        report.row("Gross Profit", "Gross Profit (Sales Revenue - Purchase Cost)", s.grossProfit());
        report.row("Expenses", "Fuel Cost", e.get(ExpenseType.FUEL));
        report.row("Expenses", "Transport Cost (Loading Charges)", e.get(ExpenseType.LOADING_CHARGES));
        report.row("Expenses", "Maintenance Cost", e.get(ExpenseType.VEHICLE_MAINTENANCE));
        report.row("Expenses", "Salary Cost", e.get(ExpenseType.SALARY));
        report.row("Expenses", "Office Expense (incl. Electricity and Rent)", office);
        report.row("Expenses", "Misc Expense", e.get(ExpenseType.MISCELLANEOUS));
        report.row("Expenses", "Total Expenses", s.totalExpenses());
        report.row("Net Profit", s.netProfit().signum() >= 0 ? "Net Profit" : "Net Loss", s.netProfit());
        return report.summary("Total Sales Revenue", s.revenue(), CURRENCY)
                .summary("Gross Profit", s.grossProfit(), CURRENCY)
                .summary("Total Expenses", s.totalExpenses(), CURRENCY)
                .summary(s.netProfit().signum() >= 0 ? "Net Profit" : "Net Loss", s.netProfit(), CURRENCY).build();
    }

    // ---------------------------------------------------------------- helpers

    private static final BigDecimal[] ZERO_PAIR = {BigDecimal.ZERO, BigDecimal.ZERO};

    private Map<Long, BigDecimal[]> totalsByMaterial(List<Object[]> rows) {
        Map<Long, BigDecimal[]> result = new HashMap<>();
        for (Object[] row : rows) {
            result.put((Long) row[0], new BigDecimal[] {(BigDecimal) row[1], (BigDecimal) row[2]});
        }
        return result;
    }

    private static BigDecimal averageRate(BigDecimal amount, BigDecimal quantity) {
        return quantity.signum() == 0 ? BigDecimal.ZERO.setScale(2) : amount.divide(quantity, 2, RoundingMode.HALF_UP);
    }

    private static Long requireId(Long id, String name) {
        if (id == null) {
            throw new BadRequestException(name + " is required");
        }
        return id;
    }

    private String today() {
        return LocalDate.now(clock).toString();
    }

    private static String period(ReportParams p) {
        if (p.fromDate() == null && p.toDate() == null) {
            return "All dates";
        }
        return "Period: " + (p.fromDate() != null ? p.fromDate() : "beginning") + " to "
                + (p.toDate() != null ? p.toDate() : "date");
    }

    private static String label(ExpenseType type) {
        return java.util.Arrays.stream(type.name().split("_"))
                .map(w -> w.charAt(0) + w.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
