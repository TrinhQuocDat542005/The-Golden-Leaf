#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ "$(date -u +%Y%m%d%H%M%S)" > '__SECURITY_REVIEW_DEADLINE__' || "$(date -u +%Y%m%d%H%M%S)" == '__SECURITY_REVIEW_DEADLINE__' ]]; then
  echo 'Security exception expired. Obtain a newly reviewed release.' >&2
  exit 1
fi
if command -v sha256sum >/dev/null; then sha256sum -c SHA256SUMS;
elif command -v shasum >/dev/null; then shasum -a 256 -c SHA256SUMS;
else echo 'Install a SHA-256 checksum tool first.' >&2; exit 1; fi
command -v java >/dev/null || { echo 'Install JDK 17 first.' >&2; exit 1; }
echo 'Local portfolio only: http://127.0.0.1:8080/demo.html — do not transfer money. Ctrl+C stops the demo.'
exec java -jar golden-leaf-demo.jar --spring.profiles.active=demo --server.port=8080
