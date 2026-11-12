# CSSMS Responsive Audit Report

**Audit date:** 2026-10-29  
**Application:** Crusher Stone Supply Management System (React 18, Material UI, Vite)  
**Status:** Responsive changes implemented and verified.

## Executive Summary

- **300 viewport checks passed:** 30 authenticated routes at 10 widths, including all requested widths plus 1023px and 1441px breakpoint edges.
- **Document-level horizontal overflow:** 0 cases.
- **Frontend production build:** Passed.
- **Frontend diagnostics:** No errors reported.
- **Additional checks:** Login and reset-password at 320px; PO and sales edit forms at 320, 768, 1024, and 1441px; dashboard/inventory card columns; form actions; drawer behavior; dialog widths; report filters; table-local scrolling.

The responsive score is a heuristic for implemented screens, not a standardized accessibility certification. Vehicle, Delivery, and standalone Settings pages are absent, so they are excluded from the scores.

## Viewport Matrix

| Width | Category | Page overflow | Dashboard cards | Inventory cards | Notes |
|---:|---|---|---:|---:|---|
| 320 | Mobile | Pass | 1 | 1 | Login, order forms, filters, and dialog spot checks passed. |
| 375 | Mobile | Pass | 1 | 1 | Route sweep passed. |
| 425 | Mobile | Pass | 1 | 1 | Route sweep passed. |
| 768 | Tablet | Pass | 2 | 2 | Temporary drawer; report filters stack in two columns. |
| 1023 | Tablet | Pass | 2 | 2 | Temporary drawer remains available. |
| 1024 | Laptop | Pass | 4 | 4 | Permanent sidebar begins at the laptop breakpoint. |
| 1280 | Laptop | Pass | 4 | 4 | Dense sales line-item table remains locally scrollable. |
| 1440 | Laptop | Pass | 4 | 4 | Dense sales line-item table has minimal local horizontal scroll. |
| 1441 | Desktop | Pass | 4 | 4 | Sales line-item table fits its scroll container. |
| 1920 | Desktop | Pass | 6 | 6 | Dashboard and inventory use six columns. |

**Measurement:** `document.documentElement.scrollWidth` and `document.body.scrollWidth` were compared against viewport width for each route/width pair. Wide data tables and transaction line-item tables scroll inside their own containers; they do not widen the page.

## Page-by-Page Audit

| Page | Current issue | Root cause | Suggested fix | Code changes |
|---|---|---|---|---|
| Login `/login` | No overflow found; mobile card was already max-width constrained. | Small card layout, but baseline type/button sizing was not consistently touch-friendly. | Keep card fluid and apply responsive type and 44px controls. | [theme.js](../frontend/src/theme.js); [LoginPage.jsx](../frontend/src/pages/auth/LoginPage.jsx) unchanged structurally. |
| Reset Password `/reset-password` | Invalid/missing-token state fits 320px; valid-token form not viewport-tested. | Public route renders an error state without a token. | Keep the card fluid; test a valid reset link when available. | [ResetPasswordPage.jsx](../frontend/src/pages/auth/ResetPasswordPage.jsx) uses shared theme. |
| Dashboard `/` | Cards used 2 columns at 1024px and 3 at 1280px/1920px. | `sm=6, lg=4` relied on MUI defaults that did not match the requested ranges. | Use 1/2/4/6 card grid and 1/2 chart grid. | [theme.js](../frontend/src/theme.js); [DashboardPage.jsx](../frontend/src/pages/dashboard/DashboardPage.jsx); [ChartCard.jsx](../frontend/src/components/ChartCard.jsx). |
| Employees `/employees` | Search/filter minimum widths could crowd narrow layouts; table needs local horizontal scrolling. | Generic filters and tables had fixed minimum sizing. | Fluid filters, zero page-level min-width, table-local overflow. | [FilterBar.jsx](../frontend/src/components/FilterBar.jsx); [SearchBar.jsx](../frontend/src/components/SearchBar.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx); [MasterCrudPage.jsx](../frontend/src/pages/masters/MasterCrudPage.jsx). |
| Suppliers `/suppliers` | Same shared list/filter behavior as Employees. | Shared CRUD table and filter components. | Reuse responsive shared components and accessible row actions. | [MasterCrudPage.jsx](../frontend/src/pages/masters/MasterCrudPage.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx). |
| Customers `/customers` | Same shared list/filter behavior as Employees. | Shared CRUD table and filter components. | Reuse responsive shared components and accessible row actions. | [MasterCrudPage.jsx](../frontend/src/pages/masters/MasterCrudPage.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx). |
| Materials `/materials` | Same shared list/filter behavior as Employees. | Shared CRUD table and filter components. | Reuse responsive shared components and accessible row actions. | [MasterCrudPage.jsx](../frontend/src/pages/masters/MasterCrudPage.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx). |
| Purchase Orders `/purchase-orders` | Multiple columns can exceed the viewport; small filters were fixed-width. | Dense business table and filter minimum widths. | Wrap filters, preserve all columns with local table scroll. | [FilterBar.jsx](../frontend/src/components/FilterBar.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx); [PurchaseOrdersPage.jsx](../frontend/src/pages/purchase/PurchaseOrdersPage.jsx). |
| New Purchase Order `/purchase-orders/new` | 980px line-item table cannot fit a phone; bottom actions needed stacking. | Many line-item fields are required simultaneously. | Keep horizontal scrolling inside the line-item area; stack actions full-width on mobile. | [PurchaseOrderFormPage.jsx](../frontend/src/pages/purchase/PurchaseOrderFormPage.jsx); [theme.js](../frontend/src/theme.js). |
| Edit Purchase Order `/purchase-orders/:id/edit` | Same dense line-item table as create. | Shared purchase form. | Local table scrolling; responsive fields/actions. | [PurchaseOrderFormPage.jsx](../frontend/src/pages/purchase/PurchaseOrderFormPage.jsx). |
| PO detail dialog | Dense item table could exceed dialog width. | Modal content includes a multi-column transaction table. | Constrain modal width and keep table scrolling inside the dialog. | [theme.js](../frontend/src/theme.js); [PurchaseOrderViewDialog.jsx](../frontend/src/pages/purchase/PurchaseOrderViewDialog.jsx). |
| Inventory `/inventory` | Cards used 2 columns at 1024px and 3 at desktop widths. | `sm=6, lg=3` default breakpoints. | Use 1/2/4/6 columns; wrap long material/status text; maintain card/table toggle. | [InventoryPage.jsx](../frontend/src/pages/inventory/InventoryPage.jsx); [theme.js](../frontend/src/theme.js). |
| Sales Orders `/sales-orders` | Dense table and filters could crowd small screens; action icon names were inconsistent. | Shared table/filter sizing; tooltip-only action labels. | Local table scroll, fluid filters, explicit aria labels. | [SalesOrdersPage.jsx](../frontend/src/pages/sales/SalesOrdersPage.jsx); [FilterBar.jsx](../frontend/src/components/FilterBar.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx). |
| New Sales Order `/sales-orders/new` | 1080px line-item table needs local scrolling on small screens; summary/action areas were rigid. | Quantity, rate, discount, tax, and totals are presented together. | Local horizontal scroll; responsive summary grid; full-width stacked mobile actions. | [SalesOrderFormPage.jsx](../frontend/src/pages/sales/SalesOrderFormPage.jsx). |
| Edit Sales Order `/sales-orders/:id/edit` | Same dense line-item grid; at 1441px it previously needed about 23px of local scroll. | Fixed table width plus page/paper padding. | Reduce item-section padding at desktop; retain local scroll below desktop. | [SalesOrderFormPage.jsx](../frontend/src/pages/sales/SalesOrderFormPage.jsx). |
| Sales detail dialog | Transaction item table can exceed narrow modal content. | Multi-column line items. | Responsive modal width and inner table scroll. | [theme.js](../frontend/src/theme.js); [SalesOrderViewDialog.jsx](../frontend/src/pages/sales/SalesOrderViewDialog.jsx). |
| Customer Payments `/customer-payments` | Filter and table density on narrow screens. | Shared filter/table components. | Stack filters and keep table scroll local; add accessible payment action name. | [FilterBar.jsx](../frontend/src/components/FilterBar.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx); [PaymentsPage.jsx](../frontend/src/pages/payments/PaymentsPage.jsx). |
| Supplier Payments `/supplier-payments` | Same shared filter/table density. | Shared payment list/dialog. | Stack filters, locally scroll the table, and preserve full-width dialog actions. | [FilterBar.jsx](../frontend/src/components/FilterBar.jsx); [PaymentsPage.jsx](../frontend/src/pages/payments/PaymentsPage.jsx); [theme.js](../frontend/src/theme.js). |
| Expenses `/expenses` | Search and filters can crowd; attachment controls may wrap. | Shared filters and dialog content. | Fluid filter row, wrapping attachment controls, responsive dialog actions. | [FilterBar.jsx](../frontend/src/components/FilterBar.jsx); [ExpensesPage.jsx](../frontend/src/pages/expenses/ExpensesPage.jsx); [theme.js](../frontend/src/theme.js). |
| Customer Statement `/accounts/customer-statement` | Many filters can make the report header dense. | Generic report filter grid and table. | 1/2/4 filter columns and local report table scrolling. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Supplier Statement `/accounts/supplier-statement` | Same generic report layout. | Shared report page/table. | Responsive filter grid and export stack. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Receivables `/accounts/receivables` | Wide report data may exceed small viewport. | Shared report table. | Keep report table scroll local and controls within page width. | [ReportTable.jsx](../frontend/src/components/ReportTable.jsx); [theme.js](../frontend/src/theme.js). |
| Payables `/accounts/payables` | Wide report and export actions. | Shared report header/table. | Stack export actions on mobile; local table scroll. | [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Purchase Statement `/reports/purchase-statement` | Five filters plus actions could form an overly wide row. | Filter grid and search controls. | Stack mobile filters; 2 columns on tablet and 4 on larger layouts. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [FormFields.jsx](../frontend/src/components/FormFields.jsx). |
| Sales Statement `/reports/sales-statement` | Multiple filters and report columns. | Shared report grid/table. | Responsive filter grid; inner report table scroll. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Material Profit `/reports/material-profit` | Date filter controls could crowd narrow widths. | Shared report grid. | One column mobile, two tablet, four larger screens. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx). |
| Current Stock `/reports/stock-current` | Report columns and exports. | Shared report renderer. | Stack exports on mobile; scroll table within report panel. | [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Material-wise Stock `/reports/stock-material-wise` | Filter/report grid could be unnecessarily wide. | Shared report grid/table. | Fluid filter and scrollable report table. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Stock Movement `/reports/stock-movement` | Date/material filters plus wide movement table. | Shared report layout. | Responsive filters and local table scrolling. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Customer Payment Report `/reports/customer-payments` | Date/customer filters and data table. | Shared report components. | Mobile stacked actions and scroll-contained table. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Supplier Payment Report `/reports/supplier-payments` | Date/supplier filters and data table. | Shared report components. | Mobile stacked actions and scroll-contained table. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Expense Report `/reports/expense-report` | Date/type filters and export actions. | Shared report components. | Responsive filter grid and export stack. | [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Profit & Loss `/reports/profit-loss` | Summary/actions can crowd mobile; filter row used default breakpoints. | P&amp;L-specific Grid and shared report table. | 1/2/4 filter/summary columns and full-width Generate on mobile. | [ProfitLossPage.jsx](../frontend/src/pages/reports/ProfitLossPage.jsx); [ReportTable.jsx](../frontend/src/components/ReportTable.jsx). |
| Users `/users` | Shared CRUD table/filter layout. | Generic master CRUD component. | Fluid filters, local table scroll, labeled row actions. | [MasterCrudPage.jsx](../frontend/src/pages/masters/MasterCrudPage.jsx); [DataTable.jsx](../frontend/src/components/DataTable.jsx). |
| Profile `/profile` | Form card and save action could feel cramped on phones. | Fixed padding and auto-width submit button. | Fluid card width/padding and full-width mobile action. | [ProfilePage.jsx](../frontend/src/pages/profile/ProfilePage.jsx). |
| Change Password `/change-password` | Form card and action sizing. | Fixed padding and auto-width button. | Fluid card width/padding and full-width mobile action. | [ChangePasswordPage.jsx](../frontend/src/pages/profile/ChangePasswordPage.jsx). |
| Vehicle Management | Page not present. | No route, component, API, or data model in the workspace. | Implement the module before responsive QA. | No code change possible in existing responsive surfaces. |
| Delivery Management | Page not present. | No route, component, API, or data model in the workspace. | Implement the module before responsive QA. | No code change possible in existing responsive surfaces. |
| Settings | No standalone Settings route. | Profile and Change Password are the available account settings pages. | Treat those pages as settings until a separate module is added. | [ProfilePage.jsx](../frontend/src/pages/profile/ProfilePage.jsx); [ChangePasswordPage.jsx](../frontend/src/pages/profile/ChangePasswordPage.jsx). |

## Component-Wise Fix List

- **Breakpoints:** [theme.js](../frontend/src/theme.js) now defines `xs=0`, `sm=768`, `md=1024`, `lg=1441`, `xl=1920`, matching the requested device bands.
- **Navigation:** [MainLayout.jsx](../frontend/src/layouts/MainLayout.jsx) now switches from temporary hamburger drawer to permanent sidebar at 1024px, constrains drawer width to 85vw on phones, scales toolbar/title spacing, and exposes `aria-expanded` on grouped navigation.
- **Dashboard/inventory grids:** [DashboardPage.jsx](../frontend/src/pages/dashboard/DashboardPage.jsx) and [InventoryPage.jsx](../frontend/src/pages/inventory/InventoryPage.jsx) use 1/2/4/6 card columns; charts use one mobile column and two columns from tablet.
- **Touch targets and dialogs:** [theme.js](../frontend/src/theme.js) sets 44px buttons/icon/toggle controls, 95vw mobile dialogs, capped 70vw desktop dialogs, stacked mobile dialog actions, and responsive pagination.
- **Filters and headers:** [FilterBar.jsx](../frontend/src/components/FilterBar.jsx), [SearchBar.jsx](../frontend/src/components/SearchBar.jsx), and [PageHeader.jsx](../frontend/src/components/PageHeader.jsx) wrap controls and use full-width mobile actions.
- **Tables:** [DataTable.jsx](../frontend/src/components/DataTable.jsx) and [ReportTable.jsx](../frontend/src/components/ReportTable.jsx) constrain width and explicitly allow local horizontal scrolling.
- **Purchase/sales forms:** [PurchaseOrderFormPage.jsx](../frontend/src/pages/purchase/PurchaseOrderFormPage.jsx) and [SalesOrderFormPage.jsx](../frontend/src/pages/sales/SalesOrderFormPage.jsx) reduce mobile padding, keep line-item scrolling local, make add-item and bottom actions touch-friendly, and collapse sales totals on mobile.
- **Reports/settings:** [ReportPage.jsx](../frontend/src/pages/reports/ReportPage.jsx), [ProfitLossPage.jsx](../frontend/src/pages/reports/ProfitLossPage.jsx), [ProfilePage.jsx](../frontend/src/pages/profile/ProfilePage.jsx), and [ChangePasswordPage.jsx](../frontend/src/pages/profile/ChangePasswordPage.jsx) now stack their filters/actions and use fluid cards.
- **Accessibility:** Explicit `aria-label`s were added to row and line-item icon controls in master, expense, payment, PO, and sales pages.

## Responsive MUI Patterns

This application uses Material UI `sx`/theme styling; it does not use Tailwind or a standalone responsive CSS file. The primary patterns are:

```js
breakpoints: {
  values: { xs: 0, sm: 768, md: 1024, lg: 1441, xl: 1920 },
}
```

```jsx
<Grid item xs={12} sm={6} md={3} xl={2}>
  <StatCard />
</Grid>
```

```jsx
<Box sx={{
  display: 'flex',
  flexDirection: { xs: 'column-reverse', sm: 'row' },
  '& .MuiButton-root': { width: { xs: '100%', sm: 'auto' }, minHeight: 44 },
}}>
  {/* form actions */}
</Box>
```

Dense transaction tables retain their necessary columns and scroll locally on narrow viewports rather than expanding the document.

## Scores

These are heuristic scores for **implemented pages only**, based on viewport overflow checks, measured grids/forms/navigation/dialogs, touch targets, and remaining dense-table behavior. They are not a WCAG certification or a pixel-by-pixel visual assessment.

| Category | Score | Basis |
|---|---:|---|
| Mobile | 96 / 100 | 30 routes have no page overflow; login, filters, dialogs, and order forms fit. Dense line-item tables scroll locally, and not every validation state was visually reviewed. |
| Tablet | 97 / 100 | Hamburger drawer and 2-column dashboard/inventory/report grids work from 768px; all measured routes remain within viewport. |
| Laptop | 97 / 100 | Four-column KPI/card grids begin at 1024px; no page overflow. Dense transaction tables use local scroll where the available content area is narrower than the table. |
| Desktop | 98 / 100 | Permanent sidebar, four dashboard columns at 1441px, six at 1920px; sales item grid fits at 1441px and above. |

## Remaining Verification Limits

- The 300-case sweep checks document/body width, not every possible data value, device pixel ratio, browser font rendering, or all modal/form states.
- Login and reset-password were spot-checked at 320px; reset-password showed the expected invalid/missing-token state, so the valid-token form still needs a token-driven check.
- Contrast was not measured with an automated WCAG tool; keyboard/screen-reader labels were improved but no full assistive-technology audit was run.
- Performance profiling on low-end devices was not performed. No manual resize listeners were added; responsive behavior uses MUI breakpoints and CSS.
- Vehicle Management, Delivery Management, and standalone Settings pages do not exist in the current route tree, so they cannot receive a responsive score.
