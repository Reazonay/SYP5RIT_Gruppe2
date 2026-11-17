#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_database="restaurant_browser_${RANDOM}_${RANDOM}"
docker compose exec -T db createdb -U restaurant "$test_database"
trap 'docker compose exec -T db dropdb -U restaurant --force "$test_database"' EXIT
export DATABASE_URL="postgres://restaurant:restaurant@localhost:5432/$test_database"
export SEED_PASSWORD="browser-test-password-only"
npm run migrate -w apps/api
npm run seed -w apps/api
node node_modules/playwright/cli.js test
