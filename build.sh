#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

command -v gradle >/dev/null 2>&1 || {
  echo "Gradle 9.4.1 is required. CI installs the pinned version automatically." >&2
  exit 1
}

gradle --no-daemon clean lintDebug assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/Sigilbound-Arcane-Duel-debug.apk
sha256sum dist/Sigilbound-Arcane-Duel-debug.apk > dist/Sigilbound-Arcane-Duel-debug.sha256
printf 'Built %s\n' "$PROJECT_DIR/dist/Sigilbound-Arcane-Duel-debug.apk"
