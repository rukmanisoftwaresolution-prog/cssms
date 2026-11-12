import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import { PageLoader } from '../components/LoadingOverlay';
import { ProtectedRoute, PublicOnlyRoute, RoleRoute } from './guards';

const lazyNamed = (loader, name) => lazy(() => loader().then((m) => ({ default: m[name] })));

const LoginPage = lazy(() => import('../pages/auth/LoginPage'));
const ResetPasswordPage = lazy(() => import('../pages/auth/ResetPasswordPage'));
const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'));
const EmployeesPage = lazy(() => import('../pages/masters/EmployeesPage'));
const SuppliersPage = lazy(() => import('../pages/masters/SuppliersPage'));
const CustomersPage = lazy(() => import('../pages/masters/CustomersPage'));
const MaterialsPage = lazy(() => import('../pages/masters/MaterialsPage'));
const PurchaseOrdersPage = lazy(() => import('../pages/purchase/PurchaseOrdersPage'));
const PurchaseOrderFormPage = lazy(() => import('../pages/purchase/PurchaseOrderFormPage'));
const SalesOrdersPage = lazy(() => import('../pages/sales/SalesOrdersPage'));
const SalesOrderFormPage = lazy(() => import('../pages/sales/SalesOrderFormPage'));
const CustomerPaymentsPage = lazy(() => import('../pages/payments/CustomerPaymentsPage'));
const SupplierPaymentsPage = lazy(() => import('../pages/payments/SupplierPaymentsPage'));
const ExpensesPage = lazy(() => import('../pages/expenses/ExpensesPage'));
const InventoryPage = lazy(() => import('../pages/inventory/InventoryPage'));
const ProfitLossPage = lazy(() => import('../pages/reports/ProfitLossPage'));
const UsersPage = lazy(() => import('../pages/users/UsersPage'));
const ProfilePage = lazy(() => import('../pages/profile/ProfilePage'));
const ChangePasswordPage = lazy(() => import('../pages/profile/ChangePasswordPage'));
const NotFoundPage = lazy(() => import('../pages/errors/NotFoundPage'));

const reports = () => import('../pages/reports/ReportPages');
const CustomerStatementPage = lazyNamed(reports, 'CustomerStatementPage');
const SupplierStatementPage = lazyNamed(reports, 'SupplierStatementPage');
const ReceivablesPage = lazyNamed(reports, 'ReceivablesPage');
const PayablesPage = lazyNamed(reports, 'PayablesPage');
const PurchaseStatementPage = lazyNamed(reports, 'PurchaseStatementPage');
const SalesStatementPage = lazyNamed(reports, 'SalesStatementPage');
const MaterialProfitPage = lazyNamed(reports, 'MaterialProfitPage');
const StockCurrentPage = lazyNamed(reports, 'StockCurrentPage');
const StockMaterialWisePage = lazyNamed(reports, 'StockMaterialWisePage');
const StockMovementPage = lazyNamed(reports, 'StockMovementPage');
const CustomerPaymentReportPage = lazyNamed(reports, 'CustomerPaymentReportPage');
const SupplierPaymentReportPage = lazyNamed(reports, 'SupplierPaymentReportPage');
const ExpenseReportPage = lazyNamed(reports, 'ExpenseReportPage');

const guarded = (resource, element, action = 'view') => (
  <RoleRoute resource={resource} action={action}>
    {element}
  </RoleRoute>
);

export default function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader minHeight="100vh" />}>
      <Routes>
        <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={guarded('dashboard', <DashboardPage />)} />

          <Route path="employees" element={guarded('employees', <EmployeesPage />)} />
          <Route path="suppliers" element={guarded('suppliers', <SuppliersPage />)} />
          <Route path="customers" element={guarded('customers', <CustomersPage />)} />
          <Route path="materials" element={guarded('materials', <MaterialsPage />)} />

          <Route path="purchase-orders" element={guarded('purchaseOrders', <PurchaseOrdersPage />)} />
          <Route path="purchase-orders/new" element={guarded('purchaseOrders', <PurchaseOrderFormPage />, 'create')} />
          <Route path="purchase-orders/:id/edit" element={guarded('purchaseOrders', <PurchaseOrderFormPage />, 'update')} />

          <Route path="inventory" element={guarded('inventory', <InventoryPage />)} />

          <Route path="sales-orders" element={guarded('salesOrders', <SalesOrdersPage />)} />
          <Route path="sales-orders/new" element={guarded('salesOrders', <SalesOrderFormPage />, 'create')} />
          <Route path="sales-orders/:id/edit" element={guarded('salesOrders', <SalesOrderFormPage />, 'update')} />

          <Route path="customer-payments" element={guarded('customerPayments', <CustomerPaymentsPage />)} />
          <Route path="supplier-payments" element={guarded('supplierPayments', <SupplierPaymentsPage />)} />
          <Route path="expenses" element={guarded('expenses', <ExpensesPage />)} />

          <Route path="accounts/customer-statement" element={guarded('customerStatement', <CustomerStatementPage />)} />
          <Route path="accounts/supplier-statement" element={guarded('supplierStatement', <SupplierStatementPage />)} />
          <Route path="accounts/receivables" element={guarded('receivable', <ReceivablesPage />)} />
          <Route path="accounts/payables" element={guarded('payable', <PayablesPage />)} />

          <Route path="reports/purchase-statement" element={guarded('purchaseStatement', <PurchaseStatementPage />)} />
          <Route path="reports/sales-statement" element={guarded('salesStatement', <SalesStatementPage />)} />
          <Route path="reports/material-profit" element={guarded('materialProfit', <MaterialProfitPage />)} />
          <Route path="reports/stock-current" element={guarded('stockReports', <StockCurrentPage />)} />
          <Route path="reports/stock-material-wise" element={guarded('stockReports', <StockMaterialWisePage />)} />
          <Route path="reports/stock-movement" element={guarded('stockReports', <StockMovementPage />)} />
          <Route path="reports/customer-payments" element={guarded('paymentReports', <CustomerPaymentReportPage />)} />
          <Route path="reports/supplier-payments" element={guarded('paymentReports', <SupplierPaymentReportPage />)} />
          <Route path="reports/expense-report" element={guarded('expenseReport', <ExpenseReportPage />)} />
          <Route path="reports/profit-loss" element={guarded('profitLoss', <ProfitLossPage />)} />

          <Route path="users" element={guarded('users', <UsersPage />)} />
          <Route path="profile" element={<ProfilePage />} />
          <Route path="change-password" element={<ChangePasswordPage />} />
          <Route path="dashboard" element={<Navigate to="/" replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
