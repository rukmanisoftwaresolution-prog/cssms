# CSSMS Frontend

React 18 + Vite + Material UI v5 frontend for the Crusher Stone Supply Management System.
API contract: `../docs/API.md`.

## Run

```bash
npm install
npm run dev        # http://localhost:5173 (proxies /api -> http://localhost:8080)
npm run build      # production build in dist/
```

Copy `.env.example` to `.env` to change `VITE_API_BASE_URL` (default `/api`).

## Docker

```bash
docker build -t cssms-frontend .
docker run -p 80:80 cssms-frontend   # nginx proxies /api/ to http://backend:8080
```

## Layout

`src/api` axios client + one service per backend area, `src/context` Auth/Notification,
`src/routes` protected & role routes, `src/layouts` main shell, `src/components` reusable UI,
`src/hooks` table/lookup hooks, `src/utils` formatters/constants (role matrix)/validators, `src/pages` screens.

Demo logins (demo profile): `admin / Admin@123`, `manager / Manager@123`, `sales / Sales@123`.
