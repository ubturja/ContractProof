#!/usr/bin/env bash
# Renders real Compose marketing screenshots and copies them into docs/assets/screenshots/.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${ROOT}/docs/assets/screenshots"
PKG="com.contractproof.app"
REMOTE="/sdcard/Android/data/${PKG}/files/marketing-screenshots"

cd "$ROOT"
./scripts/run-connected-android-tests.sh \
  -Pandroid.testInstrumentationRunnerArguments.class=com.contractproof.app.MarketingScreenshotsTest

mkdir -p "$OUT"
for f in 01-dashboard 02-contract-extraction 03-cleaner-service 04-evidence-coverage \
  05-dispute-reconstruction 06-evidence-report 07-paywall; do
  adb pull "${REMOTE}/${f}.png" "${OUT}/${f}.png"
done

for f in "${OUT}"/0*.png; do
  if command -v magick >/dev/null 2>&1; then
    magick "$f" -resize 1080x1920! "$f"
  elif command -v convert >/dev/null 2>&1; then
    convert "$f" -resize 1080x1920! "$f"
  fi
done

echo "Updated marketing screenshots in ${OUT}"
