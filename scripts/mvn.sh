#!/usr/bin/env bash
set -euo pipefail
project_root="$(cd "$(dirname "$0")/.." && pwd)"
exec mvn -B -Dmaven.repo.local="${MAVEN_REPO_LOCAL:-$project_root/.cache/maven}" -f "$project_root/pom.xml" "$@"
