#!/usr/bin/env bash
# Generates docs/design/icon-1024.png and legacy mipmap PNGs from ContractProof brand colors.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
python3 "$ROOT/scripts/generate_icon_raster.py"
