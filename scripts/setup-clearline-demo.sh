#!/usr/bin/env bash
# One-time ClearLine demo bootstrap for hosted Supabase (project ref wsblrmpzkfqjugeweqly).
# Secrets: copy setup.env.example → setup.env (gitignored), fill values, then run this script.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -f "$ROOT/setup.env" ]]; then
  set -a
  # shellcheck source=/dev/null
  source "$ROOT/setup.env"
  set +a
fi

PROJECT_REF="${SUPABASE_PROJECT_REF:-wsblrmpzkfqjugeweqly}"
DEMO_PASSWORD="${CLEARLINE_DEMO_PASSWORD:-ClearLine-Demo-2026!}"

if [[ -z "${SUPABASE_SERVICE_ROLE_KEY:-}" ]]; then
  echo "ERROR: Set SUPABASE_SERVICE_ROLE_KEY (Project Settings → API → service_role) in setup.env or the environment."
  exit 1
fi

if [[ -f "$ROOT/local.properties" ]]; then
  SUPABASE_URL="$(grep -E '^SUPABASE_URL=' "$ROOT/local.properties" | cut -d= -f2- || true)"
else
  SUPABASE_URL="${SUPABASE_URL:-https://${PROJECT_REF}.supabase.co}"
fi

if [[ -z "$SUPABASE_URL" || "$SUPABASE_URL" != https://* ]]; then
  echo "ERROR: SUPABASE_URL must be https://… (set in local.properties or setup.env)."
  exit 1
fi

create_auth_user() {
  local id="$1" email="$2"
  local http
  http="$(curl -s -o /tmp/supabase_auth_resp.json -w '%{http_code}' -X POST \
    "${SUPABASE_URL}/auth/v1/admin/users" \
    -H "apikey: ${SUPABASE_SERVICE_ROLE_KEY}" \
    -H "Authorization: Bearer ${SUPABASE_SERVICE_ROLE_KEY}" \
    -H "Content-Type: application/json" \
    -d "{\"id\":\"${id}\",\"email\":\"${email}\",\"password\":\"${DEMO_PASSWORD}\",\"email_confirm\":true}")"
  if [[ "$http" == "200" || "$http" == "201" ]]; then
    echo "  OK ${email}"
    return 0
  fi
  if grep -qE 'already been registered|duplicate' /tmp/supabase_auth_resp.json 2>/dev/null; then
    echo "  skip (exists) ${email}"
    return 0
  fi
  echo "  FAIL ${email} HTTP ${http}: $(cat /tmp/supabase_auth_resp.json)"
  return 1
}

upload_object() {
  local bucket="$1" local_path="$2" remote_path="$3"
  local content_type="${4:-application/octet-stream}"
  echo "  → ${bucket}:${remote_path}"
  local http
  http="$(curl -s -o /tmp/supabase_storage_resp.json -w '%{http_code}' -X POST \
    "${SUPABASE_URL}/storage/v1/object/${bucket}/${remote_path}" \
    -H "apikey: ${SUPABASE_SERVICE_ROLE_KEY}" \
    -H "Authorization: Bearer ${SUPABASE_SERVICE_ROLE_KEY}" \
    -H "Content-Type: ${content_type}" \
    -H "x-upsert: true" \
    --data-binary "@${local_path}")"
  if [[ "$http" == "200" || "$http" == "201" ]]; then
    return 0
  fi
  echo "  FAIL HTTP ${http}: $(cat /tmp/supabase_storage_resp.json)"
  return 1
}

upload_demo_assets() {
  local assets="$ROOT/supabase/seed/assets"
  local ORG="11111111-1111-4111-8111-111111111101"
  upload_object contracts "$assets/meridian-contract.pdf" \
    "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd01/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee01.pdf" \
    application/pdf
  upload_object contracts "$assets/northstar-contract.pdf" \
    "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd02/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee02.pdf" \
    application/pdf
  upload_object contracts "$assets/westbridge-contract.pdf" \
    "$ORG/dddddddd-dddd-4ddd-8ddd-dddddddddd03/eeeeeeee-eeee-4eee-8eee-eeeeeeeeee03.pdf" \
    application/pdf
  upload_object evidence "$assets/northstar-dock-evidence.jpg" \
    "$ORG/99999999-9999-4999-8999-999999999902/88888888-8888-4888-8888-888888888803/77777777-7777-4777-8777-777777777701" \
    image/jpeg
  upload_object reports "$assets/evidence-report.pdf" \
    "$ORG/44444444-4444-4444-8444-444444444401/evidence-report.pdf" \
    application/pdf
}

db_url_encoded() {
  local encoded
  encoded="$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1], safe=''))" "$SUPABASE_DB_PASSWORD")"
  echo "postgresql://postgres:${encoded}@db.${PROJECT_REF}.supabase.co:5432/postgres"
}

reset_database() {
  if [[ -n "${SUPABASE_DB_PASSWORD:-}" ]]; then
    local db_url
    db_url="$(db_url_encoded)"
    echo "→ supabase db reset --db-url --no-seed (destructive, direct Postgres)"
    supabase db reset --db-url "$db_url" --no-seed --yes
    return 0
  fi

  if [[ -n "${SUPABASE_ACCESS_TOKEN:-}" ]]; then
    export SUPABASE_ACCESS_TOKEN
    echo "→ supabase login (token)"
    supabase login --token "$SUPABASE_ACCESS_TOKEN" --name contractproof-setup
    LINK_ARGS=(link --project-ref "$PROJECT_REF" --yes)
    echo "→ supabase link"
    supabase "${LINK_ARGS[@]}"
    echo "→ supabase db reset --linked --no-seed (destructive)"
    supabase db reset --linked --no-seed --yes
    return 0
  fi

  echo "ERROR: Set SUPABASE_DB_PASSWORD or SUPABASE_ACCESS_TOKEN in setup.env."
  exit 1
}

apply_seed() {
  echo "→ apply supabase/seed.sql"
  local venv="${ROOT}/.tools/seed-venv"
  if [[ ! -x "${venv}/bin/python" ]]; then
    python3 -m venv "$venv"
    "${venv}/bin/pip" install -q psycopg2-binary
  fi
  SUPABASE_DB_PASSWORD="${SUPABASE_DB_PASSWORD:-}" SUPABASE_PROJECT_REF="${PROJECT_REF}" \
    "${venv}/bin/python" - "$ROOT/supabase/seed.sql" <<'PY'
import os
import sys
from pathlib import Path

import psycopg2

root = Path(sys.argv[1])
password = os.environ["SUPABASE_DB_PASSWORD"]
ref = os.environ["SUPABASE_PROJECT_REF"]
sql = root.read_text()
conn = psycopg2.connect(
    host=f"db.{ref}.supabase.co",
    port=5432,
    user="postgres",
    password=password,
    dbname="postgres",
)
conn.autocommit = True
with conn.cursor() as cur:
    cur.execute(sql)
conn.close()
print("  seed OK")
PY
}

reset_database

echo "→ create demo Auth users (fixed UUIDs)"
create_auth_user "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa01" "owner@clearline.demo"
create_auth_user "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa02" "manager@clearline.demo"
create_auth_user "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa03" "cleaner@clearline.demo"
create_auth_user "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaa04" "client@clearline.demo"

apply_seed

echo "→ upload demo storage assets"
upload_demo_assets

if [[ -n "${REVENUECAT_API_KEY:-}" ]]; then
  echo "→ merge REVENUECAT_* into local.properties"
  export REVENUECAT_API_KEY REVENUECAT_ENTITLEMENT_PRO REVENUECAT_ENTITLEMENT_BUSINESS
  REVENUECAT_ENTITLEMENT_PRO="${REVENUECAT_ENTITLEMENT_PRO:-pro}"
  REVENUECAT_ENTITLEMENT_BUSINESS="${REVENUECAT_ENTITLEMENT_BUSINESS:-business}"
  python3 - "$ROOT/local.properties" <<'PY'
import os
import sys
from pathlib import Path

path = Path(sys.argv[1])
lines = path.read_text().splitlines() if path.exists() else []
keys = {
    "REVENUECAT_API_KEY": os.environ.get("REVENUECAT_API_KEY", ""),
    "REVENUECAT_ENTITLEMENT_PRO": os.environ.get("REVENUECAT_ENTITLEMENT_PRO", "pro"),
    "REVENUECAT_ENTITLEMENT_BUSINESS": os.environ.get("REVENUECAT_ENTITLEMENT_BUSINESS", "business"),
}
out = []
seen = set()
for line in lines:
    key = line.split("=", 1)[0] if "=" in line else None
    if key in keys:
        out.append(f"{key}={keys[key]}")
        seen.add(key)
    else:
        out.append(line)
for key, val in keys.items():
    if key not in seen:
        out.append(f"{key}={val}")
path.write_text("\n".join(out) + "\n")
PY
fi

echo "Done. Run: ./gradlew :androidApp:assembleDebug"
