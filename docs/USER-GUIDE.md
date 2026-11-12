# Stone / Crusher Order Supply Management System
## End-User Guide

**Version:** 1.0.0  
**Prepared by:** [Prepared By]  
**Date:** 2026-10-29  
**Company:** [Company Name]  
**Scope:** Reflects the features available in the current application build.

> **Important scope note:** This guide describes features present in the current CSSMS application. The repository provides PostgreSQL support, roles Admin/Manager/Sales Executive, and no separate Vehicle, Delivery, or standalone Settings modules. These requested capabilities are identified as unavailable rather than described as working features.

<!-- pagebreak -->

## Table of Contents

1. Introduction
2. System Requirements
3. Getting Started
4. Dashboard
5. Purchase Orders
6. Sales Orders
7. Customer Management
8. Master Data: Suppliers, Employees, Materials
9. Inventory Management
10. Vehicle Management
11. Delivery Management
12. Accounts and Payments
13. Profit and Loss
14. Reports
15. Search, Filters, Sorting, and Pagination
16. Export and Printing
17. Notifications and Validation
18. User Roles and Permissions
19. Frequently Asked Questions
20. Troubleshooting
21. Glossary
22. Best Practices
23. Appendix: Rules and Errors
24. Support and Change Notes

## 1. Introduction

CSSMS helps a stone and crusher-material supply business record purchases, stock, customer sales, invoices, payments, expenses, account statements, and management reports. Users work in a browser-based React application; business rules and records are processed by a Spring Boot API and stored in PostgreSQL.

### 1.1 Key benefits

- One record of purchase and sales activity linked to materials, suppliers, customers, and payments.
- Stock increases when an active purchase order is created and decreases when an active sales order is created.
- Customer receivables, supplier payables, and statements are updated from invoices and payments.
- PDF/Excel report exports and PDF sales invoices are available.
- Role-based menus and API permissions limit access by account role.

### 1.2 Main modules in this build

Dashboard; Employees; Suppliers; Customers; Materials; Purchase Orders; Inventory; Sales Orders; Customer Payments; Supplier Payments; Expenses and expense attachments; Customer/Supplier Statements; Receivables; Payables; Reports; Users; Profile; Change Password.

Vehicle numbers and delivery addresses can be entered on transaction forms, but there is no fleet/driver or delivery-tracking module. There is no PO approval, sales-return, manual opening-stock, or stock-adjustment workflow.

> [Screenshot placeholder: Application navigation showing the available modules]

## 2. System Requirements

### 2.1 Supported browsers

Use a current supported version of Google Chrome, Microsoft Edge, or Mozilla Firefox. Enable JavaScript and allow downloads from the application origin for PDF and Excel exports.

### 2.2 Screen and device

The responsive interface is tested from 320px mobile width through 1920px desktop width. A 1280x720 or larger display is recommended for purchase/sales line-item forms and wide reports. On phones, wide tables scroll within their table container; rotate the device or use a larger screen when editing multiple line items.

### 2.3 Network requirements

The browser must reach the company-provided CSSMS URL. The application requires an active connection to its API while using the system. For production, connect through the company network/VPN and the configured HTTPS address. Do not use a development or demo URL for live business records.

### 2.4 Supported database

The supplied deployment and migrations target PostgreSQL. MySQL is not configured in this build.

## 3. Getting Started

### 3.1 Open the application

Use the URL provided by your administrator. The sign-in page is normally `<application-url>/login`. For local development only, Vite commonly uses `http://localhost:5173`; Docker deployment uses the configured web port. Production should use the company HTTPS URL.

> [Screenshot placeholder: Login screen]

### 3.2 Sign in

1. Open the application URL.
2. Enter your assigned username and password.
3. Select **Sign In**.
4. If successful, the Dashboard opens. Your menu items depend on your role.
5. If sign-in fails, check spelling/caps lock and contact an administrator; do not share your password.

| Field | Description |
|---|---|
| Username | Assigned account name. |
| Password | Secret associated with the account; never share it. |

Demo accounts (`admin`, `manager`, `sales`) are for a non-production demo profile only. The production administrator password is set during deployment and should be changed immediately.

### 3.3 Forgot password

1. On the login screen, select **Forgot password?**.
2. Enter the email address associated with the account and submit.
3. Follow the reset instructions supplied by your administrator.

In this build, email/SMS delivery is an integration hook. A reset token may only be shown when a development-only setting explicitly enables it. If no reset message arrives, ask an administrator to verify the configured delivery service; do not send reset tokens over an insecure channel.

When you receive an authorized reset link, enter a New Password and Confirm Password. The link must contain a valid one-time token; the password follows the same 8–100 character uppercase/lowercase/digit rule.

> [Screenshot placeholder: Forgot password dialog]

### 3.4 Change password

1. Open the profile menu in the top-right corner.
2. Select **Change Password**.
3. Enter your current password, a new password, and confirmation.
4. Select **Change Password**.

Passwords must be 8–100 characters and contain at least one uppercase letter, one lowercase letter, and one digit. Use a unique password. Changing the password revokes refresh sessions, so sign in again if prompted.

| Field | Description |
|---|---|
| Current Password | Confirms your identity. |
| New Password | Must follow the password rule above and differ from the current password. |
| Confirm New Password | Must match New Password. |

### 3.5 Update your profile

Open the profile menu, choose **Profile**, edit the allowed fields, and choose **Save Changes**. Username and role are displayed but managed through Users by Admin.

| Field | Description |
|---|---|
| Full Name | Required; up to 120 characters. |
| Email | Required; valid email address. |
| Mobile Number | Optional in the API; the UI may require a value to save profile changes. |

### 3.6 Sign out

Open the profile menu and select **Logout**. The app returns to the login page. Always sign out on shared devices.

> [Screenshot placeholder: Profile menu with Logout]

## 4. Dashboard

The Dashboard is a business overview. It shows the current day/month and stock figures based on the server timezone (`APP_TIMEZONE`, default `Asia/Kolkata`). It does not show Total Orders, Total Purchase Orders, Total Customers, Total Suppliers, or Pending Deliveries cards in this build.

| Card | Purpose and calculation | How to use |
|---|---|---|
| Today's Purchase | Sum of purchase-order totals dated today. | Compare today's supplier purchasing activity. |
| Today's Sales | Sum of sales-order totals dated today. | Monitor today's invoiced sales. |
| Today's Collection | Sum of customer payments dated today. | Monitor cash received from customers. |
| Today's Supplier Payment | Sum of supplier payments dated today. | Monitor supplier disbursements. |
| Current Inventory | Sum of current stock quantities across materials; this is quantity, not inventory value. | Review available stock and investigate low-stock materials. |
| Receivables | Outstanding pending amounts on sales orders. | Follow up customer balances; use Accounts for detail. |
| Payables | Outstanding balances on purchase orders. | Plan supplier payments; use Accounts for detail. |
| Monthly Profit | Positive net profit for the current month; zero when net result is not positive. | Review the monthly trend; use Profit & Loss for details. |
| Monthly Loss | Absolute value of a negative current-month net result; zero when net result is not negative. | Review expense/purchase impact; open Profit & Loss. |

Charts show purchase, sales, and profit for the latest six months, plus top customers, top suppliers, and material-wise sales. Top lists are limited to five entries.

> [Screenshot placeholder: Dashboard cards and six-month charts]

## 5. Purchase Orders

### 5.1 Purpose and navigation

Purchase Orders record material purchased from a supplier. Select **Purchase Orders** from the sidebar, then **New Purchase Order** to create one.

### 5.2 Workflow

```text
Select date and supplier -> Add material lines -> Review GST/total -> Create PO
      -> Stock increases and supplier payable is recorded
      -> Record supplier payment when paid
      -> If correction is needed, reverse payments first, then edit/cancel where allowed
```

### 5.3 Create a purchase order

1. Select **New Purchase Order**.
2. Confirm the PO Date and select an active Supplier.
3. Optionally enter Vehicle Number and Remarks.
4. In Items, select a Material and enter Quantity (tons), Rate per ton, and GST %.
5. Select **Add Item** to add another material line. Remove unused lines with **Remove item**.
6. Review line amounts, GST, Total Quantity, Amount, GST, and Grand Total.
7. Select **Create Purchase Order**. The system assigns a PO number.

> [Screenshot placeholder: New Purchase Order form with one material line]

### 5.4 View, edit, search, and filter

- Use the eye/view action on a PO row to open its details.
- Use the search field to find a PO by number, supplier, or vehicle.
- Filter by Supplier, Payment Status, Order Status, and date range; select **Reset** to clear filters.
- Use the Edit action for an active editable PO. The item lines are replaced by the values submitted on update; totals and stock are recalculated.
- Sort supported columns by selecting the column heading. Use pagination controls to change page or rows per page.

### 5.5 Cancel, delete, and print

There is no hard-delete or approval workflow for purchase orders. Use **Cancel** for an active PO that can be reversed. If supplier payments exist, delete/reverse those payments first. Cancellation is blocked if stock from the PO has already been sold. The PO remains as a canceled audit record. There is no dedicated PO print action; use the Purchase Statement report and its PDF/Excel export when a report copy is needed.

### 5.6 PO fields

| Field | Description / validation |
|---|---|
| PO Number | Generated by the server, format `PO-YYYYMM-#####`; do not enter manually. |
| PO Date | Required calendar date; determines the generated number period. |
| Supplier | Required active supplier. |
| Vehicle Number | Optional text, up to 20 characters; not a vehicle registry. |
| Material | Required material master record for each line. |
| Quantity | Required and greater than zero (minimum 0.001). |
| Rate / Ton | Required and non-negative. |
| GST % | 0–100; default is 5%. |
| Amount | Quantity multiplied by rate. |
| GST Amount | Amount multiplied by GST percentage. |
| Total Amount | Amount plus GST. |
| Remarks | Optional, up to 500 characters. |

**Example:** 10 tons x INR 300/ton = INR 3,000; GST at 5% = INR 150; PO total = INR 3,150.

### 5.7 Common validation and business rules

- Missing date/supplier/material or empty line list: correct the required field and retry.
- Zero/negative quantity: enter a positive quantity.
- Negative rate or GST above 100: correct the value.
- Inactive supplier: activate the supplier or select another active supplier.
- Payment exists: reverse supplier payments before editing/canceling.
- Stock already sold: the purchase cannot be canceled because reversing it would make stock inconsistent.
- PO numbers are unique/generated; the application does not promise to reject a second PO with otherwise identical details.

## 6. Sales Orders

### 6.1 Purpose and workflow

Sales Orders record customer invoices and reduce inventory. Sales Orders are available to all three roles, with different write permissions.

```text
Choose customer -> Add material lines -> Review discount/GST/advance -> Create order/invoice
       -> Stock decreases and customer receivable is posted
       -> Receive customer payments -> Cancel only after reversible payments are removed
```

### 6.2 Create a sales order

1. Select **Sales Orders** and **New Sales Order**.
2. Confirm Order Date and select an active Customer.
3. Optionally update Delivery Address, enter Vehicle Number, and add Remarks.
4. Add Material lines with Quantity, Rate, Discount, and GST %.
5. Review the subtotal, discount, tax, Grand Total, and Pending Amount.
6. Optionally enter an Advance Amount and Payment Mode. The advance cannot exceed the order total.
7. Select **Create Sales Order**. The system assigns a sales-order number and invoice number.

> [Screenshot placeholder: New Sales Order form showing discount, tax, and total]

### 6.3 Edit, view, download invoice, and cancel

- Use View to inspect an order and **Download invoice** for the PDF invoice.
- Edit an active order if permitted. Stock and balances are recalculated.
- There is no Delete or Sales Return workflow. Use **Cancel** to reverse an eligible sale.
- Customer payments other than the advance must be reversed before the order can be edited/canceled. Sales cancellation restores stock and reverses the customer balance.

### 6.4 Sales fields and calculations

| Field | Description / validation |
|---|---|
| Customer | Required active customer. Delivery address defaults to the customer's address when left blank. |
| Material | Required material master record. |
| Quantity | Required and greater than zero (minimum 0.001); available stock is validated. |
| Rate | Required and non-negative. |
| Discount | Non-negative and cannot exceed quantity x rate for that line. |
| GST % | 0–100; default is 5%. |
| Line Amount | Quantity x rate minus discount. |
| GST Amount | Line Amount x GST percentage. |
| Line Total | Line Amount plus GST. |
| Advance Amount | Optional, non-negative, and no greater than the order total. |

**Example:** 2 tons x INR 500 = INR 1,000 gross; INR 50 discount gives INR 950 taxable amount; 5% GST is INR 47.50; total is INR 997.50.

### 6.5 Business rules and common errors

- Insufficient stock: reduce quantity or record a valid purchase first.
- Credit limit exceeded: collect customer payments or review the credit limit. A limit of zero means no limit is enforced.
- Discount exceeds gross line amount: reduce the discount.
- Payments exist: reverse non-advance customer receipts before editing/canceling.
- Sales returns are not supported in this build; use your approved offline return process and reconcile with an administrator.

## 7. Customer Management

Open **Masters > Customers**. Use **Add Customer**, Edit, Search, Status filters, and Delete for unused records.

| Field | Description / validation |
|---|---|
| Customer Name | Required; up to 150 characters. |
| Company Name | Optional; up to 150 characters. |
| GST Number | Optional; if entered, use a 15-character GSTIN format. |
| Mobile Number | Required; exactly 10 digits. |
| Email | Optional valid email address. |
| Address | Required; up to 500 characters. |
| Credit Limit | Required; zero means unlimited, otherwise non-negative. |
| Status | Active or Inactive; inactive customers cannot be used for new sales. |

Customer codes are generated (for example `CUST-0001`). A used customer cannot be deleted; mark it Inactive. The current service does not prevent duplicate customer mobile numbers or GST numbers; verify records carefully before creating a customer.

### Customer statement

Go to **Accounts > Customer Statement**, select a customer and optional From/To dates, then **Generate**. Sales are debits; receipts are credits. The report shows opening balance, total sales, total collection, running balance, and closing balance.

> [Screenshot placeholder: Customer list and Customer Statement report]

## 8. Master Data: Suppliers, Employees, and Materials

### 8.1 Supplier Management

Open **Masters > Suppliers**. Select **Add Supplier**, complete required fields, and Save. Use search/status filters to find a supplier. Edit supplier details as needed. Suppliers referenced by purchase orders cannot be deleted; mark them Inactive instead.

| Field | Description / validation |
|---|---|
| Supplier Name | Required; up to 150 characters. |
| GST Number | Optional; if entered, use a valid GSTIN format. |
| PAN Number | Optional; if entered, use a valid 10-character PAN format. |
| Contact Person | Required; up to 120 characters. |
| Mobile Number | Required; exactly 10 digits. |
| Address | Required; up to 500 characters. |
| Bank Details | Optional; up to 500 characters. |
| Status | Active or Inactive. |

Supplier codes are generated (for example `SUP-0001`). **Accounts > Supplier Statement** shows purchases as credits, payments as debits, and the closing payable balance.

**Example:** Add “North Quarry Supply” with a contact person, 10-digit mobile, and address. Add a GSTIN only after checking it against the supplier's tax registration document. After creating a PO, the supplier's outstanding amount increases; recording payment reduces it.

### 8.2 Employee Management

Open **Masters > Employees**. Admin can create, edit, search, and soft-delete employee records; Manager can view them. Employee codes are generated (`EMP-####`).

| Field | Description / validation |
|---|---|
| Full Name | Required; up to 120 characters. |
| Designation | Optional; up to 100 characters. |
| Mobile Number | Required; exactly 10 digits. |
| Email | Optional valid email address. |
| Joining Date | Required date. |
| Salary | Required; non-negative amount. |
| Address | Optional; up to 500 characters. |
| Status | Active or Inactive. |

**Example:** Create an employee with a valid mobile number, joining date, and salary; use Search to confirm the generated employee code.

### 8.3 Material Management

Open **Masters > Materials**. Verify preloaded products before adding a new material. Material names must be unique (case-insensitive); used materials cannot be deleted. Material codes are generated (`MAT-####`).

| Field | Description / validation |
|---|---|
| Material Name | Required; up to 100 characters. |
| Unit | Unit label, commonly `TON`, up to 20 characters. |
| Description | Optional; up to 500 characters. |

**Example:** A material named “Crusher Dust” with unit `TON` appears in PO, sales, and Inventory selectors. Use one unit consistently.

## 9. Inventory Management

The Inventory screen is read-only. Switch between card and table views and search by material. A material is flagged Low stock at 100 tons or below (configured threshold); zero or less is Out of stock.

**Calculation:** `Current Stock = Purchased Stock - Sold Stock`.

- Active PO creation adds purchased stock.
- Active sales order creation subtracts sold stock.
- Eligible PO/sales cancellations reverse their stock movement.
- Inventory rows are locked during movements and the database prevents negative stock.

There is no manual opening-stock entry or stock-adjustment screen in this build. Correct opening balances through an approved data migration/administrator process; do not invent a purchase order to disguise an adjustment.

> [Screenshot placeholder: Inventory cards showing purchased, sold, and current stock]

**Example:** If a material has 100 tons purchased and 35 tons sold, current stock is 65 tons. A sale for more than available stock is rejected.

## 10. Vehicle Management

A separate Vehicle Management module is not present. Purchase and sales orders accept an optional Vehicle Number as text, but the application does not register vehicles, assign drivers, check duplicate vehicle numbers, or track vehicle availability/status.

## 11. Delivery Management

A separate Delivery Management module is not present. Sales orders store a Delivery Address and Vehicle Number, but there are no delivery creation, dispatch assignment, partial-delivery, completion, or cancellation statuses. Pending-delivery dashboard cards are also not present.

## 12. Accounts and Payments

### Expense Management

Open **Expenses > Add Expense**. Enter the date, expense type, and positive amount; add a description if useful. PDF/PNG/JPG attachments up to 5 MB are supported. Expense types are Fuel, Vehicle Maintenance, Salary, Office Expense, Loading Charges, Electricity, Rent, and Miscellaneous.

| Field | Description / validation |
|---|---|
| Date | Required expense date. |
| Expense Type | Required category from the supported list. |
| Amount | Required and greater than zero. |
| Description | Optional; up to 500 characters. |
| Attachment | Optional PDF/PNG/JPG, maximum 5 MB. |

**Example:** Record INR 1,250 as Fuel with a receipt image/PDF; it appears in Expense Report and is included in Profit & Loss.

### Customer payments

Open **Customer Payments > Receive Payment**. Choose Payment Date, Payment Mode, Customer, and one pending Invoice. Enter an amount not greater than that invoice's pending amount; optionally enter reference/cheque number and remarks. Delete is a reversal action and updates the invoice/customer ledger.

### Supplier payments

Open **Supplier Payments > Make Payment**. Select Payment Date, Payment Mode, Supplier, and a pending Purchase Order. Enter an amount up to the PO balance. Deleting the payment reverses it on the PO and supplier ledger.

Supported payment modes: Cash, UPI, Bank Transfer, Cheque.

| Payment field | Customer receipt | Supplier payment |
|---|---|---|
| Payment Date | Required date. | Required date. |
| Payment Mode | Cash, UPI, Bank Transfer, or Cheque. | Cash, UPI, Bank Transfer, or Cheque. |
| Customer/Supplier | Required party. | Required party. |
| Invoice/PO | Required single pending invoice. | Required single pending PO. |
| Amount | Greater than zero and no more than invoice pending balance. | Greater than zero and no more than PO balance. |
| Reference / Cheque No. | Optional, up to 100 characters. | Optional, up to 100 characters. |
| Remarks | Optional, up to 500 characters. | Optional, up to 500 characters. |

**Example:** If an invoice has INR 1,000 pending, receive INR 400 against that invoice; the remaining receivable becomes INR 600.

### Statements and aging

- Customer Statement: invoices/debits and customer receipts/credits.
- Supplier Statement: purchase credits and supplier-payment debits.
- Receivables: customer balances and aging.
- Payables: supplier balances and aging.

Payment records are tied to one invoice/PO; overpayments are rejected. Use reference numbers for bank/UPI/cheque reconciliation.

## 13. Profit and Loss

Open **Reports > Profit & Loss**. Select Daily, Monthly, Yearly, or Custom period, complete the requested date fields, and select **Generate**.

- **Revenue:** sales value after discounts, excluding GST.
- **Purchase Cost:** purchase amount excluding GST.
- **Gross Profit:** Revenue minus Purchase Cost.
- **Net Profit:** Gross Profit minus expenses.
- If net result is negative, the Dashboard shows the magnitude as Monthly Loss; if positive, as Monthly Profit.

**Example:** Sales INR 10,000 less INR 500 discount = revenue INR 9,500 (GST excluded). Purchase cost INR 7,000 gives gross profit INR 2,500. Expenses INR 800 give net profit INR 1,700.

## 14. Reports

Reports are opened from **Reports** or **Accounts**. Select available filters and click **Generate**. Reports with required filters must have them selected first.

| Report | Purpose / common filters | Output / export |
|---|---|---|
| Customer Statement | Customer required; optional dates. | Ledger, opening/closing balance; PDF/Excel. |
| Supplier Statement | Supplier required; optional dates. | Purchase/payment ledger; PDF/Excel. |
| Purchase Statement | Optional dates, supplier, material, PO number. | Purchase lines/totals; PDF/Excel. |
| Sales Statement | Optional dates, customer, material, invoice number. | Invoice lines/totals; PDF/Excel. |
| Material Profit | Optional date range. | Profit by material; PDF/Excel. |
| Current Stock | No required filter. | Purchased/sold/current by material; PDF/Excel. |
| Material-wise Stock | Optional material. | Stock by material; PDF/Excel. |
| Stock Movement | Optional dates/material. | Purchase IN and sales OUT rows; PDF/Excel. |
| Customer Payments | Optional dates/customer. | Customer receipt report; PDF/Excel. |
| Supplier Payments | Optional dates/supplier. | Supplier payment report; PDF/Excel. |
| Expense Report | Optional dates/expense type. | Expense rows/totals; PDF/Excel. |
| Profit & Loss | Period selector (date range is required by report request). | Revenue/cost/expense/net result; PDF/Excel. |

> [Screenshot placeholder: Report filters, generated report, and export controls]

**Sample output:** A Current Stock report contains one row per material, for example `Dust | TON | Purchased 200 | Sold 40 | Current 160`. A Customer Statement shows a dated invoice debit followed by receipt credits and a running balance. The exact column set depends on the selected report.

## 15. Search, Filters, Sorting, and Pagination

1. Use the search box on a list to search supported names, codes, order numbers, vehicle text, or reference fields.
2. Use dropdown/date filters to narrow results. Available filters vary by module.
3. Select **Reset** to clear filters and return to the default list.
4. Select sortable column headings to change order where supported.
5. Use the table pager to move pages and choose 10, 20, 50, or 100 rows per page.
6. On small screens, swipe/scroll horizontally inside the table container; the rest of the page remains within the viewport.

## 16. Export and Printing

- Reports offer **Export PDF** and **Export Excel** when the report supports exports.
- Sales Orders offer **Download invoice** for a PDF invoice.
- There is no dedicated Purchase Order print action; use the Purchase Statement PDF/Excel export.
- If a download does not start, check browser download permissions and retry. Do not send exported financial data to an unapproved destination.

## 17. Notifications and Validation

- Green/success notifications confirm an operation completed.
- Field-level messages identify missing/invalid input; correct the field before resubmitting.
- Warning/status chips identify canceled, inactive, low-stock, pending, partial-paid, or paid records.
- Error responses commonly mean: 400 validation/malformed data; 401 sign-in required/expired; 403 role not allowed; 404 record not found; 409 business rule conflict; 413 upload too large; 500 unexpected server error.
- For a 401, sign in again. If repeated, contact the administrator and include the time and page name, not your password/token.

## 18. User Roles and Permissions

The only configured application roles are Admin, Manager, and Sales Executive. Accountant, Purchase User, Inventory User, Dispatch User, and Read Only User are not separate roles in the current system.

### User Management (Admin only)

Open **Users > Add User**. Create a named account, assign one of the three roles below, set Active, and provide a temporary password securely. Edit a user to change role/profile/password or deactivate the account. Delete is soft delete.

| User field | Description / validation |
|---|---|
| Username | Required, 3–50 characters; letters, digits, dot, underscore, hyphen. |
| Full Name | Required; up to 120 characters. |
| Email | Required valid email address. |
| Mobile Number | Optional; if entered, exactly 10 digits. |
| Role | Required: Admin, Manager, or Sales Executive. |
| Password | Required for create; 8–100 characters with uppercase, lowercase, and digit. Optional on edit to keep existing password. |
| Active | Controls whether the account can sign in. |

| Module | Admin | Manager | Sales Executive |
|---|---|---|---|
| Dashboard | View | View | View |
| Users | Create/edit/delete/view | No access | No access |
| Employees | Full access | View | No access |
| Suppliers | Full access | Full access | No access |
| Customers | Full access | Full access | Create/edit/view; no delete |
| Materials | Full access | Full access | View |
| Purchase Orders | Full access incl. cancel | Full access incl. cancel | No access |
| Inventory | View | View | View |
| Sales Orders | Full access incl. cancel | Full access incl. cancel | Create/edit/view; no cancel |
| Customer Payments | Full access | Full access | Create/view; no delete |
| Supplier Payments / Expenses | Full access | Full access | No access |
| Customer Statement / Receivables | View | View | View |
| Supplier Statement / Payables | View | View | No access |
| Reports | All reports | All reports | Sales Statement and Customer Statement only |

Permissions are enforced by the server. If a menu is missing or an action is disabled, contact the administrator rather than sharing another user's account.

## 19. Frequently Asked Questions

**How do I create a PO?** Purchase Orders > New Purchase Order; choose supplier, add material lines, check totals, and Create.

**How do I edit a sales order?** Sales Orders > Edit. If a non-advance customer payment exists, reverse the payment first.

**How do I cancel a PO or sale?** Use the row's Cancel action. Reverse payments first. Cancellation reverses balances/stock when business rules permit; canceled records remain for audit.

**How do I return a sale or record a delivery?** These workflows are not available in the current build; follow company procedure and ask the product owner about roadmap/support.

**How do I export a report?** Generate the report, then select Export PDF or Export Excel.

**How do I reset filters?** Select Reset above the list/report.

**How do I update inventory?** Create/cancel supported POs and sales orders. Manual opening-stock/adjustment screens are not available.

**How do I record an expense?** Expenses > Add Expense; select date/type, enter a positive amount, and optionally attach a receipt.

**How do I add an employee or material?** Masters > Employees or Materials; complete required fields and Save. Codes are generated automatically.

## 20. Troubleshooting

| Issue | Possible cause | Resolution |
|---|---|---|
| Cannot log in | Incorrect username/password, inactive user, expired session, API/network unavailable. | Recheck credentials; use password recovery; ask Admin to verify active status and API health. |
| Unable to save a record | Required field missing or invalid; inactive master; business rule/credit/stock conflict. | Read field message; verify status, stock, balances, and payment dependencies. |
| PO/sale edit or cancellation rejected | Payments exist, supplier/customer inactive, or purchase stock already sold. | Reverse eligible payments first; confirm current stock before reversing a PO. |
| Report not loading | Required filter missing, invalid date range, expired session, API error. | Complete required filters, set From <= To, sign in again, retry. |
| Attachment upload fails | File type or size rejected; network/storage issue. | Use PDF/PNG/JPG up to 5 MB; retry and contact Admin if persistent. |
| Export fails | Browser blocked download or API/network error. | Allow downloads, retry, and provide Admin the report and time of failure. |
| Low/negative stock concern | Incorrect purchase/sale transaction or cancellation dependency. | Review stock movement and related orders; do not adjust data outside approved process. |

## 21. Glossary

| Term | Meaning |
|---|---|
| PO | Purchase Order: a record of materials bought from a supplier. |
| Sales Order / Invoice | Customer sale; invoice number is generated with the order. |
| Customer Ledger | Running customer account: sales debit, receipts credit. |
| Supplier Ledger | Running supplier account: purchases credit, payments debit. |
| Dispatch | Physical release/transport of goods. A dispatch tracking module is not available here. |
| GST | Goods and Services Tax percentage applied to a transaction line. |
| Receivable | Amount still due from a customer. |
| Payable | Amount still due to a supplier. |
| Inventory | Material quantities purchased, sold, and currently available. |
| Soft delete | Record is hidden from normal lists but retained for referential/audit integrity. |

## 22. Best Practices

- Search before creating a new customer or supplier; duplicate mobile/GST prevention is not enforced for customers.
- Use accurate dates, quantities, rates, GST, payment references, and remarks.
- Verify material, customer/supplier, and totals before saving.
- Record payments against the correct invoice or PO; reconcile reference numbers promptly.
- Check stock before confirming delivery commitments; the app blocks sales beyond stock.
- Mark referenced customers/suppliers inactive instead of trying to delete them.
- Export reports only to approved storage; protect customer/financial information.
- Use named individual accounts and sign out of shared workstations.

## 23. Appendix: Validation Rules and Error Codes

### 23.1 Key validations

- Password: 8–100 characters; uppercase, lowercase, and digit required.
- Customer/Supplier mobile: 10 digits.
- GSTIN/PAN: format-checked when supplied.
- Purchase/Sales quantity: greater than zero; minimum 0.001.
- Purchase/Sales rate: non-negative.
- GST percentage: 0–100.
- Sales discount: non-negative and not greater than the line gross amount.
- Payment: positive amount no greater than the invoice/PO pending balance.
- Expense: positive amount; attachments limited to 5 MB and PDF/PNG/JPG.

### 23.2 API status reference

| Status | Meaning |
|---:|---|
| 400 | Invalid or missing input. |
| 401 | Authentication required/invalid/expired. |
| 403 | Role does not have permission. |
| 404 | Record not found. |
| 409 | Duplicate/business rule conflict. |
| 413 | Upload exceeds allowed size. |
| 500 | Unexpected server failure; contact Admin with timestamp and page. |

### 23.3 Keyboard shortcuts

No application-specific shortcuts are documented. Use standard browser keyboard navigation (Tab, Shift+Tab, Enter, Escape) and do not rely on shortcuts for critical transactions.

## 24. Support and Change Notes

Record the page/module, timestamp, record code/number, expected result, and observed message when reporting an issue. Do not include passwords, JWTs, refresh tokens, or unredacted personal data. Feature requests for vehicle registration, delivery tracking, returns, opening stock, extra roles, or PO approvals should be raised with the product owner.
