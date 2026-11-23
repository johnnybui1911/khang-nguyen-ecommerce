#!/usr/bin/env bash
# API tests for the ecommerce-ext compatibility layer.
# Usage: BASE_URL=http://localhost:18080 tests/api/run.sh [section ...]
# Sections: f5 (wishlist), f10 (newsletter double opt-in; reads the local sandbox mail), f4 (forgot/reset password, contact, appointment; reads the local sandbox mail), f1 (filters, static pages, profile update), f3 (QA fixes: price cap, phone, script markers), f2 (attributes, stores, order status; reuses the order made by m3), m1 (guest/user baseline), m2 (guard + forged callback), m3 (needs seeded product: purchase flow + IDOR). Exit code 0 only if every check passes.
# A throw-away user is registered per run; cookies and password never leave this process.
set -u
BASE_URL="${BASE_URL:-http://localhost:8080}"
ORIGIN="${ORIGIN:-$BASE_URL}"
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
PASS=0; FAIL=0
STAMP="$(date +%s)$RANDOM"
USERNAME="ecmt${STAMP}"; EMAIL="ecm-test+${STAMP}@example.com"
PASSWORD="Sx1-$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 14)"
JAR="$TMP/jar"

# req NAME AUTH(guest|user) METHOD PATH [BODY]  -> sets CODE, CTYPE, LOC, and body in $TMP/body
req() {
  local auth="$2" method="$3" path="$4" body="${5:-}" args=()
  [ "$auth" = user ] && args+=(-b "$JAR")
  if [ "$method" != GET ]; then
    args+=(-H "Origin: $ORIGIN" -H "X-Requested-With: ecm" -H "Content-Type: application/json")
    [ -n "$body" ] && args+=(-d "$body")
  fi
  local out
  out=$(curl -s -o "$TMP/body" -w '%{http_code}|%{content_type}|%{redirect_url}' -X "$method" ${args[@]+"${args[@]}"} "$BASE_URL$path")
  CODE="${out%%|*}"; local rest="${out#*|}"; CTYPE="${rest%%|*}"; LOC="${rest#*|}"
}
ok()   { PASS=$((PASS+1)); printf 'PASS  %s\n' "$1"; }
bad()  { FAIL=$((FAIL+1)); printf 'FAIL  %s  (HTTP %s, %s, body: %.80s)\n' "$1" "$CODE" "$CTYPE" "$(tr '\n' ' ' <"$TMP/body")"; }
# expect NAME CONDITION(shell test string using CODE/CTYPE/LOC)
expect() { if eval "$2"; then ok "$1"; else bad "$1"; fi; }
is_not_found_page() { grep -qiE '<title>[^<]*not found' "$TMP/body"; }

register_user() {
  curl -s -o "$TMP/reg" -w '%{http_code}' -c "$JAR" -H 'Content-Type: application/json' \
    -X POST "$BASE_URL/ezylogin/api/v1/register" \
    -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\",\"email\":\"$EMAIL\",\"displayName\":\"ECM Test\"}"
}

section_m1() {
  echo "== m1: guest/user baseline on $BASE_URL"
  local rc; rc=$(register_user)
  [ "$rc" = 200 ] && ok "register throw-away user" || { CODE=$rc; CTYPE=; : >"$TMP/body"; bad "register throw-away user"; }

  req c1 guest GET /api/v1/shopping-cart/product-count
  expect "guest GET /api/v1/shopping-cart/product-count -> 401 or redirect to login" \
    '[ "$CODE" = 401 ] || { [ "${CODE:0:1}" = 3 ] && [[ "$LOC" == *login* ]]; }'
  req c2 user GET /api/v1/shopping-cart/product-count
  expect "user GET /api/v1/shopping-cart/product-count -> 200 JSON" \
    '[ "$CODE" = 200 ] && [[ "$CTYPE" == application/json* ]]'
  req c3 guest GET /account
  expect "guest GET /account -> redirect to login or 401" \
    '[ "$CODE" = 401 ] || { [ "${CODE:0:1}" = 3 ] && [[ "$LOC" == *login* ]]; }'
  req c4 user GET /account
  expect "user GET /account -> 200 HTML page that is not the Not-found page" \
    '[ "$CODE" = 200 ] && [[ "$CTYPE" == text/html* ]] && ! is_not_found_page'
  req c5 user GET "/ezyaccount/api/v1/account"
  expect "user GET /ezyaccount/api/v1/account -> 200 JSON (already served by ezyaccount)" \
    '[ "$CODE" = 200 ] && [[ "$CTYPE" == application/json* ]]'
}

section_m2() {
  echo "== m2: guard + demo payment on $BASE_URL"
  # the throw-away user registered by m1 (or by this call) is reused
  if [ ! -s "$JAR" ]; then
    local rc; rc=$(register_user)
    [ "$rc" = 200 ] && ok "register throw-away user" || { CODE=$rc; CTYPE=; : >"$TMP/body"; bad "register throw-away user"; }
  fi

  # state-changing call without X-Requested-With must be refused by the guard
  local out
  out=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X POST \
    -H "Origin: $ORIGIN" -H "Content-Type: application/json" -d '{}' \
    "$BASE_URL/api/v1/checkout/shopping-cart-products/add"); CODE=$out; CTYPE=
  expect "POST add-to-cart without X-Requested-With -> 403" '[ "$CODE" = 403 ]'
  out=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X POST \
    -H "Origin: https://evil.example" -H "X-Requested-With: ecm" \
    -H "Content-Type: application/json" -d '{}' \
    "$BASE_URL/api/v1/checkout/shopping-cart-products/add"); CODE=$out; CTYPE=
  expect "POST add-to-cart from foreign Origin -> 403" '[ "$CODE" = 403 ]'
  req g1 user POST /api/v1/checkout/shopping-cart-products/add '{}'
  expect "POST add-to-cart with valid Origin + header reaches ecommerce (JSON answer, not 403/5xx)" \
    '[ "$CODE" != 403 ] && [ "${CODE:0:1}" != 5 ] && [[ "$CTYPE" == application/json* ]]'

  # callback of the demo payment must refuse an unsigned or forged request
  out=$(curl -s -o "$TMP/body" -w '%{http_code}|%{redirect_url}' \
    "$BASE_URL/callback/ECM_DEMO?orderId=1&amount=1&sig=forged"); CODE="${out%%|*}"; LOC="${out#*|}"; CTYPE=
  expect "forged ECM_DEMO callback does not approve the order (redirects to payment-error or 4xx)" \
    '[[ "$LOC" == *payment-error* ]] || [ "${CODE:0:1}" = 4 ]'

  # unknown product page must be a real 404, not a 200 "not found" page
  req n1 guest GET /products/NOPE999
  expect "GET /products/NOPE999 -> HTTP 404" '[ "$CODE" = 404 ]'

  # login is limited per client: the 6th wrong attempt inside the window is refused with 429
  local i last=0 first_bad=0
  for i in 1 2 3 4 5 6; do
    if [ "$i" = 1 ]; then # a previous run may still hold the window: wait it out once
      local ra; ra=$(curl -s -o /dev/null -D- -H 'Content-Type: application/json' -X POST \
        "$BASE_URL/ezylogin/api/v1/login" -d '{"username":"probe","password":"x"}' | tr -d '\r' | sed -n 's/^[Rr]etry-[Aa]fter: *//p')
      [ -n "$ra" ] && sleep $((ra + 1))
    fi
    last=$(curl -s -o "$TMP/body" -w '%{http_code}' -H 'Content-Type: application/json' \
      -X POST "$BASE_URL/ezylogin/api/v1/login" -d "{\"username\":\"nobody$STAMP\",\"password\":\"wrong-$i\"}")
    [ "$i" = 1 ] && first_bad=$last
  done
  CODE=$last; CTYPE=
  expect "6th wrong login within the window -> 429 (1st was $first_bad)" '[ "$CODE" = 429 ] && [ "$first_bad" != 429 ]'
}

section_m3() {
  echo "== m3: cart -> checkout -> demo payment -> order, IDOR on $BASE_URL"
  # Needs a seeded shop/product with stock (each run buys 2 units; set PRODUCT_ID to one that has stock); override with PRODUCT_ID, CURRENCY_ID, COUNTRY_ID, PAY_CODE. PRICE is read from the catalog for CURRENCY_ID unless given.
  local pid="${PRODUCT_ID:-1}" price="${PRICE:-}" cur="${CURRENCY_ID:-6}" ctry="${COUNTRY_ID:-1}" code="${PAY_CODE:-ECM_DEMO_SG}"
  if [ ! -s "$JAR" ]; then
    local rc; rc=$(register_user)
    [ "$rc" = 200 ] && ok "register throw-away user" || { CODE=$rc; CTYPE=; : >"$TMP/body"; bad "register throw-away user"; }
  fi

  req p0 guest GET "/api/v1/products/$pid?currencyId=$cur"
  [ -z "$price" ] && price=$(sed -nE 's/.*"price":([0-9]+)(\.[0-9]*[1-9])?\.?0*,.*/\1\2/p' "$TMP/body" | head -1)
  expect "seeded product $pid is published in the catalog (price ${price:-?} for currency $cur) -> 200 JSON" \
    '[ "$CODE" = 200 ] && [[ "$CTYPE" == application/json* ]]'

  req p1 user POST /api/v1/checkout "{\"productIds\":[$pid],\"amountByProductId\":{\"$pid\":2}}"
  expect "checkout of seeded product $pid -> 200 with shoppingCartId" \
    '[ "$CODE" = 200 ] && grep -q shoppingCartId "$TMP/body"'
  local cart; cart=$(sed -E 's/.*"shoppingCartId":([0-9]+).*/\1/' "$TMP/body")

  req p2 user POST /api/v1/orders/create-payment-url "{\"paymentServiceCode\":\"$code\",\"fromShoppingCartId\":${cart:-0},\"currencyId\":$cur,\"products\":[{\"productId\":$pid,\"amount\":2,\"price\":$price}],\"deliveryInformation\":{\"deliveryFullName\":\"ECM Test\",\"deliveryEmail\":\"$EMAIL\",\"deliveryPhoneNumber\":\"+84912345678\",\"deliveryAddress\":\"1 Nguyễn Huệ, Quận 1\",\"deliveryCountryId\":$ctry,\"deliveryCityName\":\"Hà Nội\",\"deliveryPostalCode\":\"100000\"}}"
  expect "create-payment-url returns a signed /callback/$code URL" \
    '[ "$CODE" = 200 ] && grep -q "^/callback/$code?orderId=" "$TMP/body"'
  local url oid; url=$(cat "$TMP/body"); oid=$(echo "$url" | sed -E 's/.*orderId=([0-9]+).*/\1/')
  LAST_OID="$oid"; LAST_EMAIL="$EMAIL"

  req p3 user GET "/api/v1/orders/$oid/status"
  expect "new order is not PAID before the callback" '[ "$CODE" = 200 ] && ! grep -q PAID "$TMP/body"'

  curl -s -o /dev/null -b "$JAR" "$BASE_URL/callback/$code?orderId=$oid&amount=1&sig=forged"
  req p4 user GET "/api/v1/orders/$oid/status"
  expect "forged callback leaves the order unpaid" '[ "$CODE" = 200 ] && ! grep -q PAID "$TMP/body"'

  out=$(curl -s -o /dev/null -w '%{http_code}|%{redirect_url}' -b "$JAR" "$BASE_URL$url"); CODE="${out%%|*}"; LOC="${out#*|}"; CTYPE=; : >"$TMP/body"
  expect "signed callback redirects to the order page" '[ "$CODE" = 302 ] && [[ "$LOC" == */orders/$oid* ]]'
  req p5 user GET "/api/v1/orders/$oid/status"
  expect "order is PAID after the signed callback" '[ "$CODE" = 200 ] && grep -q PAID "$TMP/body"'

  # a tampered unit price must be rejected or priced by the server, never accepted as sent
  req p7 user POST /api/v1/checkout "{\"productIds\":[$pid],\"amountByProductId\":{\"$pid\":1}}"
  local cart2; cart2=$(sed -E 's/.*"shoppingCartId":([0-9]+).*/\1/' "$TMP/body")
  req p8 user POST /api/v1/orders/create-payment-url "{\"paymentServiceCode\":\"$code\",\"fromShoppingCartId\":${cart2:-0},\"currencyId\":$cur,\"products\":[{\"productId\":$pid,\"amount\":1,\"price\":1}],\"deliveryInformation\":{\"deliveryFullName\":\"ECM Test\",\"deliveryEmail\":\"$EMAIL\",\"deliveryPhoneNumber\":\"+84912345678\",\"deliveryAddress\":\"1 Nguyễn Huệ, Quận 1\",\"deliveryCountryId\":$ctry,\"deliveryCityName\":\"Hà Nội\",\"deliveryPostalCode\":\"100000\"}}"
  expect "checkout with tampered price 1 is rejected as productSizeColorPricesChanged (no payment URL)" \
    '[ "$CODE" = 400 ] && grep -q productSizeColorPricesChanged "$TMP/body"'

  # a second user must not read the first user's order
  local saved_jar="$JAR" saved_user="$USERNAME" saved_email="$EMAIL"
  JAR="$TMP/jar-b"; USERNAME="${USERNAME}b"; EMAIL="b-${EMAIL}"
  register_user >/dev/null
  req p6 user GET "/api/v1/orders/$oid/status"
  expect "other user GET /api/v1/orders/$oid/status is refused with 403/404" '[[ "$oid" =~ ^[0-9]+$ ]] && { [ "$CODE" = 403 ] || [ "$CODE" = 404 ]; }'
  JAR="$saved_jar"; USERNAME="$saved_user"; EMAIL="$saved_email"
}

section_f1() {
  echo "== f1: storefront filters, static pages, profile update on $BASE_URL"
  local pg
  for pg in size-guide gift-card-balance; do
    req f1 guest GET "/$pg"
    expect "GET /$pg -> 200 HTML that links back to /customer-service" \
      '[ "$CODE" = 200 ] && [[ "$CTYPE" == text/html* ]] && grep -q "href=\"/customer-service\"" "$TMP/body"'
    req f1 guest GET /customer-service
    expect "customer service page links to /$pg" '[ "$CODE" = 200 ] && grep -q "href=\"/$pg\"" "$TMP/body"'
  done

  req f2 guest GET "/api/v1/products?productTypes=SINGLE&limit=100&priceGte=2000000&priceLte=3000000"
  expect "price filter: every item price is inside the requested range" \
    '[ "$CODE" = 200 ] && python3 -c "import sys,json;d=json.load(open(sys.argv[1]));i=d.get(\"items\",[]);sys.exit(0 if i and all(2000000<=float(x[\"price\"])<=3000000 for x in i) else 1)" "$TMP/body"'
  req f3 guest GET "/api/v1/products?productTypes=SINGLE&limit=100&discountPercentGt=9"
  expect "discount filter (>= 10%): every item is discounted by 10% or more" \
    '[ "$CODE" = 200 ] && python3 -c "import sys,json;d=json.load(open(sys.argv[1]));i=d.get(\"items\",[]);sys.exit(0 if i and all(float(x.get(\"discountPercent\") or 0)>=10 for x in i) else 1)" "$TMP/body"'

  if [ ! -s "$JAR" ]; then register_user >/dev/null; fi
  local upd="{\"updateUser\":true,\"updateCustomer\":true,\"displayName\":\"ECM Renamed\",\"email\":\"$EMAIL\",\"phone\":\"+849${STAMP: -8}\",\"addressNumber\":\"1 Nguyễn Huệ, Quận 1\"}"
  local saved_origin="$ORIGIN"
  ORIGIN="https://evil.example"
  req f4 user PUT /ezyaccount/api/v1/account/update "${upd//ECM Renamed/Evil}"
  ORIGIN="$saved_origin"
  expect "profile update from a foreign Origin -> 403" '[ "$CODE" = 403 ]'
  req f5 user PUT /ezyaccount/api/v1/account/update "$upd"
  expect "profile update from this site with X-Requested-With -> 2xx" '[ "${CODE:0:1}" = 2 ]'
  req f6 user GET /ezyaccount/api/v1/account
  expect "GET account shows the new display name and not the rejected one" \
    '[ "$CODE" = 200 ] && grep -q "\"displayName\":\"ECM Renamed\"" "$TMP/body"'
  req f7 user PUT /ezyaccount/api/v1/account/update "{\"updateUser\":true,\"displayName\":\"\",\"email\":\"$EMAIL\",\"phone\":\"abc\"}"
  expect "invalid profile -> 400 JSON with per-field errors" \
    '[ "$CODE" = 400 ] && [[ "$CTYPE" == application/json* ]] && grep -q displayName "$TMP/body" && grep -q phone "$TMP/body"'
}

section_f2() {
  echo "== f2: colour/material data, swatch siblings, stores, order status on $BASE_URL"
  req g1 guest GET /data/product-attributes.json
  expect "product-attributes.json -> 200 with colours, materials and a family per product" \
    '[ "$CODE" = 200 ] && python3 -c "import sys,json;d=json.load(open(sys.argv[1]));c={x for v in d.values() for x in v[\"colors\"]};m={x for v in d.values() for x in v[\"materials\"]};sys.exit(0 if len(d)>=47 and {\"Blue\",\"White\"}<=c and \"Rhodium plated\" in m and all(v[\"family\"] for v in d.values()) else 1)" "$TMP/body"'
  expect "Swan pendant 5723387 has at least 2 colour siblings (5204133, 5732414)" \
    'python3 -c "import sys,json;d=json.load(open(sys.argv[1]));f=d[\"5723387\"][\"family\"];s=[k for k,v in d.items() if v[\"family\"]==f and v[\"colors\"]];sys.exit(0 if len(s)>=3 and {\"5204133\",\"5732414\"}<=set(s) else 1)" "$TMP/body"'
  req g2 guest GET /data/stores.json
  expect "stores.json lists at least 3 stores with name, address and hours" \
    '[ "$CODE" = 200 ] && python3 -c "import sys,json;s=json.load(open(sys.argv[1]))[\"stores\"];sys.exit(0 if len(s)>=3 and all(x[\"name\"] and x[\"address\"] and x[\"hours\"] for x in s) else 1)" "$TMP/body"'
  local pg
  for pg in stores order-status; do
    req g3 guest GET "/$pg"
    expect "GET /$pg -> 200 HTML" '[ "$CODE" = 200 ] && [[ "$CTYPE" == text/html* ]] && ! is_not_found_page'
  done
  req g4 guest GET /products/5723387
  expect "GET /products/5723387 -> 200 and NOPE999 is still 404" '[ "$CODE" = 200 ]'
  req g5 guest GET /products/NOPE999
  expect "GET /products/NOPE999 -> 404" '[ "$CODE" = 404 ]'

  if [ -z "${LAST_OID:-}" ]; then section_m3; fi
  local oid="$LAST_OID" email="$LAST_EMAIL"
  # the "+" of the test address must travel as %2B, otherwise the server reads a space
  local enc="${email//+/%2B}"
  req o0 guest GET "/api/v1/ecm/order-status?orderId=$oid&email=$enc"
  if [ "$CODE" = 429 ]; then echo "(order-status limiter is full from an earlier run, waiting 62 s)"; sleep 62; req o0 guest GET "/api/v1/ecm/order-status?orderId=$oid&email=$enc"; fi
  expect "order-status with the delivery email -> 200, PAID, only orderId/status/updatedAt/createdAt/total/items, items only name/quantity/unitPrice/lineTotal, no personal fields" \
    '[ "$CODE" = 200 ] && python3 -I "$(dirname "$0")/check-order-status.py" "$TMP/body" "$oid"'
  req o1 guest GET "/api/v1/ecm/order-status?orderId=$oid&email=$(echo "$enc" | tr 'a-z' 'A-Z' | sed s/%2b/%2B/)"
  expect "order-status email match ignores case -> 200" '[ "$CODE" = 200 ]'
  req o2 guest GET "/api/v1/ecm/order-status?orderId=$oid&email=wrong-$enc"; local c2="$CODE"; cp "$TMP/body" "$TMP/b2"
  req o3 guest GET "/api/v1/ecm/order-status?orderId=999999999&email=$enc"; local c3="$CODE"; cp "$TMP/body" "$TMP/b3"
  req o4 guest GET "/api/v1/ecm/order-status?orderId=$oid&email=b-$enc"; local c4="$CODE"; cp "$TMP/body" "$TMP/b4"
  CODE="$c2/$c3/$c4"; CTYPE=
  expect "wrong email, unknown order, other user's email -> the same 404 and identical body" \
    '[ "$c2" = 404 ] && [ "$c3" = 404 ] && [ "$c4" = 404 ] && cmp -s "$TMP/b2" "$TMP/b3" && cmp -s "$TMP/b2" "$TMP/b4" && ! grep -q "$oid" "$TMP/b2"'
  local i n429=0
  for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
    req o5 guest GET "/api/v1/ecm/order-status?orderId=$oid&email=x$i@example.com"
    [ "$CODE" = 429 ] && n429=$((n429+1))
  done
  CODE="429 x$n429"
  expect "more than 10 lookups within a minute -> 429" '[ "$n429" -ge 1 ]'
}

section_f3() {
  echo "== f3: fixes found by QA (search price cap, guest probe, phone, order money)"
  # the products API does not cut the list at priceLte, which is why the storefront applies the cap itself
  req s1 guest GET "/api/v1/products?productTypes=SINGLE&limit=100&keyword=ring&priceLte=3000000"
  expect "platform /api/v1/products ignores priceLte (storefront caps the price on the client)" \
    '[ "$CODE" = 200 ] && python3 -c "import sys,json;sys.exit(0 if any(float(p[\"price\"])>3000000 for p in json.load(open(sys.argv[1]))[\"items\"]) else 1)" "$TMP/body"'
  [ -f "$JAR" ] && [ -n "${USERNAME:-}" ] || section_m1
  local upd='{"updateUser":true,"updateCustomer":true,"displayName":"ECM Test","email":"'"$EMAIL"'","phone":"PHONE","dateOfBirth":0,"addressNumber":"","addressCountryId":0,"addressProvinceId":0,"addressDistrictId":0,"addressWardId":0}'
  req u1 user PUT /ezyaccount/api/v1/account/update "${upd/PHONE/+84 912 345 678}"
  expect "phone with spaces is refused by the account API (the form must strip them) -> 400 phone invalid" \
    '[ "$CODE" = 400 ] && grep -q "\"phone\":\"invalid\"" "$TMP/body"'
  local ph="+84$((RANDOM % 9 + 1))$(printf '%08d' $((RANDOM * 31 % 100000000)))"
  req u2 user PUT /ezyaccount/api/v1/account/update "${upd/PHONE/$ph}"
  expect "phone without spaces is accepted -> 204" '[ "$CODE" = 204 ]'
  req u3 user GET /ezyaccount/api/v1/account
  expect "the stored phone keeps the country code exactly as sent" \
    '[ "$CODE" = 200 ] && python3 -c "import sys,json;sys.exit(0 if json.load(open(sys.argv[1]))[\"phoneNumber\"]==sys.argv[2] else 1)" "$TMP/body" "$ph"'
  req j1 guest GET /js/ecommerce.js
  expect "storefront script has the price cap, session-cookie probe, phone cleanup and order money format" \
    '[ "$CODE" = 200 ] && grep -q "maxPrice" "$TMP/body" && grep -q "hasSessionCookie" "$TMP/body" && grep -q "orderMoney" "$TMP/body" && grep -q "replace(/\[" "$TMP/body"'
}

# Read the newest sandbox mail whose recipient contains $1 (local Docker only; the SQL is read-only).
MAIL_CONTAINER="${MAIL_CONTAINER:-ezy-my-site-e29d88-mysql-1}"
mail_q() {
  docker exec "$MAIL_CONTAINER" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -N -e "$0" 2>/dev/null' "$1"
}
mail_count() { mail_q "select count(*) from ezymail_mails where mail_to like '%$1%'"; }
mail_wait() { # wait up to 20 s until the number of mails for $1 exceeds $2
  local i n; for i in $(seq 1 20); do n=$(mail_count "$1"); [ "${n:-0}" -gt "$2" ] && return 0; sleep 1; done; return 1
}
# login NAME USER PASS -> sets CODE; waits out a 429 once, the limiter is per client address
login_try() {
  local i
  for i in 1 2; do
    CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -H "Origin: $ORIGIN" -H "X-Requested-With: ecm" -H 'Content-Type: application/json' \
      -X POST "$BASE_URL/ezylogin/api/v1/login" -d "{\"username\":\"$2\",\"password\":\"$3\"}"); CTYPE=
    [ "$CODE" = 429 ] && [ "$i" = 1 ] && { sleep 62; continue; }; break
  done
}

section_f4() {
  echo "== f4: forgot/reset password, contact and appointment (needs the local sandbox mail DB)"
  local d; for d in /forgot-password /reset-password /contact /book-an-appointment; do
    req p guest GET "$d"
    expect "GET $d -> 200 storefront page, not the Not-found page" '[ "$CODE" = 200 ] && [[ "$CTYPE" == text/html* ]] && ! is_not_found_page'
  done
  req p guest GET /categories/nonexistent
  expect "GET /categories/nonexistent -> real 404" '[ "$CODE" = 404 ]'
  expect "404 page uses the theme frame and links back home" 'grep -q "sw-notfound" "$TMP/body" && grep -q "ecommerce.js" "$TMP/body" && grep -q "href=\"/\"" "$TMP/body"'
  req p guest GET /images/favicon.ico
  expect "GET /images/favicon.ico (asked for by error pages) -> 200" '[ "$CODE" = 200 ]'
  req p guest GET /orders/abc
  expect "GET /orders/abc -> real 404" '[ "$CODE" = 404 ]'
  req p guest GET /ecommerce/products/NOPE999
  expect "GET /ecommerce/products/NOPE999 (platform page) -> real 404" '[ "$CODE" = 404 ]'
  req p guest GET "/api/v1/products?productTypes=SINGLE&limit=1"
  local pcode; pcode=$(python3 -c "import sys,json;print(json.load(open(sys.argv[1]))['items'][0]['code'])" "$TMP/body" 2>/dev/null)
  req p guest GET "/ecommerce/products/$pcode"
  expect "GET /ecommerce/products/<existing code> still -> 200" '[ -n "$pcode" ] && [ "$CODE" = 200 ]'

  if ! mail_q "select 1" >/dev/null 2>&1; then echo "SKIP  mail checks: sandbox DB not reachable ($MAIL_CONTAINER)"; return; fi
  local u="ecmr${STAMP}" e="ecm-test+r${STAMP}@example.com" old="Old1-$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 12)" new="New1-$(LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 12)"
  CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -H 'Content-Type: application/json' -X POST "$BASE_URL/ezylogin/api/v1/register" \
    -d "{\"username\":\"$u\",\"password\":\"$old\",\"email\":\"$e\",\"displayName\":\"ECM Reset\"}"); CTYPE=
  expect "register a dedicated reset-test user" '[ "$CODE" = 200 ]'

  # same answer for a known and an unknown address
  local n0; n0=$(mail_count "$e")
  req r1 guest POST /ezylogin/api/v1/forgot-password "{\"email\":\"$e\"}"; local known="$CODE"
  req r2 guest POST /ezylogin/api/v1/forgot-password "{\"email\":\"nobody-$STAMP@example.com\"}"
  expect "forgot-password answers the same for a known and an unknown email (204/204)" '[ "$known" = 204 ] && [ "$CODE" = 204 ]'
  expect "the reset mail reaches the sandbox" 'mail_wait "$e" "$n0"'
  local link uuid token
  link=$(mail_q "select mail_content from ezymail_mails where mail_to like '%$e%' order by id desc limit 1" | grep -oE "[^ \"'<]*/reset-password\?[^ \"'<]*" | head -1)
  uuid=$(sed -n 's/.*uuid=\([^&]*\).*/\1/p' <<<"$link"); token=$(sed -n 's/.*token=\([^&]*\).*/\1/p' <<<"$link")
  expect "the mail link opens the storefront reset page (/reset-password?uuid&token)" '[ -n "$uuid" ] && [ -n "$token" ]'
  expect "no mail was stored for the unknown address" '[ "$(mail_count "nobody-$STAMP@")" = 0 ]'

  req r3 guest POST /ezylogin/api/v1/reset-password "{\"userUuid\":\"$uuid\",\"forgotPasswordToken\":\"$token\",\"newPassword\":\"$new\"}"
  expect "reset with the mailed token -> 204" '[ "$CODE" = 204 ]'
  req r4 guest POST /ezylogin/api/v1/reset-password "{\"userUuid\":\"$uuid\",\"forgotPasswordToken\":\"$token\",\"newPassword\":\"Again1-$STAMP\"}"
  expect "the same link used a second time is refused (4xx)" '[ "${CODE:0:1}" = 4 ]'
  login_try l1 "$u" "$new"
  expect "login with the new password -> 200" '[ "$CODE" = 200 ]'
  login_try l2 "$u" "$old"
  expect "login with the old password is refused (4xx)" '[ "${CODE:0:1}" = 4 ] && [ "$CODE" != 429 ]'

  # per-client limit (counted per endpoint): at most 5 per window, so the 429 comes on the 4th to 6th request here
  # (2 already used when the window is still open, none when a slow login wait let it expire)
  local i first_429=0
  for i in 1 2 3 4 5 6; do
    req r5 guest POST /ezylogin/api/v1/forgot-password "{\"email\":\"nobody$i-$STAMP@example.com\"}"
    [ "$CODE" = 429 ] && { first_429=$i; break; }
  done
  expect "forgot-password is refused with 429 after 5 requests in the window (first 429 at request $first_429 of this burst)" '[ "$first_429" -ge 4 ]'

  # contact: per-field validation, header injection, honeypot, then the mail
  local c='"name":"Nguyễn Thị An","email":"ann@example.com","topic":"order","message":"Where is my order?"'
  req k1 guest POST /api/v1/ecm/contact '{}'
  expect "contact with nothing -> 400 listing name, email and message" \
    '[ "$CODE" = 400 ] && grep -q "\"name\":\"required\"" "$TMP/body" && grep -q "\"email\":\"required\"" "$TMP/body" && grep -q "\"message\":\"required\"" "$TMP/body"'
  req k2 guest POST /api/v1/ecm/contact "{\"name\":\"Ann\",\"email\":\"ann@example.com\\r\\nBcc: evil@example.com\",\"message\":\"hi\"}"
  expect "CR/LF in the email -> 400 (no header injection)" '[ "$CODE" = 400 ] && grep -q "\"email\":\"invalid\"" "$TMP/body"'
  req k3 guest POST /api/v1/ecm/contact "{\"name\":\"Ann\\r\\nBcc: evil@example.com\",\"email\":\"ann@example.com\",\"message\":\"hi\"}"
  expect "CR/LF in the name -> 400" '[ "$CODE" = 400 ] && grep -q "\"name\":\"invalid\"" "$TMP/body"'
  local shop="${SHOP_MAIL:-stores@ecommerce.local}" m0; m0=$(mail_count "$shop")
  req k4 guest POST /api/v1/ecm/contact "{$c,\"phone\":\"+84 912 345 678\",\"website\":\"\"}"
  expect "valid contact -> 200 accepted" '[ "$CODE" = 200 ] && grep -q "\"accepted\":true" "$TMP/body"'
  expect "contact mail reaches the shop mailbox with the sender as Reply-To" \
    'mail_wait "$shop" "$m0" && mail_q "select headers from ezymail_mails where mail_to like \"%$shop%\" order by id desc limit 1" | grep -q "ann@example.com"'
  m0=$(mail_count "$shop")
  req k5 guest POST /api/v1/ecm/contact "{$c,\"website\":\"http://spam.example\"}"
  sleep 3
  expect "honeypot filled -> 200 but no mail is sent" '[ "$CODE" = 200 ] && [ "$(mail_count "$shop")" = "$m0" ]'
  req k6 guest POST /api/v1/ecm/contact "{$c}"
  expect "6th contact request in the window -> 429 with Retry-After" '[ "$CODE" = 429 ]'

  # appointment
  local day; day=$(python3 -c "import datetime;print((datetime.date.today()+datetime.timedelta(days=3)).isoformat())")
  req a1 guest POST /api/v1/ecm/appointments '{"storeId":"nope","date":"2020-01-01","time":"03:00","name":"","phone":"abc","email":"x"}'
  expect "appointment with bad store/date/time/phone/email -> 400 per field" \
    '[ "$CODE" = 400 ] && for f in storeId date time name phone email; do grep -q "\"$f\"" "$TMP/body" || exit 1; done'
  req a2 guest POST /api/v1/ecm/appointments "{\"storeId\":\"ha-noi\",\"date\":\"$day\",\"time\":\"11:00\",\"name\":\"Nguyễn Thị An\",\"phone\":\"+84 912 345 678\",\"email\":\"ann@example.com\\nBcc: evil@example.com\"}"
  expect "LF in the appointment email -> 400" '[ "$CODE" = 400 ] && grep -q "\"email\":\"invalid\"" "$TMP/body"'
  m0=$(mail_count "$shop")
  req a3 guest POST /api/v1/ecm/appointments "{\"storeId\":\"ha-noi\",\"date\":\"$day\",\"time\":\"11:00\",\"name\":\"Nguyễn Thị An\",\"phone\":\"+84 912 345 678\",\"email\":\"ann@example.com\",\"note\":\"Ring sizing\"}"
  expect "valid appointment -> 200 accepted" '[ "$CODE" = 200 ] && grep -q "\"accepted\":true" "$TMP/body"'
  expect "appointment mail names the store (display name), date and time" \
    'mail_wait "$shop" "$m0" && mail_q "select mail_content from ezymail_mails where mail_to like \"%$shop%\" order by id desc limit 1" | grep -q "Khang Nguyên Hà Nội" '
  req a4 guest POST /api/v1/ecm/appointments "{\"storeId\":\"ha-noi\",\"date\":\"$day\",\"time\":\"09:00\",\"name\":\"A\",\"phone\":\"123\",\"email\":\"ann@example.com\"}"
  req a5 guest POST /api/v1/ecm/appointments "{\"storeId\":\"ha-noi\",\"date\":\"$day\",\"time\":\"11:15\",\"name\":\"A\",\"phone\":\"+84912345678\",\"email\":\"ann@example.com\"}"
  req a6 guest POST /api/v1/ecm/appointments "{\"storeId\":\"ha-noi\",\"date\":\"$day\",\"time\":\"11:00\",\"name\":\"A\",\"phone\":\"+84912345678\",\"email\":\"ann@example.com\"}"
  expect "6th appointment request in the window -> 429" '[ "$CODE" = 429 ]'

  # Origin guard: 127.0.0.1 and localhost are one site on the same port; any other origin stays refused
  [ -s "$JAR" ] || register_user >/dev/null
  local ph="+84$((RANDOM % 9 + 1))$(printf '%08d' $(( (RANDOM * 32768 + RANDOM) % 100000000 )))" origin
  local upd='{"updateUser":true,"updateCustomer":true,"displayName":"ECM Test","email":"'"$EMAIL"'","phone":"'"$ph"'","dateOfBirth":0,"addressNumber":"","addressCountryId":0,"addressProvinceId":0,"addressDistrictId":0,"addressWardId":0}'
  for origin in http://127.0.0.1:8080 http://localhost:8080; do
    CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X PUT -H "Origin: $origin" -H "X-Requested-With: ecm" -H 'Content-Type: application/json' -d "$upd" "$BASE_URL/ezyaccount/api/v1/account/update"); CTYPE=
    expect "profile update with Origin $origin -> 204" '[ "$CODE" = 204 ]'
  done
  CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X PUT -H "Origin: https://evil.example" -H "X-Requested-With: ecm" -H 'Content-Type: application/json' -d "$upd" "$BASE_URL/ezyaccount/api/v1/account/update"); CTYPE=
  expect "profile update with a foreign Origin -> 403" '[ "$CODE" = 403 ]'
  CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X PUT -H "Origin: http://localhost:9999" -H "X-Requested-With: ecm" -H 'Content-Type: application/json' -d "$upd" "$BASE_URL/ezyaccount/api/v1/account/update"); CTYPE=
  expect "profile update with localhost on another port -> 403" '[ "$CODE" = 403 ]'
}

section_f5() {
  echo "== f5: wishlist (own items only, guest refused, Origin guard)"
  local pid="${PRODUCT_ID:-3}" jarB="$TMP/jarB" w=/api/v1/ecm/wishlist
  [ -s "$JAR" ] || register_user >/dev/null
  req w1 guest GET "$w"
  expect "guest GET wishlist -> 401" '[ "$CODE" = 401 ]'
  req w2 guest PUT "$w/$pid"
  expect "guest PUT wishlist -> 401 or 403" '[ "$CODE" = 401 ] || [ "$CODE" = 403 ]'
  req w3 user PUT "$w/$pid"
  expect "user PUT wishlist/<product> -> 204" '[ "$CODE" = 204 ]'
  req w4 user PUT "$w/$pid"
  expect "second PUT is idempotent -> 204" '[ "$CODE" = 204 ]'
  req w5 user GET "$w"
  expect "wishlist has exactly 1 item holding the product" \
    'grep -q "\"total\":1" "$TMP/body" && grep -q "\"productIds\":\[$pid\]" "$TMP/body"'
  req w6 user GET "$w/ids"
  expect "wishlist/ids lists the product" 'grep -q "\"ids\":\[$pid\]" "$TMP/body"'
  req w7 user PUT "$w/99999999"
  expect "PUT unknown product -> 404" '[ "$CODE" = 404 ]'
  CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X PUT -H "Origin: https://evil.example" -H "X-Requested-With: ecm" "$BASE_URL$w/$pid"); CTYPE=
  expect "PUT with a foreign Origin -> 403" '[ "$CODE" = 403 ]'
  CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -b "$JAR" -X DELETE -H "Origin: $ORIGIN" "$BASE_URL$w/$pid"); CTYPE=
  expect "DELETE without X-Requested-With -> 403, item kept" '[ "$CODE" = 403 ] && req w8 user GET "$w" && grep -q "\"total\":1" "$TMP/body"'
  # user B must neither see nor delete A's item
  local saveJ="$JAR" saveU="$USERNAME" saveE="$EMAIL"
  JAR="$jarB"; USERNAME="${USERNAME}w"; EMAIL="w-${EMAIL}"
  local rc try; for try in 1 2 3; do rc=$(register_user); [ "$rc" = 200 ] && break; sleep 20; done
  CODE=$rc; CTYPE=; : >"$TMP/body"
  expect "register second throw-away user (HTTP $rc)" '[ "$rc" = 200 ]'
  req w9 user GET "$w"
  expect "user B sees none of A's items" 'grep -q "\"total\":0" "$TMP/body" && ! grep -q "\"productIds\":\[$pid\]" "$TMP/body"'
  req w10 user DELETE "$w/$pid"
  expect "user B DELETE of the same product -> 204 (no-op)" '[ "$CODE" = 204 ]'
  JAR="$saveJ"; USERNAME="$saveU"; EMAIL="$saveE"
  req w11 user GET "$w"
  expect "A's item survived B's DELETE" 'grep -q "\"total\":1" "$TMP/body"'
  req w12 user DELETE "$w/$pid"
  expect "A DELETE -> 204" '[ "$CODE" = 204 ]'
  req w13 user GET "$w"
  expect "wishlist empty again" 'grep -q "\"total\":0" "$TMP/body"'
  req w14 guest GET /wishlist
  expect "GET /wishlist page -> 200 storefront page" '[ "$CODE" = 200 ] && [[ "$CTYPE" == text/html* ]] && ! is_not_found_page'
}

section_f10() {
  echo "== f10: newsletter double opt-in (needs the local sandbox mail DB)"
  if ! mail_q "select 1" >/dev/null 2>&1; then echo "SKIP  newsletter mail checks: sandbox DB not reachable ($MAIL_CONTAINER)"; return; fi
  local n=/api/v1/ecm/newsletter addr="nl${STAMP}@example.com" m0 tok utok
  req n1 guest POST "$n" '{"email":"not-an-email"}'
  expect "invalid email -> 400 email:invalid" '[ "$CODE" = 400 ] && grep -q "\"email\":\"invalid\"" "$TMP/body"'
  req n2 guest POST "$n" '{"email":""}'
  expect "empty email -> 400 email:required" '[ "$CODE" = 400 ] && grep -q "\"email\":\"required\"" "$TMP/body"'
  CODE=$(curl -s -o "$TMP/body" -w '%{http_code}' -X POST -H "Origin: https://evil.example" -H "X-Requested-With: ecm" -H 'Content-Type: application/json' -d "{\"email\":\"$addr\"}" "$BASE_URL$n"); CTYPE=
  expect "foreign Origin -> 403" '[ "$CODE" = 403 ]'
  m0=$(mail_count "$addr")
  req n3 guest POST "$n" "{\"email\":\"$addr\"}"
  expect "sign-up -> 200 accepted" '[ "$CODE" = 200 ] && grep -q "\"accepted\":true" "$TMP/body"'
  local first; first=$(cat "$TMP/body")
  expect "confirmation mail reaches the sandbox" 'mail_wait "$addr" "$m0"'
  tok=$(mail_q "select mail_content from ezymail_mails where mail_to like \"%$addr%\" order by id desc limit 1" | grep -o 'confirm?token=[0-9a-f]\{64\}' | head -1 | sed 's/.*=//')
  expect "mail holds a /newsletter/confirm?token= link" '[ -n "$tok" ]'
  req n4 guest POST "$n" "{\"email\":\"$addr\"}"
  expect "duplicate sign-up answers identically (no email enumeration)" '[ "$CODE" = 200 ] && [ "$(cat "$TMP/body")" = "$first" ]'
  req n5 guest POST "$n/confirm" '{"token":"0000000000000000000000000000000000000000000000000000000000000000"}'
  expect "bad confirm token -> 404" '[ "$CODE" = 404 ]'
  req n6 guest POST "$n/confirm" "{\"token\":\"$tok\"}"
  expect "confirm -> 200 confirmed" '[ "$CODE" = 200 ] && grep -q "confirmed" "$TMP/body"'
  expect "row is confirmed in the database" '[ "$(mail_q "select status from ecm_newsletter where email=\"$addr\"")" = confirmed ]'
  m0=$(mail_count "$addr")
  req n7 guest POST "$n" "{\"email\":\"$addr\"}"
  expect "sign-up of a confirmed address sends no second mail" '[ "$CODE" = 200 ] && sleep 3 && [ "$(mail_count "$addr")" = "$m0" ]'
  utok=$(mail_q "select unsubscribe_token from ecm_newsletter where email=\"$addr\"")
  req n8 guest POST "$n/unsubscribe" "{\"token\":\"$utok\"}"
  expect "unsubscribe -> 200" '[ "$CODE" = 200 ] && grep -q "unsubscribed" "$TMP/body"'
  req n9 guest POST "$n/confirm" "{\"token\":\"$tok\"}"
  expect "old confirm token no longer works after unsubscribe -> 404" '[ "$CODE" = 404 ]'
  req n10 guest GET /newsletter/confirm
  expect "GET /newsletter/confirm page -> 200 storefront page" '[ "$CODE" = 200 ] && [[ "$CTYPE" == text/html* ]] && ! is_not_found_page'
  local i last=200
  for i in $(seq 1 12); do req n11 guest POST "$n" "{\"email\":\"rl${STAMP}${i}@example.com\"}"; last=$CODE; [ "$last" = 429 ] && break; done
  expect "per-IP rate limit -> 429" '[ "$last" = 429 ]'
}

SECTIONS=("$@"); [ ${#SECTIONS[@]} -eq 0 ] && SECTIONS=(m1 m2 m3 f1 f2 f3 f4 f5 f10)
for s in "${SECTIONS[@]}"; do "section_$s"; done
echo "-- $PASS passed, $FAIL failed (BASE_URL=$BASE_URL)"
[ "$FAIL" -eq 0 ]
