#!/usr/bin/env bash
#
# One-time decommission of the retired SebayaDM deployment (sebayadm.remorac.com).
# The project was renamed to NutriMentor; this removes every server-side artifact the
# old `deploy/setup.sh` created, so only the NutriMentor stack remains.
#
# Run as root:  sudo bash deploy/decommission-sebayadm.sh
# Idempotent — safe to re-run. Backs up the old DB and app dir BEFORE deleting anything.
#
# Removes: sebaya systemd service + unit, nginx site + TLS cert for sebayadm.remorac.com,
#          /etc/sudoers.d/sebaya, the `sebaya` database + DB user, /opt/sebaya, and the
#          `sebaya` service user. Prints a manual TODO for the DNS record.
#
# Flags:
#   --yes     Skip the interactive "type sebayadm to confirm" prompt.
#   --force   Proceed even if the new `nutrimentor` service is not active.
set -euo pipefail

# ------------------------------------------------------------------ config ----
OLD_DOMAIN="${OLD_DOMAIN:-sebayadm.remorac.com}"
OLD_SERVICE="${OLD_SERVICE:-sebaya}"
OLD_USER="${OLD_USER:-sebaya}"
OLD_APP_DIR="${OLD_APP_DIR:-/opt/sebaya/app}"
OLD_DB="${OLD_DB:-sebaya}"
OLD_DB_USER="${OLD_DB_USER:-sebaya}"
NEW_SERVICE="${NEW_SERVICE:-nutrimentor}"
BACKUP_DIR="${BACKUP_DIR:-/root/sebayadm-decommission-$(date +%Y%m%d-%H%M%S)}"

ASSUME_YES=0
FORCE=0
for arg in "$@"; do
    case "$arg" in
        --yes)   ASSUME_YES=1 ;;
        --force) FORCE=1 ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done

OLD_APP_PARENT="$(dirname "$OLD_APP_DIR")"   # /opt/sebaya

# --------------------------------------------------------------- preflight ----
if [ "$(id -u)" -ne 0 ]; then
    echo "Please run as root (sudo bash deploy/decommission-sebayadm.sh)." >&2
    exit 1
fi

if [ "$FORCE" -ne 1 ]; then
    if ! systemctl is-active --quiet "$NEW_SERVICE"; then
        echo "!! The new '${NEW_SERVICE}' service is not active." >&2
        echo "   Refusing to decommission the old stack while nothing is serving." >&2
        echo "   Start NutriMentor first, or re-run with --force if you know what you're doing." >&2
        exit 1
    fi
fi

# ------------------------------------------------------------ confirmation ----
cat <<INFO

This will PERMANENTLY remove the old SebayaDM deployment:

  systemd service   ${OLD_SERVICE}  (/etc/systemd/system/${OLD_SERVICE}.service)
  nginx site        /etc/nginx/sites-{available,enabled}/${OLD_DOMAIN}
  TLS certificate   ${OLD_DOMAIN} (certbot)
  sudoers file      /etc/sudoers.d/${OLD_USER}
  database          ${OLD_DB}  (+ DB user '${OLD_DB_USER}'@'localhost')   << irreversible
  app directory     ${OLD_APP_PARENT}                                     << irreversible
  service user      ${OLD_USER}

A backup of the database and app directory will be written to:
  ${BACKUP_DIR}

INFO

if [ "$ASSUME_YES" -ne 1 ]; then
    read -r -p "Type 'sebayadm' to proceed: " reply
    if [ "$reply" != "sebayadm" ]; then
        echo "Aborted."
        exit 1
    fi
fi

# ---------------------------------------------------------------- backups -----
echo "==> Backing up to ${BACKUP_DIR}"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

if mysql -N -e "SHOW DATABASES LIKE '${OLD_DB}';" 2>/dev/null | grep -q "$OLD_DB"; then
    echo "    Dumping database '${OLD_DB}' -> ${BACKUP_DIR}/${OLD_DB}-db.sql"
    mysqldump --databases "$OLD_DB" > "${BACKUP_DIR}/${OLD_DB}-db.sql"
else
    echo "    Database '${OLD_DB}' not found — skipping DB dump."
fi

if [ -d "$OLD_APP_PARENT" ]; then
    echo "    Archiving ${OLD_APP_PARENT} -> ${BACKUP_DIR}/opt-${OLD_USER}.tar.gz"
    tar czf "${BACKUP_DIR}/opt-${OLD_USER}.tar.gz" -C "$(dirname "$OLD_APP_PARENT")" "$(basename "$OLD_APP_PARENT")"
else
    echo "    ${OLD_APP_PARENT} not found — skipping app-dir archive."
fi

# ---------------------------------------------------------- systemd service ---
echo "==> Removing systemd service '${OLD_SERVICE}'"
systemctl disable --now "$OLD_SERVICE" 2>/dev/null || true
rm -f "/etc/systemd/system/${OLD_SERVICE}.service"
systemctl daemon-reload
systemctl reset-failed "$OLD_SERVICE" 2>/dev/null || true

# -------------------------------------------------------------------- nginx ---
echo "==> Removing nginx site for ${OLD_DOMAIN}"
rm -f "/etc/nginx/sites-enabled/${OLD_DOMAIN}"
rm -f "/etc/nginx/sites-available/${OLD_DOMAIN}"
if command -v nginx >/dev/null 2>&1; then
    if nginx -t 2>/dev/null; then
        systemctl reload nginx || true
    else
        echo "!! nginx config test failed after removing the site — reload skipped." >&2
        echo "   Inspect with: nginx -t" >&2
    fi
fi

# --------------------------------------------------------------------- TLS ----
echo "==> Removing TLS certificate for ${OLD_DOMAIN}"
if command -v certbot >/dev/null 2>&1 && [ -d "/etc/letsencrypt/live/${OLD_DOMAIN}" ]; then
    certbot delete --cert-name "$OLD_DOMAIN" --non-interactive || \
        echo "!! certbot delete failed — remove /etc/letsencrypt/{live,archive,renewal} entries for ${OLD_DOMAIN} manually." >&2
else
    echo "    No certbot cert for ${OLD_DOMAIN} — skipping."
fi

# ----------------------------------------------------------------- sudoers ----
echo "==> Removing /etc/sudoers.d/${OLD_USER}"
rm -f "/etc/sudoers.d/${OLD_USER}"

# ---------------------------------------------------------------- database ----
echo "==> Dropping database '${OLD_DB}' and user '${OLD_DB_USER}'@'localhost'"
mysql <<SQL || echo "!! Could not drop DB/user (is MariaDB running?). Backup is at ${BACKUP_DIR}." >&2
DROP DATABASE IF EXISTS \`${OLD_DB}\`;
DROP USER IF EXISTS '${OLD_DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

# --------------------------------------------------------------- app dir ------
echo "==> Removing ${OLD_APP_PARENT}"
rm -rf "$OLD_APP_PARENT"

# --------------------------------------------------------- service user -------
echo "==> Removing service user '${OLD_USER}'"
if id "$OLD_USER" >/dev/null 2>&1; then
    userdel -r "$OLD_USER" 2>/dev/null || userdel "$OLD_USER" 2>/dev/null || \
        echo "!! Could not fully remove user '${OLD_USER}' — remove it manually if it lingers." >&2
else
    echo "    User '${OLD_USER}' not present — skipping."
fi

# ----------------------------------------------------------------- report -----
cat <<DONE

Decommission complete. Backup retained at:
  ${BACKUP_DIR}

Manual follow-up (cannot be done from this host):
  * Remove the DNS A/AAAA record for ${OLD_DOMAIN}.
  * If retiring the old mobile app too: delete the old Firebase project and the
    'com.sebaya.dm' Play Store listing in their respective consoles.

Verify NutriMentor is still serving:
  curl -fsS https://nutrimentor.remorac.com/api/education
DONE
