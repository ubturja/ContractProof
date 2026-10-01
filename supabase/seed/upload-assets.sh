#!/usr/bin/env bash
# Upload ClearLine demo storage objects after `supabase db reset`.
# Requires Supabase CLI logged in and project linked.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ASSETS="$ROOT/supabase/seed/assets"
ORG="11111111-1111-4111-8111-111111111101"

upload() {
  local bucket="$1"
  local local_path="$2"
  local remote_path="$3"
  echo "→ $bucket:$remote_path"
  supabase storage cp "$local_path" "$remote_path" --bucket "$bucket"
}

upload contracts "$ASSETS/meridian-contract.pdf" \
  "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd01/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01/contract.pdf"
upload contracts "$ASSETS/northstar-contract.pdf" \
  "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd02/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02/contract.pdf"
upload contracts "$ASSETS/westbridge-contract.pdf" \
  "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd03/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03/contract.pdf"

upload evidence "$ASSETS/northstar-dock-evidence.jpg" \
  "$ORG/99999999-9999-4999-8999-999999999902/88888888-8888-4888-8888-888888888803/77777777-7777-4777-8777-777777777701"

upload reports "$ASSETS/evidence-report.pdf" \
  "$ORG/44444444-4444-4444-8444-444444444401/evidence-report.pdf"

echo "Done."
