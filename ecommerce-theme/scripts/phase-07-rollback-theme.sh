#!/usr/bin/env bash
# Roll Docker back to Freestyle 0.2.9 and restore EcmOrderViewController in ecommerce-ext.
# Usage: phase-07-rollback-theme.sh [--dry-run] [RESTORE_DB=<dump.sql>]
#   --dry-run  only print the commands that would run; nothing is executed.
set -eu
DRY=0; [ "${1:-}" = "--dry-run" ] && DRY=1
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
C="${CONTAINER:-ezy-my-site-e29d88-ezyplatform-1}"; DB="${DB_CONTAINER:-ezy-my-site-e29d88-mysql-1}"
run() { echo "+ $*"; [ "$DRY" = 1 ] || "$@"; }
cli() { echo "+ docker exec $C cli.sh: $1"; [ "$DRY" = 1 ] || docker exec "$C" sh -c "cd /app/ezyplatform && printf '$1\nexit\n' | bash ./cli.sh >/dev/null 2>&1"; }
echo "# 0. Preflight: every input must exist before anything is removed"
BAK="$ROOT/.artifacts/tmp-p7/EcmOrderViewController.java.bak"
[ -f "$BAK" ] || { echo "ABORT: missing backup $BAK" >&2; exit 1; }
if [ -n "${RESTORE_DB:-}" ]; then
  [ -f "$RESTORE_DB" ] || { echo "ABORT: missing dump $RESTORE_DB" >&2; exit 1; }
  [ -f "$RESTORE_DB.sha256" ] || { echo "ABORT: missing checksum $RESTORE_DB.sha256" >&2; exit 1; }
  echo "+ shasum -a 256 -c $(basename "$RESTORE_DB").sha256 (in $(dirname "$RESTORE_DB"))"
  (cd "$(dirname "$RESTORE_DB")" && shasum -a 256 -c "$(basename "$RESTORE_DB").sha256") || { echo "ABORT: dump checksum mismatch" >&2; exit 1; }
fi
echo "# 1. Activate Freestyle, deactivate ecommerce-theme (activating Freestyle does not switch it off), then remove it"
cli "activate theme freestyle"
cli "deactivate theme ecommerce-theme"
run docker exec "$C" rm -rf /app/ezyplatform/web/themes/ecommerce-theme
echo "# 2. Restore EcmOrderViewController and rebuild ecommerce-ext"
ORDER_DIR="$(dirname "$(find "$ROOT/ecommerce-ext" -name EcmAccountViewController.java | head -1)")"
run cp "$BAK" "$ORDER_DIR/EcmOrderViewController.java"
run env EZY_DIR="$HOME/ezyplatform-dev-site" ezy dev run "$ROOT/ecommerce-ext/export.sh"
run docker cp "$HOME/ezyplatform-dev-site/ezyplatform/web/plugins/ecommerce-ext/." "$C:/app/ezyplatform/web/plugins/ecommerce-ext/"
run docker exec "$C" chown -R root:root /app/ezyplatform/web/plugins/ecommerce-ext
echo "# 3. Optional DB restore"
if [ -n "${RESTORE_DB:-}" ]; then
  echo "+ docker exec -i $DB mysql -uroot < $RESTORE_DB"
  [ "$DRY" = 1 ] || docker exec -i "$DB" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot' < "$RESTORE_DB"
fi
echo "# 4. Restart and verify"
run docker restart "$C"
echo "+ curl :8080 expect 200, :9090 expect 302"
