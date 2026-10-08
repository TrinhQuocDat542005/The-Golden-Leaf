#!/usr/bin/env bash
set -euo pipefail
umask 077
export MSYS_NO_PATHCONV=1
fail() { printf '%s\n' "$*" >&2; exit 1; }
curl_sink() {
  case ${OSTYPE:-} in msys*|cygwin*) printf 'NUL';; *) printf '/dev/null';; esac
}
docker_path() {
  local resolved
  resolved=$(realpath "$1")
  if command -v cygpath >/dev/null 2>&1; then cygpath -m "$resolved"; else printf '%s\n' "$resolved"; fi
}
require_database() { [[ ${DB_NAME:-} =~ ^[A-Za-z][A-Za-z0-9_]{0,63}$ ]] || fail 'Invalid DB_NAME'; }
require_client() {
  : "${MYSQL_CNF:?Set MYSQL_CNF to a restricted MySQL client credentials file}"
  : "${DOCKER_NETWORK:?Set the explicit database Docker network}"
  [[ -f $MYSQL_CNF ]] || fail 'MySQL credentials file is missing'
  MYSQL_CNF_MOUNT=$(docker_path "$MYSQL_CNF")
  MYSQL_IMAGE=${MYSQL_IMAGE:-mysql:8.0}
  CRYPTO_IMAGE=${CRYPTO_IMAGE:-golden-leaf-backup-crypto:local}
}
mysql_client() {
  local executable=$1
  shift
  docker run --rm -i --network "$DOCKER_NETWORK" --read-only --cap-drop ALL \
    --user "$(id -u):$(id -g)" \
    --security-opt no-new-privileges:true \
    --tmpfs "/run/client:rw,noexec,nosuid,size=1m,mode=0700,uid=$(id -u),gid=$(id -g)" \
    --mount "type=bind,source=$MYSQL_CNF_MOUNT,target=/run/secrets/mysql.cnf,readonly" \
    --entrypoint /bin/bash "$MYSQL_IMAGE" -c \
    'cp /run/secrets/mysql.cnf /run/client/mysql.cnf; chmod 600 /run/client/mysql.cnf; exec "$1" --defaults-extra-file=/run/client/mysql.cnf "${@:2}"' \
    mysql-client "$executable" "$@"
}
