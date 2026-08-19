#!/usr/bin/env bash
#
# Repeatable production deploy for the SebayaDM backend. Run on the server as the
# `sebaya` user (or via `sudo -u sebaya`) after deploy/setup.sh has provisioned
# the host. Idempotent: pulls latest, rebuilds, migrates, restarts, health-checks.
#
#   cd /opt/sebaya/app && ./deploy/deploy.sh
#
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/sebaya/app}"
SERVICE="${SERVICE:-sebaya}"
HEALTH_URL="${HEALTH_URL:-https://sebayadm.remorac.com/api/education}"

# Use sudo for privileged systemctl/journalctl calls unless already root.
SUDO=""
[ "$(id -u)" -ne 0 ] && SUDO="sudo"

cd "$APP_DIR"

echo "==> Pulling latest source"
git pull --ff-only

echo "==> Installing Node deps (Tailwind CLI)"
if [ -f package-lock.json ]; then
    npm ci
else
    npm install
fi

echo "==> Building CSS + Go binary"
make build   # runs `make css` then `go build -ldflags "-s -w" -o bin/server`

echo "==> Applying database migrations"
make migrate-up

echo "==> Restarting service"
$SUDO systemctl restart "$SERVICE"

echo "==> Waiting for service to come up"
for i in $(seq 1 15); do
    if curl -fsS -o /dev/null "$HEALTH_URL"; then
        echo "==> Healthy: $HEALTH_URL"
        echo "Deploy complete."
        exit 0
    fi
    sleep 1
done

echo "!! Health check failed for $HEALTH_URL" >&2
echo "   Recent logs:" >&2
$SUDO systemctl status "$SERVICE" --no-pager -l | tail -n 20 >&2 || true
$SUDO journalctl -u "$SERVICE" --no-pager -n 30 >&2 || true
exit 1
