#!/usr/bin/env bash
# Container entrypoint. Secrets are environment variables, never command-line arguments or logs.
set -euo pipefail

fail() { printf 'PlaceSure startup: %s\n' "$1" >&2; exit 1; }
integer() {
  [[ "$2" =~ ^[0-9]+$ ]] && (( 10#$2 >= $3 && 10#$2 <= $4 )) || fail "$1 must be a whole number from $3 to $4."
}

: "${APP_JAR:=/app/placesure.jar}"
: "${PORT:=8080}"
: "${BIND_HOST:=0.0.0.0}"
: "${DB_PORT:=3306}"
: "${DB_NAME:=placesure}"
: "${DB_USER:=placesure_app}"
: "${DB_SSL_MODE:=DISABLED}"
: "${DB_WAIT_SECONDS:=90}"
: "${SEED_DEMO:=false}"
integer PORT "$PORT" 1 65535
integer DB_PORT "$DB_PORT" 1 65535
integer DB_WAIT_SECONDS "$DB_WAIT_SECONDS" 0 300
[[ -f "$APP_JAR" ]] || fail 'The application JAR is missing.'
[[ -n "${DB_PASSWORD:-}" ]] || fail 'DB_PASSWORD must be supplied as a secret environment variable.'
[[ "$SEED_DEMO" == true || "$SEED_DEMO" == false ]] || fail 'SEED_DEMO must be true or false.'

if [[ -z "${DB_URL:-}" ]]; then
  [[ -n "${DB_HOST:-}" ]] || fail 'Set DB_HOST or a complete DB_URL.'
  [[ "$DB_HOST" =~ ^[A-Za-z0-9][A-Za-z0-9.-]*$ ]] || fail 'DB_HOST must be a hostname or IPv4 address. For IPv6, supply DB_URL directly.'
  [[ "$DB_NAME" =~ ^[A-Za-z][A-Za-z0-9_]*$ ]] || fail 'DB_NAME must contain letters, numbers, and underscores only.'
  case "$DB_SSL_MODE" in
    DISABLED|PREFERRED|REQUIRED|VERIFY_CA|VERIFY_IDENTITY) ;;
    *) fail 'DB_SSL_MODE is not a recognized MySQL SSL mode.' ;;
  esac
  export DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?sslMode=${DB_SSL_MODE}&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8"
fi
[[ "$DB_URL" == jdbc:mysql://* ]] || fail 'DB_URL must be a MySQL JDBC URL, beginning jdbc:mysql://.'
export PORT BIND_HOST DB_USER DB_PASSWORD

# MySQL's official image uses a socket-only server during initialization. Its TCP listener
# starts after schema/import setup, so this avoids racing a fresh private database.
# Full DB_URL users may omit DB_HOST to connect to an already-running managed database.
if [[ -n "${DB_HOST:-}" ]] && (( 10#$DB_WAIT_SECONDS > 0 )); then
  printf 'PlaceSure startup: waiting for the MySQL TCP listener (up to %s seconds).\n' "$DB_WAIT_SECONDS"
  deadline=$((SECONDS + 10#$DB_WAIT_SECONDS))
  until timeout 1 bash -c 'exec 3<>/dev/tcp/"$1"/"$2"' bash "$DB_HOST" "$DB_PORT" 2>/dev/null; do
    (( SECONDS < deadline )) || fail 'MySQL did not become reachable in time. Check the private service and credentials.'
    sleep 1
  done
fi

args=()
[[ "$SEED_DEMO" == false ]] || args+=(--seed-demo)
printf 'PlaceSure startup: launching the Java application.\n'
exec java -jar "$APP_JAR" "${args[@]}"
