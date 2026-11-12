package com.cssms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class CssmsApiIntegrationTest extends PostgresTestBase {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    // ------------------------------------------------------------------ auth & roles

    @Test
    void loginRefreshRotationAndLogout() throws Exception {
        JsonNode login = call(post("/api/auth/login"), null, Map.of("username", "admin", "password", "Admin@123"), 200);
        assertThat(login.get("accessToken").asText()).isNotBlank();
        assertThat(login.get("user").get("role").asText()).isEqualTo("ADMIN");

        String refresh = login.get("refreshToken").asText();
        JsonNode rotated = call(post("/api/auth/refresh"), null, Map.of("refreshToken", refresh), 200);
        assertThat(rotated.get("refreshToken").asText()).isNotEqualTo(refresh);

        // Reusing the old token is rejected and revokes the token family.
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", refresh), 401);
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", rotated.get("refreshToken").asText()), 401);
    }

    @Test
    void badCredentialsAndMissingTokenAreRejected() throws Exception {
        call(post("/api/auth/login"), null, Map.of("username", "admin", "password", "wrong"), 401);
        call(get("/api/customers"), null, null, 401);
        call(get("/api/customers"), "garbage.token.value", null, 401);
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode login = call(post("/api/auth/login"), null, Map.of("username", "admin", "password", "Admin@123"), 200);
        String refresh = login.get("refreshToken").asText();

        call(post("/api/auth/logout"), login.get("accessToken").asText(), Map.of("refreshToken", refresh), 204);
        call(post("/api/auth/refresh"), null, Map.of("refreshToken", refresh), 401);
    }

    @Test
    void roleMatrixIsEnforced() throws Exception {
        String sales = token("sales", "Sales@123");
        String manager = token("manager", "Manager@123");

        call(get("/api/suppliers"), sales, null, 403);
        call(get("/api/purchase-orders"), sales, null, 403);
        call(get("/api/expenses"), sales, null, 403);
        call(get("/api/users"), manager, null, 403);
        call(get("/api/employees"), sales, null, 403);
        call(get("/api/customers"), sales, null, 200);
        call(get("/api/materials"), sales, null, 200);
        call(get("/api/inventory"), sales, null, 200);
        call(get("/api/dashboard"), sales, null, 200);
        call(get("/api/reports/sales-statement"), sales, null, 200);
        call(get("/api/reports/purchase-statement"), sales, null, 403);
        call(get("/api/reports/profit-loss?fromDate=2020-01-01&toDate=2030-01-01"), sales, null, 403);
        call(get("/api/employees"), manager, null, 200);
        call(post("/api/employees"), manager, employee("Blocked Person"), 403);
    }

    @Test
    void changePasswordAndProfile() throws Exception {
        String admin = token("admin", "Admin@123");
        JsonNode me = call(get("/api/auth/me"), admin, null, 200);
        assertThat(me.get("username").asText()).isEqualTo("admin");

        call(post("/api/auth/change-password"), admin, Map.of("currentPassword", "nope", "newPassword", "Whatever1"), 400);
        call(post("/api/auth/change-password"), admin, Map.of("currentPassword", "Admin@123", "newPassword", "weak"), 400);

        call(post("/api/users"), admin, Map.of("username", "resetuser", "password", "Reset@1234", "fullName", "Reset User",
                "email", "reset@cssms.local", "role", "MANAGER", "active", true), 201);
        JsonNode forgot = call(post("/api/auth/forgot-password"), null, Map.of("email", "reset@cssms.local"), 200);
        String resetToken = forgot.get("resetToken").asText();
        call(post("/api/auth/reset-password"), null, Map.of("token", resetToken, "newPassword", "Reset@5678"), 204);
        call(post("/api/auth/reset-password"), null, Map.of("token", resetToken, "newPassword", "Reset@9999"), 409);
        token("resetuser", "Reset@5678");
        call(post("/api/auth/login"), null, Map.of("username", "resetuser", "password", "Reset@1234"), 401);

        // an unknown email gives the same generic answer and no token
        JsonNode unknown = call(post("/api/auth/forgot-password"), null, Map.of("email", "nobody@cssms.local"), 200);
        assertThat(unknown.get("resetToken").isNull()).isTrue();
    }

    // ------------------------------------------------------------------ validation & master data

    @Test
    void validationErrorsReturnFieldMessages() throws Exception {
        String admin = token("admin", "Admin@123");
        JsonNode error = call(post("/api/suppliers"), admin,
                Map.of("supplierName", "", "mobileNumber", "123", "gstNumber", "BAD"), 400);
        assertThat(error.get("fieldErrors").has("supplierName")).isTrue();
        assertThat(error.get("fieldErrors").has("mobileNumber")).isTrue();
        assertThat(error.get("fieldErrors").has("gstNumber")).isTrue();
        assertThat(error.get("fieldErrors").has("address")).isTrue();
    }

    @Test
    void employeeCrudWithSoftDelete() throws Exception {
        String admin = token("admin", "Admin@123");
        JsonNode created = call(post("/api/employees"), admin, employee("Test Employee"), 201);
        long id = created.get("id").asLong();
        assertThat(created.get("employeeCode").asText()).startsWith("EMP-");

        JsonNode updated = call(put("/api/employees/" + id), admin, employee("Renamed Employee"), 200);
        assertThat(updated.get("fullName").asText()).isEqualTo("Renamed Employee");

        JsonNode search = call(get("/api/employees?search=renamed&sort=fullName,asc"), admin, null, 200);
        assertThat(search.get("totalElements").asInt()).isEqualTo(1);

        call(delete("/api/employees/" + id), admin, null, 204);
        call(get("/api/employees/" + id), admin, null, 404);
        // Unknown sort properties must not cause a server error.
        call(get("/api/employees?sort=doesNotExist,desc"), admin, null, 200);
    }

        @Test
        void customerAndSupplierCrudSearchAndValidation() throws Exception {
        String admin = token("admin", "Admin@123");
        JsonNode customer = call(post("/api/customers"), admin, Map.of("customerName", "CRUD Customer",
            "mobileNumber", "9000000011", "address", "Customer Road", "creditLimit", 2500), 201);
        long customerId = customer.get("id").asLong();
        assertThat(customer.get("customerCode").asText()).startsWith("CUST-");

        JsonNode updatedCustomer = call(put("/api/customers/" + customerId), admin, Map.of("customerName", "Updated CRUD Customer",
            "mobileNumber", "9000000012", "address", "Updated Road", "creditLimit", 3000, "status", "INACTIVE"), 200);
        assertThat(updatedCustomer.get("customerName").asText()).isEqualTo("Updated CRUD Customer");
        assertThat(call(get("/api/customers?search=Updated&status=INACTIVE"), admin, null, 200)
            .get("totalElements").asInt()).isEqualTo(1);

        JsonNode invalidCustomer = call(post("/api/customers"), admin, Map.of("customerName", "Invalid Customer",
            "mobileNumber", "123", "gstNumber", "BAD", "address", "", "creditLimit", 0), 400);
        assertThat(invalidCustomer.get("fieldErrors").has("mobileNumber")).isTrue();
        assertThat(invalidCustomer.get("fieldErrors").has("gstNumber")).isTrue();
        assertThat(invalidCustomer.get("fieldErrors").has("address")).isTrue();

        call(delete("/api/customers/" + customerId), admin, null, 204);
        call(get("/api/customers/" + customerId), admin, null, 404);

        JsonNode supplier = call(post("/api/suppliers"), admin, Map.of("supplierName", "CRUD Supplier",
            "contactPerson", "Test Contact", "mobileNumber", "9000000013", "address", "Supplier Road"), 201);
        long supplierId = supplier.get("id").asLong();
        assertThat(supplier.get("supplierCode").asText()).startsWith("SUP-");

        JsonNode updatedSupplier = call(put("/api/suppliers/" + supplierId), admin, Map.of("supplierName", "Updated CRUD Supplier",
            "contactPerson", "Updated Contact", "mobileNumber", "9000000014", "address", "New Supplier Road"), 200);
        assertThat(updatedSupplier.get("supplierName").asText()).isEqualTo("Updated CRUD Supplier");
        assertThat(call(get("/api/suppliers?search=Updated"), admin, null, 200)
            .get("totalElements").asInt()).isEqualTo(1);

        call(delete("/api/suppliers/" + supplierId), admin, null, 204);
        call(get("/api/suppliers/" + supplierId), admin, null, 404);
        }

        @Test
        void userCrudAndDuplicateUsernameAreEnforced() throws Exception {
        String admin = token("admin", "Admin@123");
        Map<String, Object> request = Map.of("username", "cruduser", "password", "Crud@1234",
            "fullName", "CRUD User", "email", "cruduser@cssms.local", "role", "MANAGER", "active", true);
        JsonNode created = call(post("/api/users"), admin, request, 201);
        long userId = created.get("id").asLong();

        call(post("/api/users"), admin, request, 409);
        JsonNode updated = call(put("/api/users/" + userId), admin, Map.of("username", "cruduser2",
            "password", "Crud@5678", "fullName", "Updated CRUD User", "email", "cruduser2@cssms.local",
            "role", "SALES_EXECUTIVE", "active", true), 200);
        assertThat(updated.get("fullName").asText()).isEqualTo("Updated CRUD User");
        assertThat(call(get("/api/users?search=cruduser2"), admin, null, 200).get("totalElements").asInt()).isEqualTo(1);

        call(delete("/api/users/" + userId), admin, null, 204);
        call(get("/api/users/" + userId), admin, null, 404);
        }

    @Test
    void duplicateMaterialIsRejectedAndUsedMaterialCannotBeDeleted() throws Exception {
        String admin = token("admin", "Admin@123");
        call(post("/api/materials"), admin, Map.of("materialName", "dust", "unit", "TON"), 409);
        call(delete("/api/materials/" + materialId(admin, "Dust")), admin, null, 409);

        JsonNode created = call(post("/api/materials"), admin, Map.of("materialName", "Temp Stone", "unit", "TON"), 201);
        call(delete("/api/materials/" + created.get("id").asLong()), admin, null, 204);
    }

        @Test
        void purchaseOrderValidationEditCancelAndStockReversal() throws Exception {
        String admin = token("admin", "Admin@123");
        long dust = materialId(admin, "Dust");
        BigDecimal stockBefore = stock(admin, dust);
        JsonNode supplier = call(post("/api/suppliers"), admin, Map.of("supplierName", "Editable PO Supplier",
            "contactPerson", "PO Contact", "mobileNumber", "9000000015", "address", "PO Road"), 201);
        long supplierId = supplier.get("id").asLong();
        String today = LocalDate.now().toString();

        call(post("/api/purchase-orders"), admin, Map.of("poDate", "not-a-date", "supplierId", supplierId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "purchaseRatePerTon", 100))), 400);
        call(post("/api/purchase-orders"), admin, Map.of("poDate", today, "supplierId", supplierId,
            "items", List.of(Map.of("materialId", dust, "quantity", 0, "purchaseRatePerTon", 100))), 400);
        call(post("/api/purchase-orders"), admin, Map.of("poDate", today, "supplierId", supplierId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "purchaseRatePerTon", -1))), 400);
        call(post("/api/purchase-orders"), admin, Map.of("poDate", today, "supplierId", supplierId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "purchaseRatePerTon", 100,
                "gstPercentage", 101))), 400);

        JsonNode first = call(post("/api/purchase-orders"), admin, Map.of("poDate", today, "supplierId", supplierId,
            "items", List.of(Map.of("materialId", dust, "quantity", 2.5, "purchaseRatePerTon", 321.2,
                "gstPercentage", 18))), 201);
        long firstId = first.get("id").asLong();
        assertThat(first.get("poNumber").asText()).startsWith("PO-");
        assertThat(dec(first, "amount")).isEqualByComparingTo("803");
        assertThat(dec(first, "gstAmount")).isEqualByComparingTo("144.54");
        assertThat(dec(first, "totalAmount")).isEqualByComparingTo("947.54");
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.add(new BigDecimal("2.5")));

        JsonNode second = call(post("/api/purchase-orders"), admin, Map.of("poDate", today, "supplierId", supplierId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "purchaseRatePerTon", 100))), 201);
        long secondId = second.get("id").asLong();
        assertThat(second.get("poNumber").asText()).isNotEqualTo(first.get("poNumber").asText());

        JsonNode updated = call(put("/api/purchase-orders/" + firstId), admin, Map.of("poDate", today,
            "supplierId", supplierId, "items", List.of(Map.of("materialId", dust, "quantity", 3,
                "purchaseRatePerTon", 300, "gstPercentage", 5))), 200);
        assertThat(dec(updated, "totalAmount")).isEqualByComparingTo("945");
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.add(BigDecimal.valueOf(4)));
        assertThat(call(get("/api/purchase-orders?search=" + first.get("poNumber").asText()), admin, null, 200)
            .get("totalElements").asInt()).isEqualTo(1);

        call(post("/api/purchase-orders/" + firstId + "/cancel"), admin, Map.of("reason", "test"), 200);
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.add(BigDecimal.ONE));
        call(post("/api/purchase-orders/" + secondId + "/cancel"), admin, Map.of("reason", "test"), 200);
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore);
        call(post("/api/purchase-orders/" + secondId + "/cancel"), admin, Map.of("reason", "again"), 409);
        }

    // ------------------------------------------------------------------ end-to-end business flow

    @Test
    void purchaseSaleAndPaymentsKeepStockLedgersAndBalancesConsistent() throws Exception {
        String admin = token("admin", "Admin@123");
        long dust = materialId(admin, "Dust");
        BigDecimal stockBefore = stock(admin, dust);

        // --- purchase 100 t @ 300 + 5% GST = 31,500
        JsonNode supplier = call(post("/api/suppliers"), admin, Map.of("supplierName", "Flow Supplier",
                "contactPerson", "Flow", "mobileNumber", "9000000001", "address", "Somewhere"), 201);
        long supplierId = supplier.get("id").asLong();
        JsonNode po = call(post("/api/purchase-orders"), admin, Map.of(
                "poDate", LocalDate.now().toString(), "supplierId", supplierId, "vehicleNumber", "MH12FLOW1",
                "items", List.of(Map.of("materialId", dust, "quantity", 100, "purchaseRatePerTon", 300, "gstPercentage", 5))), 201);
        long poId = po.get("id").asLong();
        assertThat(po.get("poNumber").asText()).startsWith("PO-");
        assertThat(dec(po, "totalAmount")).isEqualByComparingTo("31500");
        assertThat(po.get("paymentStatus").asText()).isEqualTo("PENDING");
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.add(BigDecimal.valueOf(100)));

        // --- supplier payments: overpaying is rejected, partial then full
        JsonNode payment = call(post("/api/supplier-payments"), admin, supplierPayment(supplierId, poId, 10000), 201);
        assertThat(call(get("/api/purchase-orders/" + poId), admin, null, 200).get("paymentStatus").asText()).isEqualTo("PARTIAL_PAID");
        call(post("/api/supplier-payments"), admin, supplierPayment(supplierId, poId, 99999), 409);
        call(put("/api/purchase-orders/" + poId), admin, Map.of("poDate", LocalDate.now().toString(), "supplierId", supplierId,
                "items", List.of(Map.of("materialId", dust, "quantity", 50, "purchaseRatePerTon", 300, "gstPercentage", 5))), 409);
        call(post("/api/supplier-payments"), admin, supplierPayment(supplierId, poId, 21500), 201);
        assertThat(call(get("/api/purchase-orders/" + poId), admin, null, 200).get("paymentStatus").asText()).isEqualTo("PAID");

        JsonNode statement = call(get("/api/reports/supplier-statement?supplierId=" + supplierId), admin, null, 200);
        assertThat(summary(statement, "Closing Balance")).isEqualByComparingTo("0");
        assertThat(summary(statement, "Purchase Amount")).isEqualByComparingTo("31500");

        // --- customer with a 10,000 credit limit
        JsonNode customer = call(post("/api/customers"), admin, Map.of("customerName", "Flow Customer",
                "mobileNumber", "9000000002", "address", "Elsewhere", "creditLimit", 10000), 201);
        long customerId = customer.get("id").asLong();

        // 10 t @ 500 + 5% GST = 5,250 with 1,000 advance
        JsonNode sale = call(post("/api/sales-orders"), admin, sale(customerId, dust, 10, 1000), 201);
        long saleId = sale.get("id").asLong();
        assertThat(sale.get("invoiceNumber").asText()).startsWith("INV-");
        assertThat(dec(sale, "totalAmount")).isEqualByComparingTo("5250");
        assertThat(dec(sale, "pendingAmount")).isEqualByComparingTo("4250");
        assertThat(sale.get("paymentStatus").asText()).isEqualTo("PARTIAL_PAID");
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.add(BigDecimal.valueOf(90)));
        assertThat(dec(call(get("/api/customers/" + customerId), admin, null, 200), "outstandingAmount")).isEqualByComparingTo("4250");

        // credit limit and stock guards
        call(post("/api/sales-orders"), admin, sale(customerId, dust, 20, 0), 409);
        call(post("/api/sales-orders"), admin, sale(customerId, dust, 100000, 0), 409);

        // invoice PDF
        MvcResult invoice = mvc.perform(get("/api/sales-orders/" + saleId + "/invoice").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andReturn();
        assertThat(invoice.getResponse().getStatus()).isEqualTo(200);
        assertThat(new String(invoice.getResponse().getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");

        // collect the rest
        call(post("/api/customer-payments"), admin, customerPayment(customerId, saleId, 99999), 409);
        JsonNode receipt = call(post("/api/customer-payments"), admin, customerPayment(customerId, saleId, 4250), 201);
        JsonNode paidSale = call(get("/api/sales-orders/" + saleId), admin, null, 200);
        assertThat(paidSale.get("paymentStatus").asText()).isEqualTo("PAID");
        assertThat(dec(call(get("/api/customers/" + customerId), admin, null, 200), "outstandingAmount")).isEqualByComparingTo("0");

        JsonNode customerStatement = call(get("/api/reports/customer-statement?customerId=" + customerId), admin, null, 200);
        assertThat(summary(customerStatement, "Total Sales")).isEqualByComparingTo("5250");
        assertThat(summary(customerStatement, "Total Collection")).isEqualByComparingTo("5250");
        assertThat(summary(customerStatement, "Closing Balance")).isEqualByComparingTo("0");

        // cancellation is blocked while a payment exists, allowed after the payment is removed
        call(post("/api/sales-orders/" + saleId + "/cancel"), admin, Map.of("reason", "test"), 409);
        call(delete("/api/customer-payments/" + receipt.get("id").asLong()), admin, null, 204);
        call(post("/api/sales-orders/" + saleId + "/cancel"), admin, Map.of("reason", "test"), 200);
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.add(BigDecimal.valueOf(100)));
        assertThat(dec(call(get("/api/customers/" + customerId), admin, null, 200), "outstandingAmount")).isEqualByComparingTo("0");

        // the purchase cannot be cancelled while it has payments, and the customer can now be deleted only if unused
        call(post("/api/purchase-orders/" + poId + "/cancel"), admin, Map.of("reason", "x"), 409);
        call(delete("/api/supplier-payments/" + payment.get("id").asLong()), admin, null, 204);
        call(delete("/api/customers/" + customerId), admin, null, 409);
    }

    @Test
    void salesOrderUpdateReappliesStockAndBalances() throws Exception {
        String admin = token("admin", "Admin@123");
        long dust = materialId(admin, "Dust");
        JsonNode customer = call(post("/api/customers"), admin, Map.of("customerName", "Update Customer",
                "mobileNumber", "9000000003", "address", "Addr", "creditLimit", 0), 201);
        long customerId = customer.get("id").asLong();
        BigDecimal before = stock(admin, dust);

        JsonNode sale = call(post("/api/sales-orders"), admin, sale(customerId, dust, 10, 0), 201);
        JsonNode updated = call(put("/api/sales-orders/" + sale.get("id").asLong()), admin, sale(customerId, dust, 4, 500), 200);
        assertThat(dec(updated, "totalAmount")).isEqualByComparingTo("2100");
        assertThat(dec(updated, "pendingAmount")).isEqualByComparingTo("1600");
        assertThat(updated.get("salesOrderNumber").asText()).isEqualTo(sale.get("salesOrderNumber").asText());
        assertThat(stock(admin, dust)).isEqualByComparingTo(before.subtract(BigDecimal.valueOf(4)));
        assertThat(dec(call(get("/api/customers/" + customerId), admin, null, 200), "outstandingAmount")).isEqualByComparingTo("1600");

        JsonNode pending = call(get("/api/sales-orders?customerId=" + customerId + "&pendingOnly=true"), admin, null, 200);
        assertThat(pending.get("totalElements").asInt()).isEqualTo(1);
    }

        @Test
        void salesDiscountTaxAndLineValidation() throws Exception {
        String admin = token("admin", "Admin@123");
        long dust = materialId(admin, "Dust");
        BigDecimal stockBefore = stock(admin, dust);
        JsonNode customer = call(post("/api/customers"), admin, Map.of("customerName", "Discount Customer",
            "mobileNumber", "9000000016", "address", "Discount Road", "creditLimit", 0), 201);
        long customerId = customer.get("id").asLong();
        String today = LocalDate.now().toString();

        call(post("/api/sales-orders"), admin, Map.of("orderDate", today, "customerId", customerId,
            "items", List.of(Map.of("materialId", dust, "quantity", 0, "rate", 500))), 400);
        call(post("/api/sales-orders"), admin, Map.of("orderDate", today, "customerId", customerId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "rate", -1))), 400);
        call(post("/api/sales-orders"), admin, Map.of("orderDate", today, "customerId", customerId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "rate", 500, "discount", -1))), 400);
        call(post("/api/sales-orders"), admin, Map.of("orderDate", today, "customerId", customerId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "rate", 500, "gstPercentage", 101))), 400);
        call(post("/api/sales-orders"), admin, Map.of("orderDate", today, "customerId", customerId,
            "items", List.of(Map.of("materialId", dust, "quantity", 1, "rate", 500, "discount", 501))), 400);

        JsonNode sale = call(post("/api/sales-orders"), admin, Map.of("orderDate", today, "customerId", customerId,
            "advanceAmount", 0, "items", List.of(Map.of("materialId", dust, "quantity", 2,
                "rate", 500, "discount", 50, "gstPercentage", 5))), 201);
        assertThat(dec(sale, "subTotal")).isEqualByComparingTo("1000");
        assertThat(dec(sale, "discountTotal")).isEqualByComparingTo("50");
        assertThat(dec(sale, "gstAmount")).isEqualByComparingTo("47.5");
        assertThat(dec(sale, "totalAmount")).isEqualByComparingTo("997.5");
        assertThat(dec(sale.get("items").get(0), "amount")).isEqualByComparingTo("950");
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore.subtract(BigDecimal.valueOf(2)));

        call(post("/api/sales-orders/" + sale.get("id").asLong() + "/cancel"), admin, Map.of("reason", "test"), 200);
        assertThat(stock(admin, dust)).isEqualByComparingTo(stockBefore);
        }

    // ------------------------------------------------------------------ reports, dashboard, export

    @Test
    void demoDataIsConsistentAndAllReportsWork() throws Exception {
        String admin = token("admin", "Admin@123");

        JsonNode dashboard = call(get("/api/dashboard"), admin, null, 200);
        assertThat(dashboard.get("cards").has("monthlyProfit")).isTrue();
        assertThat(dashboard.get("salesTrend").size()).isEqualTo(6);
        assertThat(dashboard.get("topCustomers").size()).isGreaterThan(0);
        assertThat(dashboard.get("materialWiseSales").size()).isGreaterThan(0);

        for (String key : List.of("stock-current", "stock-material-wise", "stock-movement", "purchase-statement",
                "sales-statement", "material-profit", "receivable", "payable", "outstanding-balance",
                "customer-payments", "supplier-payments", "expense-report")) {
            JsonNode report = call(get("/api/reports/" + key), admin, null, 200);
            assertThat(report.get("columns").size()).as(key).isGreaterThan(0);
            assertThat(report.get("rows").size()).as(key).isGreaterThan(0);
        }

        JsonNode pnl = call(get("/api/reports/profit-loss?fromDate=" + LocalDate.now().minusYears(1) + "&toDate=" + LocalDate.now()),
                admin, null, 200);
        assertThat(pnl.get("summary").size()).isEqualTo(4);
        call(get("/api/reports/profit-loss"), admin, null, 400);
        call(get("/api/reports/customer-statement"), admin, null, 400);
        call(get("/api/reports/unknown-report"), admin, null, 404);
        call(get("/api/reports/sales-statement?fromDate=not-a-date"), admin, null, 400);

        // current stock must equal purchased - sold for every material
        JsonNode inventory = call(get("/api/inventory"), admin, null, 200);
        for (JsonNode row : inventory) {
            assertThat(dec(row, "currentStock")).isEqualByComparingTo(dec(row, "purchasedStock").subtract(dec(row, "soldStock")));
        }
    }

    @Test
    void reportsExportToExcelAndPdf() throws Exception {
        String admin = token("admin", "Admin@123");
        MvcResult xlsx = mvc.perform(get("/api/reports/sales-statement/export?format=xlsx").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)).andReturn();
        assertThat(xlsx.getResponse().getStatus()).isEqualTo(200);
        assertThat(xlsx.getResponse().getContentAsByteArray()).startsWith((byte) 'P', (byte) 'K');

        MvcResult pdf = mvc.perform(get("/api/reports/purchase-statement/export?format=pdf").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)).andReturn();
        assertThat(pdf.getResponse().getStatus()).isEqualTo(200);
        assertThat(new String(pdf.getResponse().getContentAsByteArray(), 0, 4)).isEqualTo("%PDF");

        call(get("/api/reports/sales-statement/export?format=doc"), admin, null, 400);
    }

        @Test
        void expenseCrudAndAmountValidation() throws Exception {
        String admin = token("admin", "Admin@123");
        call(post("/api/expenses"), admin, Map.of("expenseDate", LocalDate.now().toString(),
            "expenseType", "FUEL", "amount", 0, "description", "Invalid expense"), 400);

        JsonNode created = call(post("/api/expenses"), admin, Map.of("expenseDate", LocalDate.now().toString(),
            "expenseType", "FUEL", "amount", 75, "description", "CRUD expense"), 201);
        long id = created.get("id").asLong();
        JsonNode updated = call(put("/api/expenses/" + id), admin, Map.of("expenseDate", LocalDate.now().toString(),
            "expenseType", "VEHICLE_MAINTENANCE", "amount", 120, "description", "Updated CRUD expense"), 200);
        assertThat(dec(updated, "amount")).isEqualByComparingTo("120");
        assertThat(call(get("/api/expenses?search=Updated"), admin, null, 200)
            .get("totalElements").asInt()).isEqualTo(1);

        call(delete("/api/expenses/" + id), admin, null, 204);
        call(get("/api/expenses/" + id), admin, null, 404);
        }

    @Test
    void expenseAttachmentUploadDownloadAndTypeValidation() throws Exception {
        String admin = token("admin", "Admin@123");
        JsonNode expense = call(post("/api/expenses"), admin, Map.of("expenseDate", LocalDate.now().toString(),
                "expenseType", "FUEL", "amount", 1234.5, "description", "Attachment test"), 201);
        long id = expense.get("id").asLong();

        MockMultipartFile bad = new MockMultipartFile("file", "script.exe", "application/octet-stream", "x".getBytes());
        assertThat(mvc.perform(MockMvcRequestBuilders.multipart("/api/expenses/" + id + "/attachment").file(bad)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)).andReturn().getResponse().getStatus()).isEqualTo(400);

        MockMultipartFile good = new MockMultipartFile("file", "bill.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        MvcResult uploaded = mvc.perform(MockMvcRequestBuilders.multipart("/api/expenses/" + id + "/attachment").file(good)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)).andReturn();
        assertThat(uploaded.getResponse().getStatus()).isEqualTo(200);
        assertThat(mapper.readTree(uploaded.getResponse().getContentAsString()).get("hasAttachment").asBoolean()).isTrue();

        MvcResult download = mvc.perform(get("/api/expenses/" + id + "/attachment").header(HttpHeaders.AUTHORIZATION, "Bearer " + admin)).andReturn();
        assertThat(download.getResponse().getStatus()).isEqualTo(200);
        assertThat(download.getResponse().getContentAsString()).startsWith("%PDF-1.4");

        JsonNode removed = call(delete("/api/expenses/" + id + "/attachment"), admin, null, 200);
        assertThat(removed.get("hasAttachment").asBoolean()).isFalse();
    }

    // ------------------------------------------------------------------ helpers

    private String token(String username, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("username", username, "password", password), 200)
                .get("accessToken").asText();
    }

    private JsonNode call(MockHttpServletRequestBuilder request, String token, Object body, int expectedStatus) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
        }
        MvcResult result = mvc.perform(request).andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(result.getResponse().getStatus()).as("%s %s -> %s", result.getRequest().getMethod(),
                result.getRequest().getRequestURI(), content).isEqualTo(expectedStatus);
        return content.isBlank() ? mapper.nullNode() : mapper.readTree(content);
    }

    private long materialId(String token, String name) throws Exception {
        JsonNode page = call(get("/api/materials?search=" + name.replace(" ", "%20")), token, null, 200);
        for (JsonNode material : page.get("content")) {
            if (material.get("materialName").asText().equalsIgnoreCase(name)) {
                return material.get("id").asLong();
            }
        }
        throw new AssertionError("Material not found: " + name);
    }

    private BigDecimal stock(String token, long materialId) throws Exception {
        for (JsonNode row : call(get("/api/inventory"), token, null, 200)) {
            if (row.get("materialId").asLong() == materialId) {
                return dec(row, "currentStock");
            }
        }
        throw new AssertionError("No inventory row for material " + materialId);
    }

    private static BigDecimal dec(JsonNode node, String field) {
        return node.get(field).decimalValue();
    }

    private static BigDecimal summary(JsonNode report, String label) {
        for (JsonNode s : report.get("summary")) {
            if (s.get("label").asText().equals(label)) {
                return s.get("value").decimalValue();
            }
        }
        throw new AssertionError("Summary not found: " + label);
    }

    private static Map<String, Object> employee(String name) {
        return Map.of("fullName", name, "mobileNumber", "9000000009", "joiningDate", "2024-01-15", "salary", 20000,
                "designation", "Operator");
    }

    private static Map<String, Object> sale(long customerId, long materialId, int qty, int advance) {
        return Map.of("orderDate", LocalDate.now().toString(), "customerId", customerId, "vehicleNumber", "MH12SALE1",
                "advanceAmount", advance, "advancePaymentMode", "CASH",
                "items", List.of(Map.of("materialId", materialId, "quantity", qty, "rate", 500, "discount", 0, "gstPercentage", 5)));
    }

    private static Map<String, Object> customerPayment(long customerId, long saleId, int amount) {
        return Map.of("paymentDate", LocalDate.now().toString(), "customerId", customerId, "salesOrderId", saleId,
                "paymentMode", "UPI", "amountReceived", amount, "referenceNumber", "REF" + amount);
    }

    private static Map<String, Object> supplierPayment(long supplierId, long poId, int amount) {
        return Map.of("paymentDate", LocalDate.now().toString(), "supplierId", supplierId, "purchaseOrderId", poId,
                "paymentMode", "BANK_TRANSFER", "amountPaid", amount, "referenceNumber", "SREF" + amount);
    }
}
