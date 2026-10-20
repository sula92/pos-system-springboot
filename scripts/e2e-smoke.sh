#!/usr/bin/env bash
# End-to-end smoke test: starts Eureka, the three services and the gateway from the built jars,
# then checks registration, gateway routing, Feign order placement, the WebClient stats call
# and the circuit-breaker fallbacks. Run after `./mvnw -B package -DskipTests` (or `verify`).
set -euo pipefail

VERSION="0.0.1-SNAPSHOT"
GW="http://localhost:8080/pos-system"
LOG_DIR="${LOG_DIR:-e2e-logs}"
mkdir -p "$LOG_DIR"
declare -A PIDS=()

start() {
  local module="$1"
  java -jar "$module/target/$module-$VERSION.jar" \
    --eureka.client.registry-fetch-interval-seconds=5 \
    --eureka.instance.lease-renewal-interval-in-seconds=5 \
    --spring.cloud.loadbalancer.cache.ttl=5s \
    > "$LOG_DIR/$module.log" 2>&1 &
  PIDS[$module]=$!
  echo "started $module (pid ${PIDS[$module]})"
}

stop_all() {
  for pid in "${PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done
}
trap stop_all EXIT

fail() {
  echo "FAIL: $*"
  exit 1
}

wait_for() {
  local name="$1" url="$2" expected="${3:-200}" timeout="${4:-180}" code=""
  for ((i = 0; i < timeout; i += 3)); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "$url" || true)
    if [[ "$code" == "$expected" ]]; then
      echo "ok: $name ($url -> $code)"
      return 0
    fi
    sleep 3
  done
  fail "$name: expected $expected from $url, last status was $code"
}

# status METHOD URL [JSON_BODY] -> prints the HTTP status; the body is saved to /tmp/e2e-body
status() {
  local method="$1" url="$2" body="${3:-}"
  if [[ -n "$body" ]]; then
    curl -s -o /tmp/e2e-body -w '%{http_code}' -X "$method" -H 'Content-Type: application/json' -d "$body" "$url" || true
  else
    curl -s -o /tmp/e2e-body -w '%{http_code}' -X "$method" "$url" || true
  fi
}

expect() {
  local name="$1" expected="$2" actual="$3"
  if [[ "$actual" != "$expected" ]]; then
    cat /tmp/e2e-body 2>/dev/null || true
    echo
    fail "$name: expected $expected, got $actual"
  fi
  echo "ok: $name ($actual)"
}

start eureka-server
wait_for "eureka-server up" "http://localhost:8761/eureka/apps" 200 120

start customer-service
start item-service
start order-service
start api-gateway

# Wait for registration in Eureka first. Polling the gateway before the services are registered
# counts as failures and can open the gateway circuit breakers.
for app in CUSTOMER-SERVICE ITEM-SERVICE ORDER-SERVICE API-GATEWAY; do
  wait_for "$app registered in Eureka" "http://localhost:8761/eureka/apps/$app"
done
# Give every client time to refresh its registry copy and load-balancer cache.
sleep 15

wait_for "gateway -> customer-service" "$GW/customer"
wait_for "gateway -> item-service" "$GW/item"
wait_for "gateway -> order-service" "$GW/order"
# If a breaker opened during start-up, let it move to half-open before the real checks.
sleep 11

# Data set-up through the gateway
expect "create customer" 201 "$(status POST "$GW/customer" '{"id":"C001","name":"Nimal Perera","address":"Colombo","email":"nimal@example.com"}')"
expect "create item" 201 "$(status POST "$GW/item" '{"code":"I001","description":"Rice 1kg","unitPrice":250.0}')"
expect "set stock to 10" 200 "$(status PUT "$GW/inventory" '{"itemCode":"I001","qty":10}')"

# Placing an order uses Feign to check the customer and to reserve stock in item-service.
# Retry briefly: order-service needs both services in its own copy of the registry.
code=""
for ((i = 0; i < 60; i += 3)); do
  code=$(status POST "$GW/order" '{"customerId":"C001","orderDetails":[{"itemCode":"I001","qty":2,"unitPrice":250.0}]}')
  [[ "$code" == "201" ]] && break
  sleep 3
done
expect "place order (Feign customer check + stock reserve)" 201 "$code"

expect "read stock" 200 "$(status GET "$GW/inventory?code=I001")"
qty=$(jq -r '.qty' /tmp/e2e-body)
[[ "$qty" == "8" ]] || fail "stock after order: expected 8, got $qty"
echo "ok: stock decremented to 8"

expect "unknown customer is 404 through Feign" 404 "$(status POST "$GW/order" '{"customerId":"C999","orderDetails":[{"itemCode":"I001","qty":1,"unitPrice":250.0}]}')"
expect "too much stock is 409 through Feign" 409 "$(status POST "$GW/order" '{"customerId":"C001","orderDetails":[{"itemCode":"I001","qty":999,"unitPrice":250.0}]}')"

# customer-service gets the order totals from order-service with WebClient
orders=""
for ((i = 0; i < 60; i += 3)); do
  status GET "$GW/customer/purchase-stats-dto" > /dev/null
  orders=$(jq -r '.[] | select(.customerId=="C001") | .totalOrders' /tmp/e2e-body 2>/dev/null || true)
  [[ "$orders" == "1" ]] && break
  sleep 3
done
if [[ "$orders" != "1" ]]; then
  cat /tmp/e2e-body
  fail "purchase stats: expected 1 order for C001, got '$orders'"
fi
echo "ok: purchase stats via WebClient show 1 order for C001"

# Internal endpoints must not be reachable through the gateway
expect "internal order endpoint blocked" 404 "$(status GET "$GW/order/internal/customer-totals")"
expect "internal inventory endpoint blocked" 404 "$(status POST "$GW/inventory/internal/reserve" '[{"itemCode":"I001","qty":1}]')"

# Circuit-breaker fallbacks: stop item-service
kill "${PIDS[item-service]}"
wait "${PIDS[item-service]}" 2>/dev/null || true
unset 'PIDS[item-service]'
echo "stopped item-service"
sleep 5

expect "gateway fallback for item-service" 503 "$(status GET "$GW/item")"
expect "order placement degrades to 503 (Feign fallback)" 503 "$(status POST "$GW/order" '{"customerId":"C001","orderDetails":[{"itemCode":"I001","qty":1,"unitPrice":250.0}]}')"
expect "customer-service still serves requests" 200 "$(status GET "$GW/customer")"

echo "E2E smoke test passed"
