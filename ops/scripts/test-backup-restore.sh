#!/usr/bin/env bash
# Disposable-only integration drill, invoked by CI. No production files or schemas used.
set -euo pipefail
# shellcheck source=ops/scripts/common.sh
source "$(dirname "$0")/common.sh"
root=$(cd "$(dirname "$0")/../.." && pwd)
fixture=$(mktemp -d)
fixture_mount=$(docker_path "$fixture")
suffix="$(date +%s)-$RANDOM"
network="golden-leaf-drill-$suffix"
container="golden-leaf-drill-$suffix"
volume="golden-leaf-drill-uploads-$suffix"
cleanup() {
  docker stop "$container" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
  docker volume rm "$volume" >/dev/null 2>&1 || true
  # Only named fixture files generated in this mktemp directory; no recursive filesystem deletion.
  rm -f -- "$fixture/client.cnf" "$fixture/identity" "$fixture/wrong-identity" "$fixture"/*.age "$fixture"/*.sha256
  rmdir "$fixture" 2>/dev/null || true
}
trap cleanup EXIT
docker build -t golden-leaf-backup-crypto:local "$(docker_path "$root/ops/backup")"
docker network create --label golden-leaf.purpose=restore-drill "$network" >/dev/null
docker run -d --rm --name "$container" --label golden-leaf.purpose=restore-drill --network "$network" \
  --tmpfs /var/lib/mysql -e MYSQL_ROOT_PASSWORD=drill-only-password mysql:8.0 >/dev/null
export MYSQL_CNF="$fixture/client.cnf" DOCKER_NETWORK="$network" DB_NAME=golden_leaf_source
printf '[client]\nhost=%s\nuser=root\npassword=drill-only-password\n' "$container" > "$MYSQL_CNF"
require_client
ready=false
for _ in $(seq 1 90); do
  if mysql_client mysql -e 'SELECT 1' >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
[[ $ready == true ]] || fail 'Fixture MySQL not ready'
mysql_client mysql -e "CREATE DATABASE golden_leaf_source; CREATE DATABASE golden_leaf_restore_drill; CREATE TABLE golden_leaf_source.receipts(id INT PRIMARY KEY,amount DECIMAL(12,2)); INSERT INTO golden_leaf_source.receipts VALUES(1,200000.50);"
docker run --rm --network none --entrypoint age-keygen --mount "type=bind,source=$fixture_mount,target=/fixture" \
  golden-leaf-backup-crypto:local -o /fixture/identity 2>/dev/null
export BACKUP_RECIPIENT
BACKUP_RECIPIENT=$(docker run --rm --network none --entrypoint age-keygen --mount "type=bind,source=$fixture_mount,target=/fixture,readonly" golden-leaf-backup-crypto:local -y /fixture/identity)
export BACKUP_DIR="$fixture"
docker volume create --label golden-leaf.purpose=restore-drill "$volume" >/dev/null
docker run --rm --network none --entrypoint sh --mount "type=volume,source=$volume,target=/uploads" golden-leaf-backup-crypto:local \
  -c 'printf "fixture image content\n" > /uploads/menu-fixture.txt'
export UPLOADS_VOLUME="$volume"
bash "$root/ops/scripts/backup.sh"
export BACKUP_FILE AGE_IDENTITY="$fixture/identity" RESTORE_CONFIRM=golden_leaf_restore_drill DB_NAME=golden_leaf_restore_drill
BACKUP_FILE=$(find "$fixture" -maxdepth 1 -name '*.sql.gz.age' -print)
# A forged checksum cannot make a damaged encrypted stream valid.
original_backup=$BACKUP_FILE
cp "$original_backup" "$fixture/tampered.sql.gz.age"
printf 'tampered' >> "$fixture/tampered.sql.gz.age"
(cd "$fixture" && sha256sum tampered.sql.gz.age > tampered.sql.gz.age.sha256)
BACKUP_FILE="$fixture/tampered.sql.gz.age"
if bash "$root/ops/scripts/restore.sh" >/dev/null 2>&1; then fail 'Damaged encrypted backup incorrectly accepted'; fi
BACKUP_FILE=$original_backup
docker run --rm --network none --entrypoint age-keygen --mount "type=bind,source=$fixture_mount,target=/fixture" \
  golden-leaf-backup-crypto:local -o /fixture/wrong-identity 2>/dev/null
AGE_IDENTITY="$fixture/wrong-identity"
if bash "$root/ops/scripts/restore.sh" >/dev/null 2>&1; then fail 'Wrong private identity incorrectly accepted'; fi
AGE_IDENTITY="$fixture/identity"
bash "$root/ops/scripts/restore.sh"
amount=$(mysql_client mysql --batch --skip-column-names "$DB_NAME" -e 'SELECT amount FROM receipts WHERE id=1')
[[ $amount == 200000.50 ]] || fail 'Money precision lost on restore'
if bash "$root/ops/scripts/restore.sh" >/dev/null 2>&1; then fail 'Nonempty restore incorrectly accepted'; fi
DB_NAME=golden_leaf_source RESTORE_CONFIRM=golden_leaf_source
if bash "$root/ops/scripts/restore.sh" >/dev/null 2>&1; then fail 'Live schema name incorrectly accepted'; fi
uploads_backup=$(find "$fixture" -maxdepth 1 -name '*.uploads.tar.gz.age' -print)
content=$(docker run --rm -i --network none --mount "type=bind,source=$fixture_mount,target=/fixture,readonly" \
  golden-leaf-backup-crypto:local -d -i /fixture/identity < "$uploads_backup" | \
  docker run --rm -i --network none --entrypoint tar golden-leaf-backup-crypto:local -xzOf - ./menu-fixture.txt)
[[ $content == 'fixture image content' ]] || fail 'Uploads archive round trip failed'
printf 'Backup/restore drill passed: DB + uploads encryption, exact DECIMAL, tampering/wrong-key/nonempty/live-schema refused.\n'
