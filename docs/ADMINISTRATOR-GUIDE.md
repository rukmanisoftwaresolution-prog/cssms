# Stone / Crusher Order Supply Management System
## Administrator Guide

**Version:** 1.0.0  
**Prepared by:** [Prepared By]  
**Date:** 2026-10-29  
**Company:** [Company Name]  
**Audience:** CSSMS administrators and deployment/operations personnel.

> **Product scope:** This guide reflects the current repository. It configures PostgreSQL, Admin/Manager/Sales Executive roles, and the modules present in the UI/API. There is no UI for role-definition editing, vehicle/delivery management, PO approval, sales returns, opening stock, or a standalone Settings module.

<!-- pagebreak -->

## Table of Contents

1. Administrator Responsibilities
2. System Prerequisites
3. Initial Deployment and Bootstrap
4. First Sign-In and User Setup
5. User Management
6. Roles and Permissions
7. Master Data Setup
8. Operational Configuration
9. Reports and Dashboard Configuration
10. Audit Logs
11. Backup and Restore
12. Security Settings
13. Environment Variables
14. Upgrades and Database Migrations
15. Troubleshooting
16. Operational Checklist
17. Unsupported/Unconfigured Capabilities

## 1. Administrator Responsibilities

The administrator is responsible for named user accounts, access reviews, master data quality, secure environment configuration, backups, restore tests, upgrades, and operational monitoring. Use individual accounts. Do not share the bootstrap admin account.

The Admin role can access all configured screens/actions. It does not create new role types; role permissions are defined in application code.

> [Screenshot placeholder: Admin landing dashboard]

## 2. System Prerequisites

### Production deployment

- Linux or Windows Server host with Docker Engine 24+ and Docker Compose v2.
- DNS and TLS certificate; terminate TLS at a reverse proxy/load balancer in front of the frontend.
- PostgreSQL 16 container in the supplied compose configuration. The backend supports PostgreSQL; MySQL is not configured.
- Persistent storage for both `pgdata` and `uploads` Docker volumes.

### Local development

- Java 17 and Maven 3.6.3+.
- PostgreSQL 14+ for a local backend database.
- Node.js/npm for the Vite frontend.

### Browser/access

Current Chrome, Edge, or Firefox; JavaScript enabled; allow application PDF/Excel downloads. Production UI/API should be published only through the approved HTTPS endpoint.

## 3. Initial Deployment and Bootstrap

### 3.1 Prepare environment

1. Copy `.env.example` to `.env` on the deployment host.
2. Restrict `.env` access; never commit it to source control.
3. Set a strong `DB_PASSWORD`, a `JWT_SECRET` of at least 32 characters, and `APP_ADMIN_PASSWORD`.
4. Set `APP_ADMIN_USERNAME` and `APP_ADMIN_EMAIL` if required.
5. Set `COMPANY_NAME`, `COMPANY_ADDRESS`, `COMPANY_PHONE`, and `COMPANY_GSTIN` for invoice branding.
6. Use `SPRING_PROFILES_ACTIVE=prod` for production. Do not enable `demo` with real data.
7. Set `CORS_ALLOWED_ORIGINS` only when UI and API are on different origins.

### 3.2 Start and verify

From the deployment directory:

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend
```

Flyway migrations run on backend startup. Verify the backend health endpoint at `http://localhost:8080/actuator/health` from the host, and open the configured web port in a browser. The database port is not published by the supplied compose file; keep it private.

> [Screenshot placeholder: Docker Compose services healthy]

### 3.3 Bootstrap administrator behavior

The bootstrap administrator is created only when the user table is empty. Configure its password before first startup and change it after first sign-in. If the user table already contains accounts, changing the bootstrap password environment variable does not reset an existing admin account.

Demo accounts are for local/test demo profile only: `admin / Admin@123`, `manager / Manager@123`, `sales / Sales@123`. Do not deploy the demo profile to production.

## 4. First Sign-In and User Setup

1. Open `<production-url>/login`.
2. Sign in with the bootstrap Admin credentials.
3. Open the profile menu and change the password.
4. Create named Manager and Sales Executive users under **Users**.
5. Confirm the role-specific menus and permissions with test accounts before handover.
6. Verify company details on a generated sales invoice.

There is no standalone Settings page. Application-wide configuration is supplied through environment variables and deployment settings.

> [Screenshot placeholder: First sign-in and profile menu]

## 5. User Management

Only Admin can access **Users**.

### Create a user

1. Open **Users** and select **Add User**.
2. Enter a unique username (3–50 characters; letters, digits, dot, underscore, hyphen), full name, email, and mobile number.
3. Select one of the configured roles: Admin, Manager, Sales Executive.
4. Set a password with at least 8 characters and uppercase, lowercase, and digit; maximum length is 100.
5. Confirm Active is enabled, then Save.
6. Ask the user to sign in and change the temporary password.

### Update, deactivate, or delete

1. Search for the account and select Edit to update profile, role, active state, or password.
2. Deactivate accounts that should no longer sign in; deactivation takes effect on subsequent requests and revokes refresh sessions.
3. Delete is a soft delete; it removes the account from normal lists but preserves audit/history references.
4. Do not delete the only active administrator. Maintain at least two controlled administrator accounts where policy permits.

> [Screenshot placeholder: Users list, Add User dialog, and role selector]

## 6. Roles and Permissions

The application defines exactly three role names in code. The role matrix is not editable in the UI.

| Module / action | Admin | Manager | Sales Executive |
|---|---|---|---|
| Dashboard | View | View | View |
| Users | Create/read/update/delete | No access | No access |
| Employees | Create/read/update/delete | Read | No access |
| Suppliers | Create/read/update/delete | Create/read/update/delete | No access |
| Customers | Create/read/update/delete | Create/read/update/delete | Create/read/update; no delete |
| Materials | Create/read/update/delete | Create/read/update/delete | Read |
| Purchase Orders | Create/read/update/cancel | Create/read/update/cancel | No access |
| Inventory | Read | Read | Read |
| Sales Orders | Create/read/update/cancel | Create/read/update/cancel | Create/read/update; no cancel |
| Customer Payments | Create/read/delete/reverse | Create/read/delete/reverse | Create/read; no delete |
| Supplier Payments | Create/read/delete/reverse | Create/read/delete/reverse | No access |
| Expenses | Create/read/update/delete | Create/read/update/delete | No access |
| Customer Statement / Receivables | Read | Read | Read |
| Supplier Statement / Payables | Read | Read | No access |
| Sales Statement | Read | Read | Read |
| Other Reports | Read | Read | No access |

“Delete” for records is usually a soft delete or reversal action. Transaction-specific rules may block edits/cancellations until payments are reversed. See [API.md](API.md) for the canonical matrix and endpoint behavior.

**Permission configuration:** Access rules are code-defined in the frontend permission map and Spring Security method annotations. There is no UI or database-backed role/permission editor. Changes require coordinated code changes, tests, and deployment.

**Not configured:** Accountant, Purchase User, Inventory User, Dispatch User, and Read Only User are not role values. Do not present these as assignable roles. If required, request a permissions design and code change.

## 7. Master Data Setup

Complete reference data before users create transactions.

1. **Materials:** Verify preloaded materials, units, and names. Codes are generated (`MAT-####`). Duplicate material names are rejected. Used materials cannot be deleted.
2. **Suppliers:** Enter supplier name, contact person, 10-digit mobile, address, and optional GST/PAN/bank details. Codes are generated (`SUP-####`). Referenced suppliers should be marked Inactive instead of deleted.
3. **Customers:** Enter customer name, 10-digit mobile, required address, non-negative credit limit, and optional company/GST/email. Codes are generated (`CUST-####`). A credit limit of zero means unlimited. Customer duplicate mobile/GST values are not currently prevented; search before adding and define a uniqueness rule with the product owner.
4. **Employees:** Admin creates and maintains employee master records; Manager has read-only access.
5. Confirm status is Active before transactions use a customer/supplier.

No Vehicle, Driver, Delivery, or Stock Adjustment master exists.

### Reference Form Fields

| Record | Required fields | Optional fields / notes |
|---|---|---|
| Employee | Full name, 10-digit mobile, joining date, non-negative salary. | Email, address (max 500), designation (max 100), status. Code is generated as `EMP-####`. |
| Material | Material name (max 100). | Description (max 500), unit (max 20; commonly TON). Code is generated as `MAT-####`; duplicate names are rejected. |
| Customer | Name, 10-digit mobile, address (max 500), non-negative credit limit. | Company, GSTIN, email, status. Duplicate customer mobile/GST is not prevented. |
| Supplier | Name, contact person, 10-digit mobile, address. | GSTIN, PAN, bank details (max 500), status. |
| Expense | Date, type, amount greater than zero. | Description (max 500), PDF/PNG/JPG attachment up to 5 MB. |

## 8. Operational Configuration

### Company/invoice details

Set `COMPANY_NAME`, `COMPANY_ADDRESS`, `COMPANY_PHONE`, and `COMPANY_GSTIN` in the deployment environment. Generate a test invoice and verify values before production use.

### Time zone

`APP_TIMEZONE` controls the meaning of “today” on the dashboard and date-based calculations. The default is `Asia/Kolkata`; align it with company policy before go-live.

### File uploads

Expense attachments support PDF, PNG, and JPG up to 5 MB. Configure persistent `uploads` storage and monitor free disk space. The application stores generated file paths; include the uploads volume in backups.

## 9. Reports and Dashboard Configuration

Dashboard cards and report definitions are application-defined, not user-configurable. The dashboard calculates:

- Today's Purchase/Sales: sum of dated purchase/sales order totals.
- Today's Collection/Supplier Payment: sum of payments dated today.
- Current Inventory: total current quantities across materials.
- Receivables/Payables: pending sales/purchase balances.
- Monthly Profit/Loss: current month's net result, shown in the positive card only.

The dashboard trends cover six months; top customer/supplier lists return up to five rows. Report filters, columns, and calculations are defined in the code/API; there is no report builder or column configuration screen. Reports export to PDF and XLSX. Swagger/OpenAPI is disabled in production unless temporarily enabled by configuration.

## 10. Audit Logs

State-changing API requests and authentication events are recorded in the `audit_logs` table. The current UI has no Audit Logs screen. Authorized database/operations personnel can query the table using the organization’s read-only access procedure. Restrict log access because records can contain usernames, paths, entity identifiers, and client metadata. Follow retention and privacy policy.

## 11. Backup and Restore

Back up both the PostgreSQL database volume and uploads volume. The deployment guide provides these examples:

```bash
# Database backup
docker compose exec -T db pg_dump -U cssms -Fc cssms > backups/cssms-YYYY-MM-DD.dump

# Uploads backup (replace volume name with the actual Docker volume)
docker run --rm -v cssms_uploads:/data -v "$PWD/backups":/b alpine \
  tar czf /b/uploads-YYYY-MM-DD.tgz -C /data .
```

Restore is destructive to the target database. Confirm target environment, maintenance window, and backup integrity before running:

```bash
docker compose exec -T db pg_restore -U cssms -d cssms --clean --if-exists \
  < backups/cssms-YYYY-MM-DD.dump
```

Restore the matching uploads archive as well. Test restoration regularly in a non-production environment. Do not overwrite a production volume without a verified backup and approved change window.

## 12. Security Settings

- Terminate TLS at an approved reverse proxy/load balancer; publish only the proxy to the internet.
- Keep PostgreSQL private; the supplied compose file does not publish its port.
- Keep `.env` out of source control and restrict file permissions.
- Use a strong `JWT_SECRET` (at least 32 characters). Changing it invalidates active sessions.
- Use strong administrator passwords and named accounts. Deactivate departed users promptly.
- Set exact `CORS_ALLOWED_ORIGINS`; avoid wildcard origins in production.
- Swagger is disabled by default in production. Enable only temporarily and through controlled access.
- Forgot-password email/SMS delivery is an integration hook; do not enable token exposure in production.
- Uploads enforce extension/path checks and size limits. Keep application and dependencies patched.

## 13. Environment Variables

Review `.env.example` and `docker-compose.yml` for defaults. Common settings include:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` by default; `demo` loads sample data only into an empty database. |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL JDBC connection and credentials. |
| `JWT_SECRET` | Signing secret, minimum 32 characters. |
| `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`, `APP_ADMIN_EMAIL` | Initial admin bootstrap values. |
| `CORS_ALLOWED_ORIGINS` | Allowed browser origins when UI/API are cross-origin. |
| `SWAGGER_ENABLED` | Controls API docs in production. |
| `COMPANY_NAME`, `COMPANY_ADDRESS`, `COMPANY_PHONE`, `COMPANY_GSTIN` | Invoice company header. |
| `APP_TIMEZONE` | Business date/time zone; default `Asia/Kolkata`. |
| `WEB_PORT` | Host port for the frontend container. |
| `UPLOAD_DIR` | Backend attachment storage path when running outside Compose. |
| `SERVER_PORT`, `DB_POOL_SIZE`, `JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS`, `APP_LOG_LEVEL` | Backend service/pool/token/log settings. |

MySQL settings are not supported by the checked-in migrations/configuration.

## 14. Upgrades and Database Migrations

1. Take a verified database and uploads backup.
2. Review release notes and migration SQL.
3. Deploy the new application image/build.
4. Confirm Flyway migration completion in backend logs and health endpoint.
5. Smoke-test login, purchase/sales lists, inventory, and reports.
6. Do not edit an already-applied migration. Add a new versioned migration.
7. Rolling back application code after a migration may require restoring the pre-upgrade backup.

## 15. Troubleshooting

| Issue | Likely cause | Administrator action |
|---|---|---|
| Backend unhealthy | PostgreSQL unavailable, invalid DB credentials, migration failure. | Check `docker compose ps`, backend/db logs, DB health, and migration history. |
| Login rejected | Wrong credentials, inactive user, invalid JWT configuration. | Confirm user active and reset through approved process; never request the user's password. |
| 401 responses | Access/refresh token expired or refresh session revoked. | Sign in again; investigate repeated 401s/session-refresh logs. |
| 403 response | Role lacks module/action permission. | Confirm the fixed role matrix; do not workaround with a shared account. |
| 409 response | Duplicate material, insufficient stock/credit, payments block edit/cancel, or referenced master. | Read the message, correct source transaction/payment/status, then retry. |
| Report/export failure | Invalid filter, API outage, browser download policy. | Test API health, date parameters, backend logs, and browser download settings. |
| Attachment failure | Disallowed type/size, storage permissions, disk full. | Confirm PDF/PNG/JPG <=5MB, upload volume permissions, and free space. |
| Missing menu | Role is not allowed or feature is absent. | Check role matrix and route tree; Vehicle/Delivery/Settings are not implemented modules. |

## 16. Operational Checklist

- [ ] Production uses `prod`, not the demo profile.
- [ ] Bootstrap admin password changed; named accounts created.
- [ ] Only Admin/Manager/Sales Executive roles assigned according to policy.
- [ ] TLS and exact CORS origins configured.
- [ ] PostgreSQL remains private; `.env` is protected.
- [ ] Company invoice details and time zone verified.
- [ ] Database and uploads backups scheduled; restore tested.
- [ ] Health endpoint, logs, disk, `pgdata`, and uploads monitored.
- [ ] API docs disabled or access-controlled.
- [ ] Feature gaps (duplicate customer prevention, vehicle/delivery, sales returns, opening stock, extra roles) are accepted or tracked.

## 17. Unsupported/Unconfigured Capabilities

The current build does not provide configurable roles, Accountant/Purchase/Inventory/Dispatch/Read-only role definitions, Vehicle/Driver management, Delivery tracking, PO approval, sales returns, opening-stock entry, stock adjustment, a standalone Settings screen, or an Audit Logs UI. Audit events are stored in the database; product roles/permissions are code-defined. Route/API features should be implemented and regression-tested before documenting them as available.
