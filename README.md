# SebayaDM

Diabetes self-management platform built around **peer groups (kelompok sebaya)** — helping
Diabetes Mellitus patients improve diet adherence and self-care. Based on the DIACARE
reference prototype and the research design in `_references/`.

## Components

| Component | Stack | Location |
|---|---|---|
| Backend API + Admin panel | Go (chi + sqlc) monolith, MariaDB | this repo |
| Admin panel (`/admin`) | Server-rendered Go `html/template` + HTMX + custom Tailwind CSS (Lucide icons) | `internal/app/admin`, `template/`, `static/css/admin.src.css` |
| REST API (`/api`) | JSON, JWT (Bearer) | `internal/app/api` |
| Patient app | Android (Kotlin + Jetpack Compose) | `android/` |

## Features

Five daily self-management pillars + peer support:
- **Konsumsi Obat** (medication), **Aktivitas Fisik** (activity), **Catatan Diet** (diet) —
  daily yes/no logs, upserted per day.
- **Catatan Gula Darah** (blood glucose) — numeric mg/dL readings with timing.
- **Edukasi Diabetes** — admin-managed articles.
- **Kelompok Sebaya leaderboard** — patients ranked within their group by number of
  fully-compliant days over the last 30 days.
- Admin: patient (responden) management, group assignment + study arm (intervensi/kontrol),
  education CRUD, adherence monitoring, CSV export for research, audit trail.

## Backend — run locally

Requires Go 1.25+, a MariaDB/MySQL server, `sqlc`, `golang-migrate`, and Node 18+/npm
(for the admin CSS build).

```bash
cp .env.example .env          # then edit DB_* and JWT_SECRET
npm install                   # one-time: install the Tailwind CLI
make css                      # build static/css/admin.css from admin.src.css
make sqlc                     # regenerate internal/database/sqlc (only after query/schema changes)
make migrate-up               # apply migrations
go run cmd/genhash/main.go <password>   # make a bcrypt hash for the admin seed
make dev                      # run with live reload (air); in a 2nd terminal: make css-watch
```

The admin panel is styled with a **customized Tailwind CSS** build (no daisyUI). Source lives in
`static/css/admin.src.css` — the emerald theme, Sora/Plus Jakarta Sans fonts, and the in-house
component classes (`btn`, `card`, `input`, `drawer`, …). `make build` regenerates `admin.css`
before compiling, and the generated file is committed so a plain `go build` works without Node.
Editing templates or `admin.src.css` requires re-running `make css` (or `make css-watch`).

Seed an admin user (role `admin`) directly, e.g.:

```sql
INSERT INTO `user` (role, nama_lengkap, usia, jenis_kelamin, username, password_hash)
VALUES ('admin','Administrator',40,'L','admin','<bcrypt-hash>');
```

- Admin panel: <http://localhost:8080/admin>
- API base: <http://localhost:8080/api>

### Key API endpoints

```
POST /api/auth/register            POST /api/auth/login          GET  /api/me
GET  /api/dashboard                (today's pillar status)
POST /api/logs/medication|activity|diet|glucose   + GET history
GET  /api/education  ·  GET /api/education/{slug}
GET  /api/leaderboard              (caller's peer group)
```

All `/api` routes except auth + education require `Authorization: Bearer <jwt>`.

## Android app — build & run

Open `android/` in Android Studio, or from the CLI (JDK 17+, Android SDK 35):

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

The app targets `http://10.0.2.2:8080/api/` (host machine from the Android emulator) — see
`API_BASE_URL` in `android/app/build.gradle.kts`. Run the backend, launch an emulator, install
the debug APK, then register/login and use the daily-log screens.

## Architecture notes

- **Store + audit trail**: every mutation goes through a `*Tx` method in
  `internal/database/store` that runs the query and an `audit_trail` insert in one transaction.
- **Timestamps** are `DATETIME` (not Unix ints); **migrations** use golang-migrate (`sqlc`
  reads the `.up.sql` files, ignores `.down.sql`).
- **Auth**: JWT — Bearer header for the API, HTTP-only `admin_token` cookie for the panel.
