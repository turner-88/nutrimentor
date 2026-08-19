#!/usr/bin/env bash
#
# One-time provisioning for a SebayaDM production host (Ubuntu/Debian).
# Installs prerequisites, creates the service user + app checkout, wires up
# systemd + nginx + a Let's Encrypt cert, and runs the first deploy.
#
# Run as root:  sudo bash deploy/setup.sh
# Idempotent — safe to re-run. Prompts before overwriting an existing .env.
#
# Assumptions:
#   * Debian/Ubuntu with apt and systemd.
#   * DNS A/AAAA record for DOMAIN already points at this server (needed by certbot).
#   * A MariaDB/MySQL server is reachable (installed here if missing).
set -euo pipefail

# ------------------------------------------------------------------ config ----
DOMAIN="${DOMAIN:-sebayadm.remorac.com}"
APP_USER="${APP_USER:-sebaya}"
APP_DIR="${APP_DIR:-/opt/sebaya/app}"
REPO_URL="${REPO_URL:-https://github.com/remorac/sebaya-app.git}"
GO_VERSION="${GO_VERSION:-1.25.1}"
NODE_MAJOR="${NODE_MAJOR:-20}"
DB_NAME="${DB_NAME:-sebaya}"
DB_USER="${DB_USER:-sebaya}"
CERTBOT_EMAIL="${CERTBOT_EMAIL:-}"   # optional; used for cert expiry notices

if [ "$(id -u)" -ne 0 ]; then
    echo "Please run as root (sudo bash deploy/setup.sh)." >&2
    exit 1
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "==> Installing OS packages"
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y \
    git curl ca-certificates build-essential \
    nginx mariadb-server mariadb-client \
    certbot python3-certbot-nginx

# ------------------------------------------------------------------- Go -------
if ! command -v go >/dev/null 2>&1 || [ "$(go version 2>/dev/null | awk '{print $3}')" != "go${GO_VERSION}" ]; then
    echo "==> Installing Go ${GO_VERSION}"
    arch="$(dpkg --print-architecture)"   # amd64 / arm64
    curl -fsSL "https://go.dev/dl/go${GO_VERSION}.linux-${arch}.tar.gz" -o /tmp/go.tar.gz
    rm -rf /usr/local/go
    tar -C /usr/local -xzf /tmp/go.tar.gz
    rm -f /tmp/go.tar.gz
    ln -sf /usr/local/go/bin/go /usr/local/bin/go
    ln -sf /usr/local/go/bin/gofmt /usr/local/bin/gofmt
fi
go version

# ------------------------------------------------------------------ Node ------
if ! command -v node >/dev/null 2>&1; then
    echo "==> Installing Node ${NODE_MAJOR}.x"
    curl -fsSL "https://deb.nodesource.com/setup_${NODE_MAJOR}.x" | bash -
    apt-get install -y nodejs
fi
node --version

# --------------------------------------------------------------- migrate ------
if ! command -v migrate >/dev/null 2>&1; then
    echo "==> Installing golang-migrate CLI"
    arch="$(dpkg --print-architecture)"
    curl -fsSL "https://github.com/golang-migrate/migrate/releases/latest/download/migrate.linux-${arch}.tar.gz" \
        | tar -xz -C /tmp migrate
    install -m 0755 /tmp/migrate /usr/local/bin/migrate
    rm -f /tmp/migrate
fi
migrate -version || true

# ------------------------------------------------------- service user + dir ---
if ! id "$APP_USER" >/dev/null 2>&1; then
    echo "==> Creating system user ${APP_USER}"
    useradd --system --create-home --home-dir "/home/${APP_USER}" --shell /usr/sbin/nologin "$APP_USER"
fi

mkdir -p "$(dirname "$APP_DIR")"
if [ ! -d "$APP_DIR/.git" ]; then
    echo "==> Cloning ${REPO_URL} into ${APP_DIR}"
    git clone "$REPO_URL" "$APP_DIR"
fi
mkdir -p "$APP_DIR/static/uploads" "$APP_DIR/secrets"
chown -R "$APP_USER:$APP_USER" "$(dirname "$APP_DIR")"

# ----------------------------------------------------------------- database ---
echo "==> Ensuring MariaDB database + user exist"
systemctl enable --now mariadb
DB_PASSWORD="${DB_PASSWORD:-}"
if [ -z "$DB_PASSWORD" ]; then
    DB_PASSWORD="$(openssl rand -base64 24)"
    echo "    Generated DB password for ${DB_USER}: ${DB_PASSWORD}"
    echo "    (store this — it is written into ${APP_DIR}/.env below)"
fi
mysql <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

# --------------------------------------------------------------------- .env ---
ENV_FILE="$APP_DIR/.env"
if [ -f "$ENV_FILE" ]; then
    echo "==> ${ENV_FILE} already exists — leaving it untouched."
else
    echo "==> Writing ${ENV_FILE} from .env.production.example"
    JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48)}"
    sed \
        -e "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" \
        -e "s|^DB_NAME=.*|DB_NAME=${DB_NAME}|" \
        -e "s|^DB_USER=.*|DB_USER=${DB_USER}|" \
        -e "s|^DB_PASSWORD=.*|DB_PASSWORD=${DB_PASSWORD}|" \
        -e "s|^APP_URL=.*|APP_URL=https://${DOMAIN}|" \
        "$APP_DIR/.env.production.example" > "$ENV_FILE"
    chown "$APP_USER:$APP_USER" "$ENV_FILE"
    chmod 600 "$ENV_FILE"
    echo "    Review ${ENV_FILE} and set FCM_* if push notifications are used."
fi

# ------------------------------------------------------------------ systemd ---
echo "==> Installing systemd unit"
install -m 0644 "$REPO_ROOT/deploy/sebaya.service" /etc/systemd/system/sebaya.service
systemctl daemon-reload
systemctl enable sebaya

# Allow the service user to restart/inspect the service (used by deploy.sh).
SYSTEMCTL="$(command -v systemctl)"
JOURNALCTL="$(command -v journalctl)"
cat > /etc/sudoers.d/sebaya <<SUDOERS
${APP_USER} ALL=(root) NOPASSWD: ${SYSTEMCTL} restart sebaya, ${SYSTEMCTL} status sebaya, ${JOURNALCTL} -u sebaya *
SUDOERS
chmod 0440 /etc/sudoers.d/sebaya
visudo -cf /etc/sudoers.d/sebaya

# -------------------------------------------------------------------- nginx ---
echo "==> Installing nginx site"
install -m 0644 "$REPO_ROOT/deploy/nginx/sebayadm.remorac.com.conf" \
    "/etc/nginx/sites-available/${DOMAIN}"
ln -sf "/etc/nginx/sites-available/${DOMAIN}" "/etc/nginx/sites-enabled/${DOMAIN}"
nginx -t
systemctl reload nginx

# --------------------------------------------------------------------- TLS ----
if [ ! -d "/etc/letsencrypt/live/${DOMAIN}" ]; then
    echo "==> Obtaining Let's Encrypt certificate for ${DOMAIN}"
    if [ -n "$CERTBOT_EMAIL" ]; then
        certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$CERTBOT_EMAIL" --redirect
    else
        certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos --register-unsafely-without-email --redirect
    fi
else
    echo "==> TLS cert for ${DOMAIN} already present — skipping certbot."
fi

# ------------------------------------------------------------- first deploy ---
echo "==> Running first build + deploy"
chown -R "$APP_USER:$APP_USER" "$APP_DIR"
sudo -u "$APP_USER" env APP_DIR="$APP_DIR" bash "$APP_DIR/deploy/deploy.sh"

echo
echo "Provisioning complete. Service: systemctl status sebaya"
echo "Seed an admin user, then visit https://${DOMAIN}/admin/login"
