#!/usr/bin/env bash
# Intentionally refuses production names and nonempty schemas; no DROP/TRUNCATE/CREATE DATABASE.
set -euo pipefail
# shellcheck source=ops/scripts/common.sh
source "$(dirname "$0")/common.sh"
require_database
[[ $DB_NAME =~ _restore_[A-Za-z0-9_]+$ ]] || fail 'Restore target must have _restore_ suffix, never a live schema'
[[ ${RESTORE_CONFIRM:-} == "$DB_NAME" ]] || fail 'Set RESTORE_CONFIRM to the exact empty target schema name'
require_client
: "${BACKUP_FILE:?Set the encrypted .sql.gz.age file}"
: "${AGE_IDENTITY:?Set the separately stored age identity file}"
[[ -f $BACKUP_FILE && -f $AGE_IDENTITY && -f $BACKUP_FILE.sha256 ]] || fail 'Missing backup, checksum or identity'
backup_name=$(basename "$BACKUP_FILE")
[[ $backup_name =~ ^[A-Za-z0-9_.-]+\.sql\.gz\.age$ ]] || fail 'Invalid backup filename'
# Never execute checksum paths from an untrusted sidecar.
expected=$(awk 'NR==1 {print $1}' "$BACKUP_FILE.sha256")
[[ $expected =~ ^[a-fA-F0-9]{64}$ ]] || fail 'Invalid checksum'
actual=$(sha256sum "$BACKUP_FILE" | awk '{print $1}')
[[ $expected == "$actual" ]] || fail 'Checksum mismatch; restore aborted before writes'
identity_mount=$(docker_path "$AGE_IDENTITY")
decrypt() {
  docker run --rm -i --network none --read-only --cap-drop ALL \
    --mount "type=bind,source=$identity_mount,target=/run/secrets/identity,readonly" \
    "$CRYPTO_IMAGE" -d -i /run/secrets/identity < "$BACKUP_FILE"
}
decrypt | gzip -t # Verify authenticated encryption and compression fully BEFORE DB mutation.
count=$(mysql_client mysql --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME'")
[[ $count == 0 ]] || fail 'Target schema is not empty; refuse overwrite'
exists=$(mysql_client mysql --batch --skip-column-names -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$DB_NAME'")
[[ $exists == 1 ]] || fail 'Target schema must already exist and be approved'
decrypt | gzip -dc | mysql_client mysql "$DB_NAME"
printf 'Restored into empty isolated schema %s. Validate rows/Flyway/application before any promotion.\n' "$DB_NAME"
