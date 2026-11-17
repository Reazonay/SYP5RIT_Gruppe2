#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if curl -sS --max-time 1 -o /dev/null http://localhost:3000 2>/dev/null; then
  echo 'Port 3000 ist belegt. Bitte die laufende Anwendung vor dem Browsertest beenden.' >&2
  exit 1
fi
test_database="restaurant_browser_${RANDOM}_${RANDOM}"
test_logs="$(mktemp -d)"
api_pid=""
cleanup() {
  if [ -n "$api_pid" ]; then kill "$api_pid" 2>/dev/null || true; wait "$api_pid" 2>/dev/null || true; fi
  docker compose exec -T db dropdb -U restaurant --force "$test_database"
}
trap cleanup EXIT
docker compose exec -T db createdb -U restaurant "$test_database"
export DATABASE_URL="postgres://restaurant:restaurant@localhost:5432/$test_database"
export SEED_PASSWORD="browser-test-password-only"
export PUBLIC_URL="http://localhost:3000"
export APP_JOBS_ENABLED=false
export PORT=3000
bash scripts/mvn.sh -q -DskipTests package
bash scripts/api-command.sh seed > "$test_logs/seed.log" 2>&1
java -jar target/restaurant.jar > "$test_logs/api.log" 2>&1 &
api_pid=$!
ready=false
for attempt in {1..100}; do
  if curl -fsS http://localhost:3000/api/health >/dev/null 2>&1; then ready=true; break; fi
  sleep 0.2
done
if [ "$ready" != true ]; then cat "$test_logs/api.log"; exit 1; fi
if ! RUN_BROWSER_TESTS=1 bash scripts/mvn.sh -Dtest=BrowserTest test; then
  cat "$test_logs/api.log"
  exit 1
fi
