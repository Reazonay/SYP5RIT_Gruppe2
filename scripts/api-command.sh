#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_root"
command_name="${1:?Usage: api-command.sh migrate|seed}"
case "$command_name" in migrate|seed) ;; *) echo 'Expected migrate or seed' >&2; exit 2;; esac
if [ ! -f target/restaurant.jar ]; then bash scripts/mvn.sh -q -DskipTests package; fi
exec java -jar target/restaurant.jar --spring.main.web-application-type=none --app.jobs.enabled=false --app.command="$command_name"
