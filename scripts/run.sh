#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v java >/dev/null || { echo 'Java 17+ is required.' >&2; exit 1; }
if [[ "${1:-}" == '--build' ]]; then shift; ./scripts/build.sh; fi
if [[ -f releases/placesure.jar ]]; then jar='releases/placesure.jar'
elif [[ -f target/placesure.jar ]]; then jar='target/placesure.jar'
else ./scripts/build.sh; jar='releases/placesure.jar'; fi
if [[ ! -f .env && -z "${DB_PASSWORD:-}" ]]; then
  echo 'Configuration missing. Copy .env.example to .env and set local database credentials, or export DB_URL, DB_USER, and DB_PASSWORD.' >&2
fi
# Copy to a unique runtime file: rebuilding the target JAR must not corrupt a live classloader.
mkdir -p .runtime
runtime_jar="$(mktemp .runtime/placesure-XXXXXX.jar)"
cp "$jar" "$runtime_jar"
trap 'rm -f "$runtime_jar"' EXIT
java -jar "$runtime_jar" "$@"
