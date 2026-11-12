# Crusher Stone Supply Management System (CSSMS)

Production-ready ERP for a stone / crusher material supply business: purchases, inventory, sales & invoices, customer/supplier payments, expenses, accounts, reports and a dashboard, with role-based access (Admin, Manager, Sales Executive).

| | |
|---|---|
| Backend | Java 17, Spring Boot 3.2, Spring Security + JWT (refresh tokens), Spring Data JPA/Hibernate, Flyway, Bean Validation, springdoc OpenAPI, Apache POI (Excel), OpenPDF (PDF), Lombok, Maven |
| Frontend | React 18 (Vite), Material UI, React Router, Axios, React Hook Form, Context API, Recharts |
| Database | PostgreSQL |
| Packaging | Docker Compose (db + backend + nginx/frontend) |

## Repository layout

```
backend/            Spring Boot API (src/main/resources/db/migration = Flyway DDL)
frontend/           React SPA (+ Dockerfile, nginx.conf)
docs/               API.md · ARCHITECTURE.md · ER-DIAGRAM.md · DEPLOYMENT.md
docker-compose.yml  one-command deployment
.env.example        deployment configuration template
```

## Quick start (Docker)

```bash
cp .env.example .env          # set DB_PASSWORD, JWT_SECRET, APP_ADMIN_PASSWORD
# optional: SPRING_PROFILES_ACTIVE=prod,demo   -> loads sample data on an empty database
docker compose up -d --build
```

Open `http://localhost` and sign in with the admin credentials from `.env` (with the `demo` profile also `manager / Manager@123` and `sales / Sales@123`).

## Local development

```bash
# 1. PostgreSQL (database/user "cssms"/"cssms" are the defaults; override with DB_URL, DB_USERNAME, DB_PASSWORD)
# 2. Backend  (http://localhost:8080, Swagger UI at /swagger-ui.html)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=demo      # demo = sample data + dev conveniences
# 3. Frontend (http://localhost:5173, proxies /api to :8080)
cd frontend
npm install
npm run dev
```

Default bootstrap admin (non-prod): `admin / Admin@123`.

## Tests

```bash
cd backend && mvn test
```

The integration tests start a real embedded PostgreSQL (no Docker required) and cover migrations, security/role matrix, the purchase → sale → payment → cancel lifecycle, reports and PDF/Excel export.

## Modules

Authentication (login, logout, refresh, change/forgot/reset password, profile) · Users · Employees · Suppliers · Customers · Material master (Dust, 10 MM, 20 MM, 40 MM, Boulder, M-Sand, P-Sand pre-loaded) · Purchase orders · Inventory · Sales orders & invoice PDF · Customer payments · Supplier payments · Expenses (with attachments) · Accounts (customer/supplier statements, receivable/payable aging) · Reports (purchase, sales, material profit, stock, payments, expenses, profit & loss; PDF + Excel export) · Dashboard (9 cards, 6 charts).

## Documentation

* [REST API reference and role matrix](docs/API.md) – also available live as Swagger UI
* [Architecture, security and business rules](docs/ARCHITECTURE.md)
* [ER diagram](docs/ER-DIAGRAM.md)
* [Production deployment guide](docs/DEPLOYMENT.md)
