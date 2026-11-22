#!/usr/bin/env bash
# Removes the data that the test suites created on the LOCAL demo database and keeps the demo data.
#
# KEEPS:   the user johnny.test1 and its order #77 (with that order's cart), the admin account,
#          all products, categories, settings, mails and newsletter rows.
# REMOVES: every other storefront user that looks like a test user (username starts with ecm, p7
#          or p8, or an @example.com / @example.test address, or khangnguyen.shop.hn+test@...),
#          plus the orders, carts, tokens, keywords, wishlist rows and order-status rows of those
#          users. A user that does not look like a test user is never removed.
#
# Modes (the default changes nothing):
#   (no flag) | --dry-run     print the exact lists and row counts that would be removed
#   --simulate                run the real DELETE statements inside a transaction and ROLLBACK;
#                             prints the affected row counts and proves nothing was changed
#   --apply --confirm DELETE-TEST-DATA
#                             full backup (+ sha256), export of the rows to be removed, then delete
#                             in ONE transaction, then verify the kept data
#
# Environment (defaults in brackets):
#   MYSQL_CONTAINER [ezy-my-site-e29d88-mysql-1]   DB [blog]
#   KEEP_USER_EMAIL [johnny.test1@khangnguyen.com]  KEEP_ORDER_ID [77]
#   BACKUP_ROOT [<repo>/.artifacts/backups]
# The database password is read inside the container from its own environment; it is never printed.
#
# Rollback: scripts/rollback-test-data.sh --dir <backup directory printed by --apply>
set -euo pipefail

MYSQL_CONTAINER="${MYSQL_CONTAINER:-ezy-my-site-e29d88-mysql-1}"
DB="${DB:-blog}"
KEEP_USER_EMAIL="${KEEP_USER_EMAIL:-johnny.test1@khangnguyen.com}"
KEEP_ORDER_ID="${KEEP_ORDER_ID:-77}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_ROOT="${BACKUP_ROOT:-$HERE/../../.artifacts/backups}"

MODE=dry-run
CONFIRM=""
while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run) MODE=dry-run ;;
    --simulate) MODE=simulate ;;
    --apply) MODE=apply ;;
    --confirm) shift; CONFIRM="${1:-}" ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
  shift
done
if [ "$MODE" = apply ] && [ "$CONFIRM" != "DELETE-TEST-DATA" ]; then
  echo "refusing: --apply needs --confirm DELETE-TEST-DATA" >&2
  exit 2
fi
[[ "$KEEP_ORDER_ID" =~ ^[0-9]+$ ]] || { echo "KEEP_ORDER_ID must be a number" >&2; exit 2; }

msql() { # SQL on stdin; extra mysql flags as arguments
  docker exec -i "$MYSQL_CONTAINER" sh -c \
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$@" 2>&1 | grep -v "Using a password on the command line"; exit "${PIPESTATUS[0]:-0}"' \
    sh "$@" "$DB"
}
q() { printf '%s\n' "$1" | docker exec -i "$MYSQL_CONTAINER" sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 -N -B "$1" 2>/dev/null' sh "$DB"; }
join_ids() { paste -sd, - ; }
or_null() { if [ -z "$1" ]; then echo NULL; else echo "$1"; fi; }

docker inspect -f '{{.State.Running}}' "$MYSQL_CONTAINER" | grep -q true \
  || { echo "container $MYSQL_CONTAINER is not running" >&2; exit 1; }

KEEP_USER_ID="$(q "select id from ezy_users where email='$KEEP_USER_EMAIL'" | head -n 2)"
[[ "$KEEP_USER_ID" =~ ^[0-9]+$ ]] || { echo "keep user '$KEEP_USER_EMAIL' not found (or not unique): stop" >&2; exit 1; }
[ "$(q "select count(*) from ecommerce_orders where id=$KEEP_ORDER_ID")" = 1 ] \
  || { echo "keep order #$KEEP_ORDER_ID not found: stop" >&2; exit 1; }
KEEP_CART_ID="$(q "select from_shopping_cart_id from ecommerce_orders where id=$KEEP_ORDER_ID")"
[[ "$KEEP_CART_ID" =~ ^[0-9]+$ ]] || KEEP_CART_ID=0

TEST_USER_WHERE="id<>$KEEP_USER_ID and (username regexp '^(ecm|p7|p8)' or email regexp '@example[.](com|test)\$' or email like 'khangnguyen.shop.hn+test@%')"
USERS="$(q "select id from ezy_users where $TEST_USER_WHERE order by id" | join_ids)"
UNKNOWN_USERS="$(q "select concat(id,':',username) from ezy_users where id<>$KEEP_USER_ID and id not in ($(or_null "$USERS")) order by id" | join_ids)"
ORDERS="$(q "select id from ecommerce_orders where id<>$KEEP_ORDER_ID and creator_user_id in ($(or_null "$USERS")) order by id" | join_ids)"
KEPT_FOREIGN_ORDERS="$(q "select id from ecommerce_orders where id<>$KEEP_ORDER_ID and creator_user_id not in ($(or_null "$USERS")) order by id" | join_ids)"
CARTS="$(q "select id from ecommerce_shopping_carts where id<>$KEEP_CART_ID and owner_id in ($(or_null "$USERS")) order by id" | join_ids)"

# table:column:set  (children before parents)
PLAN=(
  "ecommerce_shopping_cart_products:shopping_cart_id:CARTS"
  "ecommerce_order_products:order_id:ORDERS"
  "ecommerce_order_recipients:order_id:ORDERS"
  "ecommerce_order_histories:order_id:ORDERS"
  "ecommerce_payment_histories:order_id:ORDERS"
  "ecommerce_voucher_usage_histories:order_id:ORDERS"
  "ecommerce_affiliate_token_usage_histories:order_id:ORDERS"
  "ecommerce_delivery_orders:order_id:ORDERS"
  "ecommerce_invoice_orders:order_id:ORDERS"
  "ecommerce_process_delivery_orders:order_id:ORDERS"
  "ecommerce_shop_seller_commission_histories:order_id:ORDERS"
  "ecommerce_orders:id:ORDERS"
  "ecommerce_shopping_carts:id:CARTS"
  "ecommerce_user_order_statuses:user_id:USERS"
  "ecm_wishlist:user_id:USERS"
  "ezy_user_access_tokens:user_id:USERS"
  "ezy_user_meta:user_id:USERS"
  "ezy_user_keywords:user_id:USERS"
  "ezy_users:id:USERS"
)
set_of() { case "$1" in USERS) echo "$USERS" ;; ORDERS) echo "$ORDERS" ;; CARTS) echo "$CARTS" ;; esac; }
where_of() { echo "$2 in ($(or_null "$(set_of "$3")"))"; }

count_line() {
  local t="$1" c="$2" s="$3"
  printf '  %-48s %8s rows\n' "$t" "$(q "select count(*) from $t where $(where_of "$t" "$c" "$s")")"
}

print_plan() {
  echo "== KEEP =="
  echo "  user  id=$KEEP_USER_ID  $(q "select concat(username,' <',email,'>') from ezy_users where id=$KEEP_USER_ID")"
  echo "  order #$KEEP_ORDER_ID (cart $KEEP_CART_ID), $(q "select count(*) from ecommerce_products") products, $(q "select count(*) from ezy_admins") admin account(s)"
  echo "== USERS to remove: $(echo "$USERS" | tr ',' '\n' | grep -c . || true) =="
  q "select concat('  ',id,'  ',username,'  ',email) from ezy_users where id in ($(or_null "$USERS")) order by id"
  echo "== ORDERS to remove: $(echo "$ORDERS" | tr ',' '\n' | grep -c . || true) =="
  q "select concat('  #',id,'  user ',creator_user_id,'  ',status) from ecommerce_orders where id in ($(or_null "$ORDERS")) order by id"
  echo "== CARTS to remove: $(echo "$CARTS" | tr ',' '\n' | grep -c . || true) (ids: ${CARTS:-none}) =="
  echo "== Rows per table =="
  local e
  for e in "${PLAN[@]}"; do IFS=: read -r t c s <<<"$e"; count_line "$t" "$c" "$s"; done
  echo "== NOT touched =="
  echo "  users that do not look like test users (kept): ${UNKNOWN_USERS:-none}"
  echo "  orders of those users (kept): ${KEPT_FOREIGN_ORDERS:-none}"
  echo "  ezymail_* mail records, ecm_newsletter rows, products, product stock (stock is NOT restored)"
}

delete_sql() {
  local e t c s
  echo "START TRANSACTION;"
  for e in "${PLAN[@]}"; do
    IFS=: read -r t c s <<<"$e"
    echo "DELETE FROM $t WHERE $(where_of "$t" "$c" "$s");"
    echo "SELECT '$t' AS tbl, ROW_COUNT() AS deleted;"
  done
}

keep_check() {
  echo "  keep user present:   $(q "select count(*) from ezy_users where id=$KEEP_USER_ID")"
  echo "  keep order present:  $(q "select count(*) from ecommerce_orders where id=$KEEP_ORDER_ID") (lines: $(q "select count(*) from ecommerce_order_products where order_id=$KEEP_ORDER_ID"), recipient: $(q "select count(*) from ecommerce_order_recipients where order_id=$KEEP_ORDER_ID"))"
  echo "  products / admins:   $(q "select count(*) from ecommerce_products") / $(q "select count(*) from ezy_admins")"
  echo "  users / orders left: $(q "select count(*) from ezy_users") / $(q "select count(*) from ecommerce_orders")"
}

echo "mode=$MODE container=$MYSQL_CONTAINER db=$DB"
print_plan

case "$MODE" in
  dry-run)
    echo
    echo "dry-run only: nothing was changed."
    ;;
  simulate)
    echo
    echo "== simulate: real DELETE statements, then ROLLBACK =="
    echo "before:"; keep_check
    { delete_sql; echo "ROLLBACK;"; } | msql -t
    echo "after rollback (must equal 'before'):"; keep_check
    ;;
  apply)
    STAMP="$(date +%H%M)"
    OUT="$BACKUP_ROOT/p9-before-cleanup-$STAMP"
    mkdir -p "$OUT"
    echo
    echo "== backup to $OUT =="
    docker exec "$MYSQL_CONTAINER" sh -c \
      'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --no-tablespaces --default-character-set=utf8mb4 "$1" 2>/dev/null' sh "$DB" > "$OUT/db-full.sql"
    tail -n 3 "$OUT/db-full.sql" | grep -q "Dump completed" || { echo "full dump incomplete: stop, nothing deleted" >&2; exit 1; }
    {
      echo "-- rows removed by cleanup-test-data.sh at $(date '+%F %T %z'); restore with rollback-test-data.sh"
      echo "SET FOREIGN_KEY_CHECKS=0;"
      for e in "${PLAN[@]}"; do
        IFS=: read -r t c s <<<"$e"
        W="$(where_of "$t" "$c" "$s")"
        docker exec "$MYSQL_CONTAINER" sh -c \
          'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --no-create-info --insert-ignore --skip-add-locks --skip-disable-keys --skip-lock-tables --no-tablespaces --skip-triggers --default-character-set=utf8mb4 --where="$2" "$1" "$3" 2>/dev/null' sh "$DB" "$W" "$t"
      done
    } > "$OUT/deleted-rows.sql"
    printf 'users=%s\norders=%s\ncarts=%s\nkeep_user=%s\nkeep_order=%s\n' \
      "$USERS" "$ORDERS" "$CARTS" "$KEEP_USER_ID" "$KEEP_ORDER_ID" > "$OUT/ids.txt"
    (cd "$OUT" && shasum -a 256 db-full.sql deleted-rows.sql ids.txt > SHA256SUMS && cat SHA256SUMS)
    echo "== delete in one transaction =="
    { delete_sql; echo "COMMIT;"; } | msql -t
    echo "== verify =="; keep_check
    echo "rollback: $HERE/rollback-test-data.sh --dir $OUT"
    ;;
esac
