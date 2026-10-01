#!/usr/bin/env bash
# Pre-recording checks: checklist reminders + host unit tests (no Supabase required).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== ContractProof demo recording verification ==="
echo ""
echo "Manual steps (required before recording):"
echo "  1. supabase db reset"
echo "  2. Create Auth users per docs/development/demo-data.md"
echo "  3. ./supabase/seed/upload-assets.sh"
echo "  4. Dry-run docs/qa/hackathon-demo-video-script.md on device/emulator"
echo ""
echo "Golden-path bullets:"
echo "  - Meridian extraction: two requirements (version eeee...ee01)"
echo "  - Cleaner Today: Meridian job after owner approve (visit weekday 4)"
echo "  - Dispute 44444444-4444-4444-8444-444444444401 timeline + report PDF"
echo ""

if [[ -f "$ROOT/local.properties" ]]; then
  if grep -q 'DEMO_BYPASS_SUBSCRIPTION=true' "$ROOT/local.properties" 2>/dev/null; then
    echo "NOTE: DEMO_BYPASS_SUBSCRIPTION=true in local.properties (recording-only)."
  fi
else
  echo "WARN: local.properties missing — configure Supabase before building demo APK."
fi

if [[ -n "${JAVA_HOME:-}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
  :
elif [[ -d "$ROOT/.jdk17/bin" ]]; then
  export JAVA_HOME="$ROOT/.jdk17"
fi

echo "Running :composeApp:testAndroidHostTest ..."
./gradlew :composeApp:testAndroidHostTest -q
echo ""
echo "Host unit tests: PASS"
echo "Complete manual checklist above before recording."
