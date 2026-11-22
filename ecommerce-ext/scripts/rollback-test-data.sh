#!/usr/bin/env bash
# Restores what cleanup-test-data.sh --apply removed.
#   --dir <backup dir>            re-insert the exported rows (deleted-rows.sql) after checking sha256;
#                                 rows already present are skipped (INSERT IGNORE); kept data is untouched
#   --dir <backup dir> --full --confirm RESTORE-FULL
#                                 replace the WHOLE database with db-full.sql (loses anything written since)
# Environment: MYSQL_CONTAINER [ezy-my-site-e29d88-mysql-1]  DB [blog]
set -euo pipefail
MYSQL_CONTAINER="${MYSQL_CONTAINER:-ezy-my-site-e29d88-mysql-1}"
DB="${DB:-blog}"
DIR=""; FULL=0; CONFIRM=""
while [ $# -gt 0 ]; do
  case "$1" in
    --dir) shift; DIR="${1:-}" ;;
    --full) FULL=1 ;;
    --confirm) shift; CONFIRM="${1:-}" ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done
[ -d "$DIR" ] || { echo "--dir <backup directory> is required" >&2; exit 2; }
(cd "$DIR" && shasum -a 256 -c SHA256SUMS) || { echo "sha256 check failed: stop" >&2; exit 1; }
run() { docker exec -i "$MYSQL_CONTAINER" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$1" 2>&1 | grep -v "Using a password"; exit ${PIPESTATUS[0]}' sh "$DB"; }
count() { docker exec "$MYSQL_CONTAINER" sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -N -B $DB -e \"select (select count(*) from ezy_users),(select count(*) from ecommerce_orders),(select count(*) from ecommerce_order_products)\" 2>/dev/null"; }
echo "users/orders/order_lines before: $(count)"
if [ "$FULL" = 1 ]; then
  [ "$CONFIRM" = "RESTORE-FULL" ] || { echo "--full needs --confirm RESTORE-FULL" >&2; exit 2; }
  run < "$DIR/db-full.sql"
else
  { echo "START TRANSACTION;"; cat "$DIR/deleted-rows.sql"; echo "COMMIT;"; } | run
fi
echo "users/orders/order_lines after:  $(count)"
