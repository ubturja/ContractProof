#!/usr/bin/env bash
# Print and write release metadata for the universal APK in dist/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APK=$(find "$ROOT/dist" -maxdepth 1 -name 'ContractProof-*-universal.apk' 2>/dev/null | head -1)
if [[ -z "$APK" || ! -f "$APK" ]]; then
  echo "No APK in dist/. Run ./scripts/assemble-release-apk.sh first." >&2
  exit 1
fi

FILENAME=$(basename "$APK")
SIZE_BYTES=$(stat -c%s "$APK" 2>/dev/null || stat -f%z "$APK")
SHA256=$(sha256sum "$APK" | awk '{print $1}')

VERSION_NAME="unknown"
VERSION_CODE="unknown"
if command -v aapt >/dev/null 2>&1; then
  BADGING=$(aapt dump badging "$APK" 2>/dev/null || true)
  VERSION_NAME=$(echo "$BADGING" | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -1)
  VERSION_CODE=$(echo "$BADGING" | sed -n "s/.*versionCode='\([^']*\)'.*/\1/p" | head -1)
fi
if [[ "$VERSION_NAME" == "unknown" ]]; then
  VERSION_NAME=$(echo "$FILENAME" | sed -E 's/ContractProof-([^-]+)-.*/\1/')
  VERSION_CODE=$(echo "$FILENAME" | sed -E 's/ContractProof-[^-]+-([0-9]+)-.*/\1/')
fi

OUT="$ROOT/docs/release/RELEASE_METADATA.md"
mkdir -p "$(dirname "$OUT")"
cat >"$OUT" <<EOF
# Release metadata (generated)

Do not commit APK binaries. Regenerate with \`./scripts/release-metadata.sh\` after each build.

| Field | Value |
|-------|--------|
| Filename | \`$FILENAME\` |
| Version name | $VERSION_NAME |
| Version code | $VERSION_CODE |
| File size (bytes) | $SIZE_BYTES |
| SHA-256 | \`$SHA256\` |

## Verify checksum

\`\`\`bash
sha256sum dist/$FILENAME
# expected: $SHA256
\`\`\`

See [apk-verification.md](apk-verification.md) for judge-friendly steps.
EOF

echo "Wrote $OUT"
cat "$OUT"
