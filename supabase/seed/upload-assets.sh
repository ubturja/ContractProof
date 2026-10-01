#!/usr/bin/env bash
# Upload ClearLine demo storage objects after `supabase db reset`.
# Uses current CLI syntax: supabase storage cp <file> ss:///<bucket>/<object>
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ASSETS="$ROOT/supabase/seed/assets"
ORG="11111111-1111-4111-8111-111111111101"

storage_target=()
if supabase status >/dev/null 2>&1; then
  storage_target=(--local)
elif supabase projects list >/dev/null 2>&1 && [[ -f "$ROOT/supabase/.temp/project-ref" ]]; then
  storage_target=(--linked)
else
  echo "Supabase is not running locally and no project is linked." >&2
  echo "Start local stack:  supabase start && supabase db reset" >&2
  echo "Or link hosted:     supabase link --project-ref <ref> && supabase db reset --linked" >&2
  echo "Then re-run:        $0" >&2
  exit 1
fi

upload() {
  local bucket="$1"
  local local_path="$2"
  local remote_path="$3"
  echo "→ ${bucket}:${remote_path}"
  supabase storage cp "${storage_target[@]}" "$local_path" "ss:///${bucket}/${remote_path}"
}

upload contracts "$ASSETS/meridian-contract.pdf" \
  "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd01/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01.pdf"
upload contracts "$ASSETS/northstar-contract.pdf" \
  "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd02/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02.pdf"
upload contracts "$ASSETS/westbridge-contract.pdf" \
  "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd03/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03.pdf"

upload evidence "$ASSETS/northstar-dock-evidence.jpg" \
  "$ORG/99999999-9999-4999-8999-999999999902/88888888-8888-4888-8888-888888888803/77777777-7777-4777-8777-777777777701"

upload reports "$ASSETS/evidence-report.pdf" \
  "$ORG/44444444-4444-4444-8444-444444444401/evidence-report.pdf"

echo "Done."
