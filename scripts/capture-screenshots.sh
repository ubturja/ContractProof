#!/usr/bin/env bash
# Capture hackathon screenshots from a running emulator/device (1080x1920).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADB="${ROOT}/.tools/platform-tools/adb"
OUT="${ROOT}/docs/assets/screenshots"
mkdir -p "$OUT"

if [[ ! -x "$ADB" ]]; then
  echo "Install platform-tools: wget + unzip to ${ROOT}/.tools (see docs/qa/screenshots.md)"
  exit 1
fi

if ! "$ADB" get-state 2>/dev/null | grep -q device; then
  echo "No Android device/emulator connected."
  exit 1
fi

capture() {
  local name="$1"
  echo "Capture: $name — navigate in app, then press Enter"
  read -r _
  "$ADB" exec-out screencap -p > "${OUT}/${name}.png"
  w=$(identify -format '%w' "${OUT}/${name}.png" 2>/dev/null || true)
  echo "Saved ${OUT}/${name}.png (${w:-unknown}px wide)"
}

echo "Follow docs/qa/screenshots.md golden-path steps before each capture."
capture "01-dashboard"
capture "02-contract-extraction"
capture "03-cleaner-service"
capture "04-evidence-coverage"
capture "05-dispute-reconstruction"
capture "06-evidence-report"
capture "07-paywall"
