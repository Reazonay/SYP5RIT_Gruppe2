#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_database="restaurant_test_${RANDOM}_${RANDOM}"
docker compose exec -T db createdb -U restaurant "$test_database"
trap 'docker compose exec -T db dropdb -U restaurant --force "$test_database"' EXIT
export DATABASE_URL="postgres://restaurant:restaurant@localhost:5432/$test_database"
RUN_DATABASE_TESTS=1 bash scripts/mvn.sh test
