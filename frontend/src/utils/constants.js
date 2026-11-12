export const ROLES = {
  ADMIN: 'ADMIN',
  MANAGER: 'MANAGER',
  SALES_EXECUTIVE: 'SALES_EXECUTIVE',
};

export const ROLE_OPTIONS = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'MANAGER', label: 'Manager' },
  { value: 'SALES_EXECUTIVE', label: 'Sales Executive' },
];

export const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

export const ORDER_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

export const PAYMENT_STATUS_OPTIONS = [
  { value: 'PENDING', label: 'Pending' },
  { value: 'PARTIAL_PAID', label: 'Partial Paid' },
  { value: 'PAID', label: 'Paid' },
];

export const PAYMENT_MODE_OPTIONS = [
  { value: 'CASH', label: 'Cash' },
  { value: 'UPI', label: 'UPI' },
  { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
  { value: 'CHEQUE', label: 'Cheque' },
];

export const EXPENSE_TYPE_OPTIONS = [
  { value: 'FUEL', label: 'Fuel' },
  { value: 'VEHICLE_MAINTENANCE', label: 'Vehicle Maintenance' },
  { value: 'SALARY', label: 'Salary' },
  { value: 'OFFICE_EXPENSE', label: 'Office Expense' },
  { value: 'LOADING_CHARGES', label: 'Loading Charges' },
  { value: 'ELECTRICITY', label: 'Electricity' },
  { value: 'RENT', label: 'Rent' },
  { value: 'MISCELLANEOUS', label: 'Miscellaneous' },
];

export const DEFAULT_UNIT = 'TON';
export const DEFAULT_GST = 5;
export const LOW_STOCK_THRESHOLD = 100;
export const MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024;
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
export const LOOKUP_SIZE = 1000;

const ALL = ['ADMIN', 'MANAGER', 'SALES_EXECUTIVE'];
const AM = ['ADMIN', 'MANAGER'];
const ADMIN = ['ADMIN'];

/**
 * Role matrix (mirrors docs/API.md). `view` controls menu + route access;
 * other keys control which write actions are rendered.
 */
export const PERMISSIONS = {
  dashboard: { view: ALL },
  users: { view: ADMIN, create: ADMIN, update: ADMIN, delete: ADMIN },
  employees: { view: AM, create: ADMIN, update: ADMIN, delete: ADMIN },
  suppliers: { view: AM, create: AM, update: AM, delete: AM },
  customers: { view: ALL, create: ALL, update: ALL, delete: AM },
  materials: { view: ALL, create: AM, update: AM, delete: AM },
  purchaseOrders: { view: AM, create: AM, update: AM, cancel: AM },
  inventory: { view: ALL },
  salesOrders: { view: ALL, create: ALL, update: ALL, cancel: AM },
  customerPayments: { view: ALL, create: ALL, delete: AM },
  supplierPayments: { view: AM, create: AM, delete: AM },
  expenses: { view: AM, create: AM, update: AM, delete: AM },
  customerStatement: { view: ALL },
  supplierStatement: { view: AM },
  receivable: { view: ALL },
  payable: { view: AM },
  purchaseStatement: { view: AM },
  salesStatement: { view: ALL },
  materialProfit: { view: AM },
  stockReports: { view: AM },
  paymentReports: { view: AM },
  expenseReport: { view: AM },
  profitLoss: { view: AM },
};

export function can(role, resource, action = 'view') {
  const allowed = PERMISSIONS[resource]?.[action];
  return !!role && !!allowed && allowed.includes(role);
}

/** Sidebar structure. Items are hidden when the role lacks `view` on `resource`. */
export const MENU = [
  { label: 'Dashboard', path: '/', icon: 'Dashboard', resource: 'dashboard' },
  {
    label: 'Masters',
    icon: 'Category',
    children: [
      { label: 'Employees', path: '/employees', icon: 'Badge', resource: 'employees' },
      { label: 'Suppliers', path: '/suppliers', icon: 'LocalShipping', resource: 'suppliers' },
      { label: 'Customers', path: '/customers', icon: 'People', resource: 'customers' },
      { label: 'Materials', path: '/materials', icon: 'Landscape', resource: 'materials' },
    ],
  },
  { label: 'Purchase Orders', path: '/purchase-orders', icon: 'ShoppingCart', resource: 'purchaseOrders' },
  { label: 'Inventory', path: '/inventory', icon: 'Inventory2', resource: 'inventory' },
  { label: 'Sales Orders', path: '/sales-orders', icon: 'PointOfSale', resource: 'salesOrders' },
  { label: 'Customer Payments', path: '/customer-payments', icon: 'Payments', resource: 'customerPayments' },
  { label: 'Supplier Payments', path: '/supplier-payments', icon: 'AccountBalanceWallet', resource: 'supplierPayments' },
  { label: 'Expenses', path: '/expenses', icon: 'ReceiptLong', resource: 'expenses' },
  {
    label: 'Accounts',
    icon: 'AccountBalance',
    children: [
      { label: 'Customer Statement', path: '/accounts/customer-statement', icon: 'Description', resource: 'customerStatement' },
      { label: 'Supplier Statement', path: '/accounts/supplier-statement', icon: 'Description', resource: 'supplierStatement' },
      { label: 'Receivables', path: '/accounts/receivables', icon: 'CallReceived', resource: 'receivable' },
      { label: 'Payables', path: '/accounts/payables', icon: 'CallMade', resource: 'payable' },
    ],
  },
  {
    label: 'Reports',
    icon: 'Assessment',
    children: [
      { label: 'Purchase Statement', path: '/reports/purchase-statement', icon: 'Article', resource: 'purchaseStatement' },
      { label: 'Sales Statement', path: '/reports/sales-statement', icon: 'Article', resource: 'salesStatement' },
      { label: 'Material Profit', path: '/reports/material-profit', icon: 'TrendingUp', resource: 'materialProfit' },
      { label: 'Stock - Current', path: '/reports/stock-current', icon: 'Warehouse', resource: 'stockReports' },
      { label: 'Stock - Material Wise', path: '/reports/stock-material-wise', icon: 'Warehouse', resource: 'stockReports' },
      { label: 'Stock - Movement', path: '/reports/stock-movement', icon: 'SwapVert', resource: 'stockReports' },
      { label: 'Payments - Customer', path: '/reports/customer-payments', icon: 'Payments', resource: 'paymentReports' },
      { label: 'Payments - Supplier', path: '/reports/supplier-payments', icon: 'Payments', resource: 'paymentReports' },
      { label: 'Expense Report', path: '/reports/expense-report', icon: 'ReceiptLong', resource: 'expenseReport' },
      { label: 'Profit & Loss', path: '/reports/profit-loss', icon: 'ShowChart', resource: 'profitLoss' },
    ],
  },
  { label: 'Users', path: '/users', icon: 'ManageAccounts', resource: 'users' },
];

export function filterMenu(role) {
  return MENU.map((item) => {
    if (item.children) {
      const children = item.children.filter((c) => can(role, c.resource));
      return children.length ? { ...item, children } : null;
    }
    return can(role, item.resource) ? item : null;
  }).filter(Boolean);
}
