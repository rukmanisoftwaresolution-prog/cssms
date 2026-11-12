import ReportPage from './ReportPage';

const from = { name: 'fromDate', label: 'From Date', type: 'date' };
const to = { name: 'toDate', label: 'To Date', type: 'date' };
const customer = (required) => ({ name: 'customerId', label: 'Customer', type: 'select', lookup: 'customers', required });
const supplier = (required) => ({ name: 'supplierId', label: 'Supplier', type: 'select', lookup: 'suppliers', required });
const material = { name: 'materialId', label: 'Material', type: 'select', lookup: 'materials' };

// Accounts
export const CustomerStatementPage = () => (
  <ReportPage
    reportKey="customer-statement"
    title="Customer Statement"
    subtitle="Ledger of invoices and receipts for a customer"
    filters={[customer(true), from, to]}
  />
);

export const SupplierStatementPage = () => (
  <ReportPage
    reportKey="supplier-statement"
    title="Supplier Statement"
    subtitle="Ledger of purchases and payments for a supplier"
    filters={[supplier(true), from, to]}
  />
);

export const ReceivablesPage = () => (
  <ReportPage reportKey="receivable" title="Receivables" subtitle="Outstanding amounts to be received from customers" />
);

export const PayablesPage = () => (
  <ReportPage reportKey="payable" title="Payables" subtitle="Outstanding amounts payable to suppliers" />
);

// Reports
export const PurchaseStatementPage = () => (
  <ReportPage
    reportKey="purchase-statement"
    title="Purchase Statement"
    filters={[from, to, supplier(false), material, { name: 'poNumber', label: 'PO Number', type: 'text' }]}
  />
);

export const SalesStatementPage = () => (
  <ReportPage
    reportKey="sales-statement"
    title="Sales Statement"
    filters={[from, to, customer(false), material, { name: 'invoiceNumber', label: 'Invoice Number', type: 'text' }]}
  />
);

export const MaterialProfitPage = () => (
  <ReportPage reportKey="material-profit" title="Material Profit" subtitle="Profit per material" filters={[from, to]} />
);

export const StockCurrentPage = () => <ReportPage reportKey="stock-current" title="Current Stock Report" />;

export const StockMaterialWisePage = () => (
  <ReportPage reportKey="stock-material-wise" title="Material-wise Stock Report" filters={[material]} />
);

export const StockMovementPage = () => (
  <ReportPage reportKey="stock-movement" title="Stock Movement" subtitle="Purchases (IN) and sales (OUT)" filters={[from, to, material]} />
);

export const CustomerPaymentReportPage = () => (
  <ReportPage reportKey="customer-payments" title="Customer Payment Report" filters={[from, to, customer(false)]} />
);

export const SupplierPaymentReportPage = () => (
  <ReportPage reportKey="supplier-payments" title="Supplier Payment Report" filters={[from, to, supplier(false)]} />
);

export const ExpenseReportPage = () => (
  <ReportPage
    reportKey="expense-report"
    title="Expense Report"
    filters={[from, to, { name: 'expenseType', label: 'Expense Type', type: 'select', lookup: 'expenseTypes' }]}
  />
);
