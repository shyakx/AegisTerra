#!/bin/sh
set -eu

PORT="${PORT:-80}"
export PORT

convert_db_url() {
  url="${AEGISTERRA_DB_URL:-${DATABASE_URL:-}}"
  if [ -z "$url" ]; then
    echo "AEGISTERRA_DB_URL or DATABASE_URL is required" >&2
    exit 1
  fi

  case "$url" in
    jdbc:*)
      export AEGISTERRA_DB_URL="$url"
      return
      ;;
  esac

  rest="${url#*://}"
  userpass="${rest%%@*}"
  hostdb="${rest#*@}"
  user="${userpass%%:*}"
  pass="${userpass#*:}"

  export AEGISTERRA_DB_USER="${AEGISTERRA_DB_USER:-$user}"
  export AEGISTERRA_DB_PASSWORD="${AEGISTERRA_DB_PASSWORD:-$pass}"

  case "$hostdb" in
    db:*|db/*)
      export AEGISTERRA_DB_URL="jdbc:postgresql://${hostdb}"
      ;;
    *)
      case "$hostdb" in
        *\?*) export AEGISTERRA_DB_URL="jdbc:postgresql://${hostdb}&sslmode=require" ;;
        *)    export AEGISTERRA_DB_URL="jdbc:postgresql://${hostdb}?sslmode=require" ;;
      esac
      ;;
  esac
}

convert_db_url

envsubst '${PORT}' < /etc/nginx/templates/default.conf.template > /etc/nginx/conf.d/default.conf

JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=prod &
JAVA_PID=$!

trap 'kill $JAVA_PID 2>/dev/null || true' EXIT INT TERM

i=0
until curl -fsS "http://127.0.0.1:8080/api/v1/health" >/dev/null 2>&1; do
  i=$((i + 1))
  if [ "$i" -gt 60 ]; then
    echo "Backend did not become healthy in time" >&2
    wait "$JAVA_PID"
    exit 1
  fi
  sleep 2
done

nginx -g 'daemon off;'
