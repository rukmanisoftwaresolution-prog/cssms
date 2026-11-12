# CSSMS – Production Deployment Guide

## 1. Prerequisites

* A Linux (or Windows Server) host with Docker Engine 24+ and Docker Compose v2.
* A DNS name and a TLS certificate (terminate TLS in a reverse proxy / load balancer in front of the `frontend` container, e.g. Caddy, Traefik, nginx or a cloud load balancer).
* Outbound access only needed for pulling images.

## 2. Configure

```bash
cp .env.example .env
```

Edit `.env` – at minimum:

| Variable | Notes |
|---|---|
| `DB_PASSWORD` | Strong password for the PostgreSQL `cssms` user. |
| `JWT_SECRET` | ≥ 32 random chars (`openssl rand -base64 48`). Changing it logs everybody out. |
| `APP_ADMIN_PASSWORD` | Password of the bootstrap admin (created only when the user table is empty). Change it again in the UI after first login. |
| `CORS_ALLOWED_ORIGINS` | The public URL of the UI, e.g. `https://cssms.example.com` (only used when the UI and API are on different origins). |
| `COMPANY_*` | Printed on invoices. |
| `SPRING_PROFILES_ACTIVE` | `prod` (default). `prod,demo` loads sample data into an **empty** database – never use it for real data. |

## 3. Start

```bash
docker compose up -d --build
docker compose ps                      # db, backend, frontend should be healthy/running
docker compose logs -f backend         # Flyway migrations run automatically on start
```

UI: `http://<host>:${WEB_PORT:-80}` · Health: `http://localhost:8080/actuator/health` (bound to localhost only).
Swagger UI is disabled in `prod`; set `SWAGGER_ENABLED=true` temporarily if you need it (`/swagger-ui.html`).

## 4. TLS and reverse proxy

Publish only the proxy to the internet. Example Caddyfile:

```
cssms.example.com {
    reverse_proxy frontend:80
}
```

(attach Caddy to the compose network, or point it at `localhost:${WEB_PORT}`). Set `CORS_ALLOWED_ORIGINS=https://cssms.example.com`.

## 5. Backups and restore

```bash
# nightly backup (cron)
docker compose exec -T db pg_dump -U cssms -Fc cssms > backups/cssms-$(date +%F).dump
docker run --rm -v cssms_uploads:/data -v "$PWD/backups":/b alpine tar czf /b/uploads-$(date +%F).tgz -C /data .

# restore
docker compose exec -T db pg_restore -U cssms -d cssms --clean --if-exists < backups/cssms-YYYY-MM-DD.dump
```

Back up both volumes: `pgdata` (database) and `uploads` (expense attachments). Test a restore regularly.

## 6. Upgrades

```bash
git pull
docker compose up -d --build        # new image; Flyway applies new migrations on start
```

* Never edit an applied migration; add `V3__…sql` etc.
* Take a database backup before every upgrade. Rolling back the application image after a migration ran requires restoring the backup.

## 7. Operations checklist

* [ ] `.env` is not committed and has restrictive permissions (`chmod 600 .env`).
* [ ] TLS in front of the UI; database port **not** published (it isn't in the compose file).
* [ ] Default demo users do not exist (no `demo` profile).
* [ ] Admin password changed after first login; create named Manager / Sales users in *Users*.
* [ ] Backups scheduled and restore tested.
* [ ] Logs shipped/rotated (`docker compose logs`, or configure a Docker logging driver). Audit trail is in the `audit_logs` table.
* [ ] Monitor `/actuator/health` and disk usage of the `pgdata` / `uploads` volumes.
* [ ] Time zone: `APP_TIMEZONE` (default `Asia/Kolkata`) controls "today" on the dashboard and aging calculations.

## 8. Running without Docker

Backend (Java 17, Maven 3.6.3+, PostgreSQL 14+):

```bash
createdb cssms   # and a user/password, then:
export DB_URL=jdbc:postgresql://localhost:5432/cssms DB_USERNAME=cssms DB_PASSWORD=...
export JWT_SECRET=... APP_ADMIN_PASSWORD=...
cd backend && mvn -DskipTests package
java -jar target/cssms-backend-1.0.0.jar --spring.profiles.active=prod
```

Frontend: `cd frontend && npm ci && npm run build`, serve `dist/` with any web server and proxy `/api` to the backend (see `frontend/nginx.conf`).

For local development: `mvn spring-boot:run -Dspring-boot.run.profiles=demo` (needs a local PostgreSQL) and `npm run dev` (Vite proxies `/api` to `localhost:8080`).
