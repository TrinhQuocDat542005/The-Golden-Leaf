#!/usr/bin/env bash
# Production-shaped local test only: Firebase disabled, fake monitoring credential, localhost ports.
set -euo pipefail
# shellcheck source=ops/scripts/common.sh
source "$(dirname "$0")/common.sh"
root=$(cd "$(dirname "$0")/../.." && pwd)
fixture=$(mktemp -d)
fixture_mount=$(docker_path "$fixture")
sink=$(curl_sink)
suffix="$(date +%s)-$RANDOM"
network="golden-leaf-container-$suffix"
db="golden-leaf-container-db-$suffix"
backend="golden-leaf-container-api-$suffix"
proxy="golden-leaf-container-proxy-$suffix"
volume="golden-leaf-container-uploads-$suffix"
cleanup() {
  docker stop "$proxy" "$backend" "$db" >/dev/null 2>&1 || true
  docker network rm "$network" >/dev/null 2>&1 || true
  docker volume rm "$volume" >/dev/null 2>&1 || true
  rm -f -- "$fixture/privkey.pem" "$fixture/fullchain.pem"
  rmdir "$fixture" 2>/dev/null || true
}
trap cleanup EXIT
openssl req -x509 -newkey rsa:2048 -nodes -keyout "$fixture_mount/privkey.pem" -out "$fixture_mount/fullchain.pem" \
  -days 1 -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' >/dev/null 2>&1
docker network create --label golden-leaf.purpose=container-drill "$network" >/dev/null
docker volume create --label golden-leaf.purpose=container-drill "$volume" >/dev/null
docker run -d --rm --name "$db" --network "$network" --network-alias db --label golden-leaf.purpose=container-drill \
  --tmpfs /var/lib/mysql -e MYSQL_ROOT_PASSWORD=container-test-only-password -e MYSQL_DATABASE=golden_leaf_container_test mysql:8.0 >/dev/null
ready=false
for _ in $(seq 1 90); do
  if docker exec "$db" mysqladmin ping --silent >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
[[ $ready == true ]] || fail 'Disposable DB not ready'
docker run -d --rm --name "$backend" --network "$network" --network-alias backend --label golden-leaf.purpose=container-drill \
  --read-only --cap-drop ALL --security-opt no-new-privileges:true --tmpfs /tmp:rw,noexec,nosuid,size=128m \
  --mount "type=volume,source=$volume,target=/app/uploads" \
  -p 127.0.0.1:18080:8080 -p 127.0.0.1:18081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod -e DB_URL='jdbc:mysql://db:3306/golden_leaf_container_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  -e DB_USERNAME=root -e DB_PASSWORD=container-test-only-password -e FIREBASE_CREDENTIALS_PATH=/unused \
  -e MONITORING_TOKEN=container-test-monitoring-token-32-characters \
  "${BACKEND_IMAGE:-golden-leaf:ci}" --app.firebase.enabled=false >/dev/null
ready=false
for _ in $(seq 1 90); do
  if curl -q --fail --silent --max-time 2 http://127.0.0.1:18081/actuator/health/readiness >/dev/null; then ready=true; break; fi
  sleep 1
done
if [[ $ready != true ]]; then docker logs --tail 10 "$backend"; fail 'Backend not ready'; fi
[[ $(docker exec "$backend" id -u) != 0 ]] || fail 'Backend ran as root'
docker exec "$backend" sh -c 'test -w /app/uploads && test ! -w /app/app.jar'
status=$(curl -q --silent --max-time 10 -o "$sink" -w '%{http_code}' http://127.0.0.1:18081/actuator/prometheus)
[[ $status == 401 ]] || fail 'Anonymous metrics not blocked'
metrics=$(curl -q --fail --silent --max-time 10 -H 'Authorization: Bearer container-test-monitoring-token-32-characters' \
  http://127.0.0.1:18081/actuator/prometheus)
[[ $metrics == *goldenleaf_operations_snapshot_age_seconds* ]] || fail 'Business metric missing'
BASE_URL=http://127.0.0.1:18080/ REQUESTS=100 CONCURRENCY=5 node "$(docker_path "$root/ops/scripts/load-smoke.mjs")"
nginx_config=$(docker_path "$root/ops/nginx/nginx.conf")
proxy_config=$(docker_path "$root/ops/nginx/proxy_params_goldenleaf")
docker run -d --rm --name "$proxy" --network "$network" --label golden-leaf.purpose=container-drill \
  --read-only --tmpfs /var/cache/nginx --tmpfs /var/run --tmpfs /tmp -p 127.0.0.1:18443:443 \
  --mount "type=bind,source=$nginx_config,target=/etc/nginx/nginx.conf,readonly" \
  --mount "type=bind,source=$proxy_config,target=/etc/nginx/proxy_params_goldenleaf,readonly" \
  --mount "type=bind,source=$fixture_mount,target=/etc/nginx/tls,readonly" nginx:1.28-alpine >/dev/null
# Trust only this generated fixture cert; never disable TLS verification.
curl -q --fail --silent --show-error --retry 5 --retry-connrefused --retry-delay 1 --cacert "$fixture_mount/fullchain.pem" \
  https://localhost:18443/api/thucdon >/dev/null
status=$(curl -q --silent --cacert "$fixture_mount/fullchain.pem" -o "$sink" -w '%{http_code}' https://localhost:18443/actuator/prometheus)
[[ $status == 404 ]] || fail 'Proxy exposed management route'
status=$(curl -q --silent --cacert "$fixture_mount/fullchain.pem" -o "$sink" -w '%{http_code}' https://localhost:18443/api/staff/bookings)
[[ $status == 401 ]] || fail 'Proxy bypassed staff authentication'
headers=$(curl -q --silent --cacert "$fixture_mount/fullchain.pem" -D - -o "$sink" https://localhost:18443/staff.html)
[[ $headers == *Strict-Transport-Security* ]] || fail 'HSTS header missing'
printf 'Container drill passed: non-root/read-only, uploads writable, private metrics, bounded load, verified TLS proxy and auth.\n'
