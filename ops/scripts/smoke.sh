#!/usr/bin/env bash
set -euo pipefail
# shellcheck source=ops/scripts/common.sh
source "$(dirname "$0")/common.sh"
sink=$(curl_sink)
: "${BASE_URL:?Set an HTTPS staging or production API URL}"
[[ $BASE_URL == https://* ]] || { echo 'HTTPS required' >&2; exit 1; }
[[ $BASE_URL != *'?'* && $BASE_URL != *'#'* && $BASE_URL != *'@'* ]] || exit 1
BASE_URL=${BASE_URL%/}
curl -q --fail --silent --show-error --max-time 15 "$BASE_URL/api/thucdon" >/dev/null
status=$(curl -q --silent --show-error --max-time 15 -o "$sink" -w '%{http_code}' "$BASE_URL/api/staff/bookings")
[[ $status == 401 ]] || { echo "Private route unexpectedly returned $status" >&2; exit 1; }
status=$(curl -q --silent --show-error --max-time 15 -o "$sink" -w '%{http_code}' "$BASE_URL/actuator/prometheus")
[[ $status == 404 ]] || { echo 'Public metrics route is not blocked' >&2; exit 1; }
echo 'Public menu reachable; staff authentication enforced; public metrics blocked.'
