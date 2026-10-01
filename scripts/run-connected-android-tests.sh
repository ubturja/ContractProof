#!/usr/bin/env bash
# Run Android instrumented tests. Starts the ContractProof_API34 emulator if none is connected.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK=$(grep '^sdk.dir=' "$ROOT/local.properties" | cut -d= -f2-)
if [[ -z "$SDK" || ! -d "$SDK" ]]; then
  echo "Set sdk.dir in local.properties to your Android SDK." >&2
  exit 1
fi
export ANDROID_SDK_ROOT="$SDK" ANDROID_HOME="$SDK"
export PATH="$SDK/platform-tools:$SDK/emulator:$SDK/cmdline-tools/latest/bin:$PATH"

if ! adb devices | awk 'NR>1 && $2=="device"{found=1} END{exit !found}'; then
  if ! avdmanager list avd 2>/dev/null | grep -q 'ContractProof_API34'; then
    echo "Creating AVD ContractProof_API34 ..."
    echo no | avdmanager create avd -n ContractProof_API34 \
      -k "system-images;android-34;google_apis;x86_64" --device pixel_6
  fi
  echo "Starting emulator ContractProof_API34 ..."
  emulator -avd ContractProof_API34 -no-window -gpu swiftshader_indirect \
    -no-audio -no-boot-anim -no-snapshot-save >/tmp/contractproof-emulator.log 2>&1 &
  adb wait-for-device
  for _ in $(seq 1 90); do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      break
    fi
    sleep 2
  done
fi

cd "$ROOT"
exec ./gradlew :androidApp:connectedDebugAndroidTest "$@"
