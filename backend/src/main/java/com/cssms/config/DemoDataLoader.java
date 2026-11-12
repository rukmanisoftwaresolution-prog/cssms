package com.cssms.config;

import com.cssms.dto.CustomerDto;
import com.cssms.dto.EmployeeDto;
import com.cssms.dto.ExpenseDto;
import com.cssms.dto.PaymentDto;
import com.cssms.dto.PurchaseDto;
import com.cssms.dto.SalesDto;
import com.cssms.dto.SupplierDto;
import com.cssms.dto.UserDto;
import com.cssms.enums.ExpenseType;
import com.cssms.enums.PaymentMode;
import com.cssms.enums.RoleName;
import com.cssms.enums.Status;
import com.cssms.repository.MaterialRepository;
import com.cssms.repository.SupplierRepository;
import com.cssms.repository.UserRepository;
import com.cssms.service.CustomerPaymentService;
import com.cssms.service.CustomerService;
import com.cssms.service.EmployeeService;
import com.cssms.service.ExpenseService;
import com.cssms.service.PurchaseOrderService;
import com.cssms.service.SalesOrderService;
import com.cssms.service.SupplierPaymentService;
import com.cssms.service.SupplierService;
import com.cssms.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sample data for demos and manual testing (profile "demo"). Everything goes through the real services,
 * so stock, ledgers, outstanding balances and payment statuses are consistent.
 */
@Slf4j
@Component
@Profile("demo")
@Order(2)
@RequiredArgsConstructor
public class DemoDataLoader implements ApplicationRunner {

    private final SupplierRepository supplierRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmployeeService employeeService;
    private final SupplierService supplierService;
    private final CustomerService customerService;
    private final PurchaseOrderService purchaseOrderService;
    private final SalesOrderService salesOrderService;
    private final CustomerPaymentService customerPaymentService;
    private final SupplierPaymentService supplierPaymentService;
    private final ExpenseService expenseService;
    private final Clock clock;

    @Override
    public void run(ApplicationArguments args) {
        if (supplierRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        Map<String, Long> material = new HashMap<>();
        materialRepository.findAll().forEach(m -> material.put(m.getMaterialName(), m.getId()));

        createUsers();
        createEmployees(today);
        List<Long> suppliers = createSuppliers();
        List<Long> customers = createCustomers();
        createPurchases(today, material, suppliers);
        createSales(today, material, customers);
        createExpenses(today);
        log.info("Demo data loaded. Users: admin/Admin@123, manager/Manager@123, sales/Sales@123");
    }

    private void createUsers() {
        if (!userRepository.existsByUsernameIgnoreCase("manager")) {
            userService.create(new UserDto.Request("manager", "Manager@123", "Mahesh Manager", "manager@cssms.local",
                    "9876500001", RoleName.MANAGER, true));
        }
        if (!userRepository.existsByUsernameIgnoreCase("sales")) {
            userService.create(new UserDto.Request("sales", "Sales@123", "Sana Sales", "sales@cssms.local",
                    "9876500002", RoleName.SALES_EXECUTIVE, true));
        }
    }

    private void createEmployees(LocalDate today) {
        employeeService.create(new EmployeeDto.Request("Ramesh Patil", "9822000001", "ramesh@cssms.local",
                "Plot 12, Crusher Road", today.minusYears(3), bd(28000), "Weighbridge Operator", Status.ACTIVE));
        employeeService.create(new EmployeeDto.Request("Suresh Jadhav", "9822000002", null,
                "Village Nandur", today.minusYears(2), bd(24000), "Loader Operator", Status.ACTIVE));
        employeeService.create(new EmployeeDto.Request("Anita Deshmukh", "9822000003", "anita@cssms.local",
                "Shivaji Nagar", today.minusMonths(8), bd(32000), "Accountant", Status.ACTIVE));
    }

    private List<Long> createSuppliers() {
        return List.of(
                supplierService.create(new SupplierDto.Request("Sahyadri Quarry Works", "27ABCDE1234F1Z5", "ABCDE1234F",
                        "Vikas More", "9811000001", "Quarry Road, Pune", "HDFC 50100123456789 / HDFC0001234",
                        Status.ACTIVE)).id(),
                supplierService.create(new SupplierDto.Request("Shree Stone Traders", null, null, "Ganesh Pawar",
                        "9811000002", "MIDC Area, Satara", null, Status.ACTIVE)).id(),
                supplierService.create(new SupplierDto.Request("Krishna Minerals", "27PQRSX5678L1Z9", "PQRSX5678L",
                        "Rahul Shinde", "9811000003", "Highway 4, Kolhapur", "SBI 30123456789 / SBIN0004321",
                        Status.ACTIVE)).id());
    }

    private List<Long> createCustomers() {
        return List.of(
                customerService.create(new CustomerDto.Request("Anil Kulkarni", "Kulkarni Constructions",
                        "27AAAPK1234A1Z1", "9900000001", "anil@kulkarni.example", "Baner, Pune", bd(500000),
                        Status.ACTIVE)).id(),
                customerService.create(new CustomerDto.Request("Mohan Joshi", "Joshi Infra Pvt Ltd", null,
                        "9900000002", "mohan@joshi.example", "Hinjewadi, Pune", bd(500000), Status.ACTIVE)).id(),
                customerService.create(new CustomerDto.Request("Sunil Bhosale", "Bhosale Builders", null,
                        "9900000003", null, "Wakad, Pune", bd(300000), Status.ACTIVE)).id(),
                customerService.create(new CustomerDto.Request("Dilip Chavan", null, null, "9900000004", null,
                        "Chakan, Pune", bd(100000), Status.ACTIVE)).id());
    }

    private void createPurchases(LocalDate today, Map<String, Long> m, List<Long> s) {
        var p1 = purchase(today.minusDays(55), s.get(0), "MH12AB1234",
                item(m, "Dust", 120, 350, 5), item(m, "20 MM", 200, 520, 5));
        var p2 = purchase(today.minusDays(45), s.get(1), "MH11CD5678",
                item(m, "10 MM", 150, 540, 5), item(m, "40 MM", 100, 480, 5));
        var p3 = purchase(today.minusDays(35), s.get(0), "MH12AB1234", item(m, "M-Sand", 180, 900, 5));
        purchase(today.minusDays(25), s.get(2), "MH09EF9012", item(m, "Boulder", 250, 300, 5), item(m, "P-Sand", 100, 800, 5));
        purchase(today.minusDays(12), s.get(1), "MH11CD5678", item(m, "20 MM", 150, 530, 5));
        purchase(today.minusDays(4), s.get(0), "MH12AB1234", item(m, "Dust", 80, 360, 5));

        supplierPayment(today.minusDays(50), s.get(0), p1.id(), PaymentMode.BANK_TRANSFER, bd(100000), "NEFT-1001");
        supplierPayment(today.minusDays(40), s.get(1), p2.id(), PaymentMode.CHEQUE, p2.totalAmount(), "CHQ-224411");
        supplierPayment(today.minusDays(30), s.get(0), p3.id(), PaymentMode.UPI, bd(50000), "UPI-778899");
    }

    private void createSales(LocalDate today, Map<String, Long> m, List<Long> c) {
        var s1 = sale(today.minusDays(40), c.get(0), "MH14XY1111", bd(20000), PaymentMode.CASH, sitem(m, "20 MM", 60, 700, 0, 5));
        var s2 = sale(today.minusDays(30), c.get(1), "MH14XY2222", BigDecimal.ZERO, null,
                sitem(m, "10 MM", 40, 760, 0, 5), sitem(m, "40 MM", 30, 700, 500, 5));
        var s3 = sale(today.minusDays(28), c.get(2), "MH12QQ3333", BigDecimal.ZERO, null, sitem(m, "M-Sand", 50, 1200, 0, 5));
        var s4 = sale(today.minusDays(20), c.get(0), "MH14XY1111", BigDecimal.ZERO, null,
                sitem(m, "Dust", 40, 520, 0, 5), sitem(m, "20 MM", 50, 710, 0, 5));
        sale(today.minusDays(15), c.get(3), "MH12ZZ4444", BigDecimal.ZERO, null, sitem(m, "Boulder", 100, 450, 0, 5));
        sale(today.minusDays(8), c.get(1), "MH14XY2222", bd(15000), PaymentMode.UPI, sitem(m, "20 MM", 40, 720, 0, 5));
        sale(today.minusDays(3), c.get(2), "MH12QQ3333", BigDecimal.ZERO, null, sitem(m, "M-Sand", 40, 1210, 0, 5));
        sale(today, c.get(0), "MH14XY1111", BigDecimal.ZERO, null, sitem(m, "10 MM", 30, 770, 0, 5));

        customerPayment(today.minusDays(30), c.get(0), s1.id(), PaymentMode.UPI, bd(10000), "UPI-4455");
        customerPayment(today.minusDays(22), c.get(1), s2.id(), PaymentMode.BANK_TRANSFER, s2.totalAmount(), "NEFT-9001");
        customerPayment(today.minusDays(20), c.get(2), s3.id(), PaymentMode.CASH, bd(30000), null);
        customerPayment(today.minusDays(10), c.get(0), s4.id(), PaymentMode.CHEQUE, bd(25000), "CHQ-556677");
    }

    private void createExpenses(LocalDate today) {
        expense(today.minusDays(28), ExpenseType.FUEL, 18500, "Diesel for loaders");
        expense(today.minusDays(24), ExpenseType.VEHICLE_MAINTENANCE, 7200, "Tipper servicing");
        expense(today.minusDays(20), ExpenseType.LOADING_CHARGES, 5400, "Loading labour");
        expense(today.minusDays(18), ExpenseType.ELECTRICITY, 21400, "Crusher unit electricity bill");
        expense(today.minusDays(15), ExpenseType.SALARY, 84000, "Staff salaries");
        expense(today.minusDays(14), ExpenseType.RENT, 15000, "Weighbridge land rent");
        expense(today.minusDays(9), ExpenseType.OFFICE_EXPENSE, 3100, "Stationery and printing");
        expense(today.minusDays(5), ExpenseType.FUEL, 16200, "Diesel for trucks");
        expense(today.minusDays(2), ExpenseType.MISCELLANEOUS, 1800, "Tea and refreshments");
    }

    private PurchaseDto.Response purchase(LocalDate date, Long supplierId, String vehicle, PurchaseDto.ItemRequest... items) {
        return purchaseOrderService.create(new PurchaseDto.Request(date, supplierId, vehicle, "Demo purchase", List.of(items)));
    }

    private SalesDto.Response sale(LocalDate date, Long customerId, String vehicle, BigDecimal advance, PaymentMode mode,
                                   SalesDto.ItemRequest... items) {
        return salesOrderService.create(new SalesDto.Request(date, customerId, null, vehicle, advance, mode,
                "Demo sale", List.of(items)));
    }

    private void supplierPayment(LocalDate date, Long supplierId, Long poId, PaymentMode mode, BigDecimal amount, String ref) {
        supplierPaymentService.create(new PaymentDto.SupplierRequest(date, supplierId, poId, mode, amount, ref, null));
    }

    private void customerPayment(LocalDate date, Long customerId, Long soId, PaymentMode mode, BigDecimal amount, String ref) {
        customerPaymentService.create(new PaymentDto.CustomerRequest(date, customerId, soId, mode, amount, ref, null));
    }

    private void expense(LocalDate date, ExpenseType type, int amount, String description) {
        expenseService.create(new ExpenseDto.Request(date, type, bd(amount), description));
    }

    private static PurchaseDto.ItemRequest item(Map<String, Long> m, String name, int qty, int rate, int gst) {
        return new PurchaseDto.ItemRequest(m.get(name), bd(qty), bd(rate), bd(gst));
    }

    private static SalesDto.ItemRequest sitem(Map<String, Long> m, String name, int qty, int rate, int discount, int gst) {
        return new SalesDto.ItemRequest(m.get(name), bd(qty), bd(rate), bd(discount), bd(gst));
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
