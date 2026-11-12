# CSSMS Functional Test Report

**Run date:** 2026-10-29  
**Result:** 18 automated backend integration tests passed; frontend production build passed.  
**Backend test environment:** Spring Boot test profile with isolated embedded PostgreSQL 14.10. The test suite did not modify the shared demo database.  
**Browser scope:** Authenticated UI route checks, form opening, selected master/user/expense CRUD, purchase and sales lifecycles, payment reversal, statements, dashboard reconciliation, and P&L exports. Test records were uniquely tagged; deletable QA records were removed. The transaction-linked QA customer remains inactive, and canceled QA purchase/sales orders remain as audit records. Stock and balances were restored to their pre-test values.

## Authentication

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| AUTH-01 | Login with valid admin credentials | PASS | `loginRefreshRotationAndLogout`; browser login also succeeded. |
| AUTH-02 | Reject invalid credentials, missing token, and malformed token | PASS | `badCredentialsAndMissingTokenAreRejected`. |
| AUTH-03 | Refresh token rotates; replay revokes the token family | PASS | `loginRefreshRotationAndLogout`. |
| AUTH-04 | Logout revokes refresh token | PASS | `logoutRevokesRefreshToken`; logout request uses authenticated access token. |
| AUTH-05 | Password policy, reset token, and one-time reset behavior | PASS | `changePasswordAndProfile`. |
| AUTH-06 | Role-based endpoint access | PASS | `roleMatrixIsEnforced` covers admin, manager, and sales permissions. |
| AUTH-07 | Access-token/session expiry behavior | NOT TESTED | Expiry duration is configured, but expiry over time was not exercised. |
| AUTH-08 | Remember Me | NOT AVAILABLE | No Remember Me feature was found in the current app/API. |

## Dashboard

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| DASH-01 | Dashboard cards, trends, top customers, material sales data | PASS | `demoDataIsConsistentAndAllReportsWork`; six trend periods and expected dashboard sections are checked. |
| DASH-02 | Dashboard renders in browser | PASS | Browser smoke showed nine KPI cards and charts. |
| DASH-03 | Verify each KPI against independently calculated source totals | PARTIAL | Values render, but every card is not reconciled against source transactions. |
| DASH-04 | Pending-delivery summary | NOT AVAILABLE | No delivery module/data model is present. |

## Purchase Orders

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| PO-01 | Create PO; generate PO number; calculate amount, GST, and total | PASS | `purchaseSaleAndPaymentsKeepStockLedgersAndBalancesConsistent`; also `purchaseOrderValidationEditCancelAndStockReversal`. |
| PO-02 | PO number uniqueness for separate orders | PASS | `purchaseOrderValidationEditCancelAndStockReversal` compares generated numbers. |
| PO-03 | Prevent duplicate business orders with identical details | NOT TESTED | Unique generated numbers are checked; duplicate payload prevention is not established. |
| PO-04 | Edit PO and recalculate totals/stock | PASS | `purchaseOrderValidationEditCancelAndStockReversal`; paid PO edit is rejected in the purchase/payment flow. |
| PO-05 | Cancel PO and reverse stock; reject repeated cancellation | PASS | `purchaseOrderValidationEditCancelAndStockReversal`. |
| PO-06 | Date format, quantity, rate, and GST boundaries | PASS | Malformed date, zero quantity, negative rate, and GST above 100 are rejected. Date business rules such as past/future limits are not specified/tested. |
| PO-07 | View and search PO | PASS | PO detail retrieval is exercised in transaction flows; search by PO number is checked. |
| PO-08 | All PO filters | PARTIAL | Search and payment/status filters are exercised in suite scenarios; full filter combinations are not covered. |
| PO-09 | PO approval workflow | NOT AVAILABLE | No approval endpoint or workflow was found. Cancellation is supported instead. |
| PO-10 | Hard-delete PO | NOT AVAILABLE | There is no delete endpoint; PO cancellation is the supported removal workflow and is tested above. |

## Sales

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| SALE-01 | Create sale with customer/material, quantity, rate, discount, GST, and final total | PASS | `salesDiscountTaxAndLineValidation` checks a discounted 5% GST calculation. |
| SALE-02 | Reject invalid quantity, negative rate/discount, excessive GST, and discount above line amount | PASS | `salesDiscountTaxAndLineValidation`. |
| SALE-03 | Deduct stock; enforce stock and customer credit limits | PASS | `purchaseSaleAndPaymentsKeepStockLedgersAndBalancesConsistent`. |
| SALE-04 | Edit sale and recalculate balance/stock | PASS | `salesOrderUpdateReappliesStockAndBalances`. |
| SALE-05 | Cancel sale and restore stock/balance | PASS | Transaction flow and `salesDiscountTaxAndLineValidation`. Cancellation after payment is blocked until payment reversal. |
| SALE-06 | Invoice generation | PASS | PDF signature/content checked in the transaction flow. |
| SALE-07 | Sales return | NOT AVAILABLE | No sales-return workflow/API was found. |

## Customers

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| CUST-01 | Create, update, search, and delete unused customer | PASS | `customerAndSupplierCrudSearchAndValidation`. |
| CUST-02 | Validate mobile, GST, and required address | PASS | Invalid mobile/GST/address produce field errors. |
| CUST-03 | Prevent duplicate customers | FAIL | UI accepted two customers with the same mobile number. Both temporary records were deleted. `CustomerService` has no duplicate check; the uniqueness key needs a business decision. |
| CUST-04 | Customer ledger and outstanding balance | PASS | `purchaseSaleAndPaymentsKeepStockLedgersAndBalancesConsistent` checks customer statement totals and closing balance. |

## Suppliers

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| SUP-01 | Create, update, search, and delete unused supplier | PASS | `customerAndSupplierCrudSearchAndValidation`. |
| SUP-02 | Validate supplier contact, mobile, GST, and address | PASS | `validationErrorsReturnFieldMessages`. |
| SUP-03 | Supplier payments, payment limits, and ledger balance | PASS | Purchase/payment flow checks partial/full payments, overpayment rejection, and supplier statement totals. |

## Inventory

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| INV-01 | Purchase adds stock; sale deducts stock; cancellation reverses movement | PASS | PO and sales lifecycle tests assert exact stock deltas. |
| INV-02 | Prevent negative stock | PASS | Oversized sale is rejected in `purchaseSaleAndPaymentsKeepStockLedgersAndBalancesConsistent`. |
| INV-03 | Current stock equals purchased minus sold | PASS | `demoDataIsConsistentAndAllReportsWork` checks each material. |
| INV-04 | Opening-stock entry | NOT AVAILABLE | No opening-stock operation was found in the documented inventory API. |
| INV-05 | Deliberate inventory-mismatch recovery | PARTIAL | Reconciliation invariant is checked; no separate mismatch-repair workflow exists in the API. |

## Vehicles and Deliveries

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| VEH-01 | Vehicle registration, driver assignment, status, duplicate vehicle, allocation conflicts | NOT AVAILABLE | Vehicle number is a text field on orders; there is no vehicle/driver management module. |
| DEL-01 | Create, complete, cancel, partial delivery, delivered-vs-invoiced quantity | NOT AVAILABLE | No delivery module or delivery endpoints were found. |

## Accounts, Payments, and Reports

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| ACC-01 | Customer statement and running/closing balance | PASS | `purchaseSaleAndPaymentsKeepStockLedgersAndBalancesConsistent`. |
| ACC-02 | Supplier statement and closing balance | PASS | Purchase/payment lifecycle test. |
| ACC-03 | Receivable/payable reports | PASS | `demoDataIsConsistentAndAllReportsWork`. |
| ACC-04 | Remaining Accounts requirements | BLOCKED | The request supplied to QA ended after “Accounts Module Validate: Customer…”. The rest of the acceptance criteria were not provided. |
| PAY-01 | Customer/supplier payment creation, overpayment rejection, reversal | PASS | Purchase/sales lifecycle test. |
| RPT-01 | Report endpoints and required date parameters | PASS | `demoDataIsConsistentAndAllReportsWork`. |
| RPT-02 | PDF/Excel exports and invalid format rejection | PASS | `reportsExportToExcelAndPdf`. |

## Other Implemented Modules

| ID | Test case | Result | Evidence / notes |
|---|---|---|---|
| USER-01 | User create, update, search, delete, duplicate username rejection | PASS | `userCrudAndDuplicateUsernameAreEnforced`. |
| EMP-01 | Employee create, update, search, soft delete | PASS | `employeeCrudWithSoftDelete`. |
| MAT-01 | Duplicate material rejection and used-material delete protection | PASS | `duplicateMaterialIsRejectedAndUsedMaterialCannotBeDeleted`. |
| EXP-01 | Expense create, update, search, delete, amount validation | PASS | `expenseCrudAndAmountValidation`. |
| EXP-02 | Attachment upload/download/removal and invalid file type rejection | PASS | `expenseAttachmentUploadDownloadAndTypeValidation`. |

## Browser Functional Checks

**Environment:** Authenticated admin session at `http://localhost:5174`. CRUD/workflow tests used QA-tagged data; transaction-linked documents were canceled and their payments reversed.

| ID | Page / action | Result | Observed behavior |
|---|---|---|---|
| UI-01 | Dashboard `/` | PASS | Dashboard heading, nine KPI cards, trends, and customer/supplier summaries rendered. |
| UI-02 | Employees `/employees` | PASS | Employee list heading and filters rendered. |
| UI-03 | Suppliers `/suppliers` | PASS | Supplier list heading and filters rendered. |
| UI-04 | Customers `/customers` | PASS | Customer list heading and filters rendered. |
| UI-05 | Materials `/materials` | PASS | Material list heading rendered. |
| UI-06 | Purchase Orders `/purchase-orders` | PASS | PO list heading and filters rendered. |
| UI-07 | Inventory `/inventory` | PASS | Inventory heading and stock rows rendered. |
| UI-08 | Sales Orders `/sales-orders` | PASS | Sales list heading and filters rendered. |
| UI-09 | Customer Payments `/customer-payments` | PASS | Payment list heading and filters rendered. |
| UI-10 | Supplier Payments `/supplier-payments` | PASS | Payment list heading and filters rendered. |
| UI-11 | Expenses `/expenses` | PASS | Expense list heading and filters rendered. |
| UI-12 | Customer Statement `/accounts/customer-statement` | PASS | Statement heading and customer/date controls rendered. |
| UI-13 | Supplier Statement `/accounts/supplier-statement` | PASS | Statement heading and supplier/date controls rendered. |
| UI-14 | Receivables `/accounts/receivables` | PASS | Receivable report rendered. |
| UI-15 | Payables `/accounts/payables` | PASS | Payable report and export controls rendered. |
| UI-16 | Purchase Statement `/reports/purchase-statement` | PASS | Report heading and filters rendered. |
| UI-17 | Sales Statement `/reports/sales-statement` | PASS | Report heading and filters rendered. |
| UI-18 | Material Profit `/reports/material-profit` | PASS | Report heading and filters rendered. |
| UI-19 | Current Stock `/reports/stock-current` | PASS | Report data and export controls rendered. |
| UI-20 | Material-wise Stock `/reports/stock-material-wise` | PASS | Report heading and material filter rendered. |
| UI-21 | Stock Movement `/reports/stock-movement` | PASS | Report heading and filters rendered. |
| UI-22 | Customer Payment Report `/reports/customer-payments` | PASS | Report heading and filters rendered. |
| UI-23 | Supplier Payment Report `/reports/supplier-payments` | PASS | Report heading and filters rendered. |
| UI-24 | Expense Report `/reports/expense-report` | PASS | Report heading and filters rendered. |
| UI-25 | Profit & Loss `/reports/profit-loss` | PASS | Period controls and report generation controls rendered. |
| UI-26 | Users `/users` | PASS | User list heading and role filter rendered. |
| UI-27 | Profile `/profile` | PASS | Profile fields rendered. |
| UI-28 | Change Password `/change-password` | PASS | Password fields rendered. |
| UI-29 | Add Employee | PASS | Dialog opened with 7 accessible fields; closed without saving. |
| UI-30 | Add Supplier | PASS | Dialog opened with 8 accessible fields; closed without saving. |
| UI-31 | Add Customer | PASS | Dialog opened with 7 accessible fields; closed without saving. |
| UI-32 | Add Material | PASS | Dialog opened with 3 accessible fields; closed without saving. |
| UI-33 | New Purchase Order | PASS | Navigation to PO form succeeded; 5 accessible fields rendered; no submission. |
| UI-34 | New Sales Order | PASS | Navigation to sales form succeeded; 7 accessible fields rendered; no submission. |
| UI-35 | Receive Customer Payment | PASS | Dialog opened with 6 accessible fields; closed without saving. |
| UI-36 | Make Supplier Payment | PASS | Dialog opened with 6 accessible fields; closed without saving. |
| UI-37 | Add Expense | PASS | Dialog opened with 3 accessible fields; closed without saving. |
| UI-38 | Add User | PASS | Dialog opened with 6 accessible fields; closed without saving. |
| UI-39 | Employee create, update, delete | PASS | QA employee created, updated, then deleted. |
| UI-40 | Customer create, update, delete | PASS | Valid GST/mobile customer created, updated, and deleted before use in transactions. |
| UI-41 | Duplicate customer mobile | FAIL | Two QA customers sharing one mobile were both accepted; both were then deleted. |
| UI-42 | Supplier create, update, delete | PASS | QA supplier created, updated, and deleted. |
| UI-43 | Material create, update, delete | PASS | QA material created, updated description confirmed in table, then deleted. |
| UI-44 | Expense create, update, delete | PASS | QA expense created and amount updated; deletion confirmed after list refresh. |
| UI-45 | User create, update, delete | PASS | QA user created, updated, and deleted. |
| UI-46 | PO create and GST total | PASS | Created `PO-202610-00008`; 2 tons at ₹100 plus 5% GST rendered ₹210. |
| UI-47 | PO edit and recalculation | PASS | Quantity edited to 3; PO total updated to ₹315. |
| UI-48 | Supplier partial payment, reversal, PO cancellation | PASS | ₹100 payment reversed; PO returned to unpaid balance ₹315, then canceled. |
| UI-49 | Sales create, discount/GST, invoice | PASS | Created `INV-202610-00009`; ₹1,000 gross less ₹50 discount plus 5% GST = ₹997.50. Invoice endpoint returned HTTP 200. |
| UI-50 | Sales edit and recalculation | PASS | Quantity edited to 3; total updated to ₹1,522.50. |
| UI-51 | Customer payment and reversal | PASS | ₹100 receipt recorded and deleted; pending amount restored. |
| UI-52 | Sales cancellation and inventory/balance reversal | PASS | Sale canceled; Dust returned to 160 tons and customer outstanding returned to ₹0. Customer was marked inactive because the canceled order references it. |
| UI-53 | Customer statement for QA customer | PASS | Generated statement showed ₹0 sales, ₹0 collection, and ₹0 closing balance after cancellation. |
| UI-54 | Supplier statement | PASS | Statement rendered with closing balance ₹2,03,640; canceled QA PO/payment were absent. |
| UI-55 | Dashboard after transaction reversal | PASS | Metrics returned to baseline: inventory 951, today’s purchase ₹1,010, receivables ₹2,18,780, payables ₹4,49,865. |
| UI-56 | Inventory card/table view | PASS | Both views rendered; Dust shows 200 purchased, 40 sold, 160 current. |
| UI-57 | Profit & Loss generation and exports | PASS | October report generated; PDF and Excel export endpoints both returned HTTP 200. |
| UI-58 | Profile update with empty mobile | FAIL | Existing admin profile has no mobile; UI blocked saving another profile change with “Mobile number is required.” No profile change was saved. |
| UI-59 | Access-token expiry during browser session | PARTIAL | After prolonged session, API requests logged 401s; manual logout/re-login restored the UI. Automatic refresh behavior was not conclusively verified. |

## Overall Limitations

- Backend functional tests use MockMvc against the full Spring application and isolated PostgreSQL. Browser tests exercised the listed workflows, but not every filter combination, every report form, or every invalid-input path through the UI.
- Performance/load testing and a comprehensive security assessment were not run. Security coverage here means authentication, token handling, validation, and role authorization tests.
- No separate vehicle, delivery, PO approval, sales return, Remember Me, or opening-stock feature was available to test.
- The duplicate-customer requirement is unmet in current service logic. The desired uniqueness key needs a business decision.
