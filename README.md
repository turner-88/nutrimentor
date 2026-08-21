# NutriMentor

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

Requires Go 1.25+, a MariaDB/MySQL server, `sqlc`, and `golang-migrate`. The admin CSS
build uses the standalone Tailwind CLI, which `make css` downloads automatically — no
Node.js required.

```bash
cp .env.example .env          # then edit DB_* and JWT_SECRET
make css                      # download the Tailwind CLI + build static/css/admin.css from admin.src.css
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

## Deploy to production

Deployment runs the Go binary as a systemd service behind nginx (TLS via Let's Encrypt),
targeting `https://nutrimentor.remorac.com`. Everything lives in `deploy/`:

| File | Purpose |
|---|---|
| `deploy/setup.sh` | One-time provisioning (Debian/Ubuntu): installs Go/nginx/certbot/migrate, creates the `nutrimentor` user + `/opt/nutrimentor/app` checkout, the MariaDB DB/user, `.env`, the systemd unit, the nginx site, and the TLS cert, then runs the first deploy. |
| `deploy/deploy.sh` | Repeatable deploy: `git pull` → `make css` + `make build` → `make migrate-up` → restart service → health-check. |
| `deploy/nutrimentor.service` | systemd unit (`WorkingDirectory=/opt/nutrimentor/app`, `EnvironmentFile=.env`). |
| `deploy/nginx/nutrimentor.remorac.com.conf` | nginx reverse proxy → `127.0.0.1:8081`. |
| `.env.production.example` | Production env template (bind to loopback, `ENV=production`). |
| `deploy/decommission-sebayadm.sh` | One-time cleanup of the retired `sebayadm.remorac.com` host (old `sebaya` service/user/DB, nginx site, TLS cert, `/opt/sebaya`). Backs up the DB + app dir before deleting; the DNS record must be removed manually. |

First-time provisioning (point the DNS A record at the server first):

```bash
git clone https://github.com/remorac/nutrimentor-app.git /tmp/nutrimentor && cd /tmp/nutrimentor
sudo bash deploy/setup.sh          # override defaults via env, e.g. CERTBOT_EMAIL=you@example.com
```

Subsequent releases:

```bash
cd /opt/nutrimentor/app && sudo -u nutrimentor ./deploy/deploy.sh
```

The backend binds `127.0.0.1:8081` and nginx terminates TLS in front of it. Uploaded files
persist under `/opt/nutrimentor/app/static/uploads`; FCM credentials (if used) go in
`/opt/nutrimentor/app/secrets/`. Logs: `journalctl -u nutrimentor -f`.

## Android app — build & run

Open `android/` in Android Studio, or from the CLI (JDK 17+, Android SDK 35):

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug
```

`API_BASE_URL` is set per build type in `android/app/build.gradle.kts`: **debug** targets a LAN
dev host (`http://192.168…:8081/api/` — edit for your machine's IP or use `10.0.2.2` on the
emulator), **release** targets production `https://nutrimentor.remorac.com/api/`. Run the backend,
launch an emulator, install the debug APK, then register/login and use the daily-log screens.

### Release APK (signed)

Two files are required and are gitignored — provide them before building:

1. `android/app/google-services.json` — from the Firebase console (the `google-services`
   plugin fails the build without it).
2. `android/keystore.properties` — copy from `keystore.properties.example` and point it at a
   keystore you generate:

   ```bash
   cd android
   keytool -genkeypair -v -keystore nutrimentor-release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias nutrimentor
   ```

Then build the signed APK:

```bash
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleRelease
# → android/app/build/outputs/apk/release/app-release.apk
```

If `keystore.properties` is absent the release build still runs but produces an unsigned APK.

## Architecture notes

- **Store + audit trail**: every mutation goes through a `*Tx` method in
  `internal/database/store` that runs the query and an `audit_trail` insert in one transaction.
- **Timestamps** are `DATETIME` (not Unix ints); **migrations** use golang-migrate (`sqlc`
  reads the `.up.sql` files, ignores `.down.sql`).
- **Auth**: JWT — Bearer header for the API, HTTP-only `admin_token` cookie for the panel.
