#!/usr/bin/env bash
# Linux/Git Bash + Docker. No plaintext dump is ever written to host disk.
set -euo pipefail
# shellcheck source=ops/scripts/common.sh
source "$(dirname "$0")/common.sh"
require_database
require_client
: "${BACKUP_RECIPIENT:?Set an age public recipient (private key must be stored separately)}"
: "${BACKUP_DIR:?Set a restricted backup destination}"
mkdir -p "$BACKUP_DIR"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
name="$DB_NAME-$stamp-$(od -An -N4 -tx1 /dev/urandom | tr -d ' \n')"
partial="$BACKUP_DIR/$name.sql.gz.age.partial"
trap 'rm -f -- "$partial"' EXIT
mysql_client mysqldump --single-transaction --quick --no-tablespaces --set-gtid-purged=OFF \
  --routines --events --triggers "$DB_NAME" | gzip -c | \
  docker run --rm -i --network none --read-only --cap-drop ALL "$CRYPTO_IMAGE" -r "$BACKUP_RECIPIENT" > "$partial"
[[ -s $partial ]] || fail 'Empty encrypted backup'
mv -- "$partial" "$BACKUP_DIR/$name.sql.gz.age"
(cd "$BACKUP_DIR" && sha256sum "$name.sql.gz.age" > "$name.sql.gz.age.sha256")
if [[ -n ${UPLOADS_VOLUME:-} ]]; then
  docker volume inspect "$UPLOADS_VOLUME" >/dev/null
  partial="$BACKUP_DIR/$name.uploads.tar.gz.age.partial"
  docker run --rm --network none --read-only --cap-drop ALL --entrypoint tar \
    --mount "type=volume,source=$UPLOADS_VOLUME,target=/uploads,readonly" "$CRYPTO_IMAGE" -czf - -C /uploads . | \
    docker run --rm -i --network none --read-only --cap-drop ALL "$CRYPTO_IMAGE" -r "$BACKUP_RECIPIENT" > "$partial"
  mv -- "$partial" "$BACKUP_DIR/$name.uploads.tar.gz.age"
  (cd "$BACKUP_DIR" && sha256sum "$name.uploads.tar.gz.age" > "$name.uploads.tar.gz.age.sha256")
fi
printf 'Encrypted database backup: %s/%s.sql.gz.age\n' "$BACKUP_DIR" "$name"
printf 'No retention deletion performed. Copy encrypted files off-host and verify restore regularly.\n'
