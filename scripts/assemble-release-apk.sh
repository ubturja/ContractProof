#!/usr/bin/env bash
# Build a universal release APK (all ABIs in one package). Output: dist/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ensure_jdk17() {
  if [[ -n "${JAVA_HOME:-}" ]] && [[ -x "${JAVA_HOME}/bin/javac" ]]; then
    return 0
  fi
  for candidate in \
    /usr/lib/jvm/java-17-openjdk-amd64 \
    /usr/lib/jvm/java-17-openjdk \
    /usr/lib/jvm/java-17-amazon-corretto \
    "$ROOT/.jdk17"; do
    if [[ -d "$candidate" && -x "$candidate/bin/javac" ]]; then
      export JAVA_HOME="$candidate"
      return 0
    fi
  done
  local jdk_dir="$ROOT/.jdk17"
  local archive="$ROOT/.jdk17.tar.gz"
  local url="https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jdk_x64_linux_hotspot_17.0.13_11.tar.gz"
  echo "Downloading Temurin JDK 17 to $jdk_dir ..."
  mkdir -p "$jdk_dir"
  curl -fsSL "$url" -o "$archive"
  tar -xzf "$archive" -C "$jdk_dir" --strip-components=1
  rm -f "$archive"
  export JAVA_HOME="$jdk_dir"
}

ensure_jdk17
echo "Using JAVA_HOME=$JAVA_HOME"

if [[ ! -f "$ROOT/keystore.properties" ]] && [[ "${CONTRACTPROOF_USE_DEBUG_SIGNING:-}" != "true" ]]; then
  echo "No keystore.properties found. For a local installable APK, run:" >&2
  echo "  CONTRACTPROOF_USE_DEBUG_SIGNING=true $0" >&2
  echo "For judge distribution, copy keystore.properties.example → keystore.properties" >&2
  exit 1
fi

export CONTRACTPROOF_USE_DEBUG_SIGNING="${CONTRACTPROOF_USE_DEBUG_SIGNING:-true}"

./gradlew clean :androidApp:assembleRelease --no-daemon

VERSION_NAME=$(grep 'versionName' androidApp/build.gradle.kts | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
VERSION_CODE=$(grep 'versionCode' androidApp/build.gradle.kts | head -1 | sed -E 's/.*= ([0-9]+).*/\1/')
APK_SRC="$ROOT/androidApp/build/outputs/apk/release/androidApp-release.apk"
if [[ ! -f "$APK_SRC" ]]; then
  APK_SRC=$(find "$ROOT/androidApp/build/outputs/apk/release" -name '*.apk' | head -1)
fi
if [[ ! -f "$APK_SRC" ]]; then
  echo "Release APK not found under androidApp/build/outputs/apk/release" >&2
  exit 1
fi

mkdir -p "$ROOT/dist"
OUT="$ROOT/dist/ContractProof-${VERSION_NAME}-${VERSION_CODE}-universal.apk"
cp "$APK_SRC" "$OUT"
echo "Built: $OUT"
ls -la "$OUT"
