#!/usr/bin/env bash
#
# Builds the `SharedRecipes` XCFrameworks and renders a ready-to-publish
# `Package.swift` that points at the matching GitHub release asset.
#
# Usage:
#   scripts/build-xcframework.sh <version> [owner/repo]
#
# Example:
#   scripts/build-xcframework.sh 1.0.0 zvonimir/MyApplication
#
# Environment:
#   DEVELOPER_DIR       must point at a full Xcode installation
#                       (defaults to /Applications/Xcode.app/Contents/Developer)
#   KONAN_DATA_DIR      Kotlin/Native data directory (defaults to ~/.konan).
#                       Forwarded to Gradle as -Pkonan.data.dir.
#   GRADLE_USER_HOME    Gradle home (defaults to the standard location)
#
# Output (in build/spm):
#   SharedRecipes.xcframework        the framework bundle
#   SharedRecipes.xcframework.zip    the release asset
#   SharedRecipes.xcframework.zip.sha256
#   Package.swift                    rendered from spm/Package.swift.template
#
set -euo pipefail

VERSION="${1:-}"
REPO_SLUG="${2:-${GITHUB_REPOSITORY:-OWNER/REPO}}"

if [[ -z "$VERSION" ]]; then
  echo "error: version argument is required, e.g. scripts/build-xcframework.sh 1.0.0 owner/repo" >&2
  exit 1
fi

VERSION="${VERSION#v}"
OWNER="${REPO_SLUG%%/*}"
REPO="${REPO_SLUG##*/}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if [[ -z "${DEVELOPER_DIR:-}" && -d /Applications/Xcode.app/Contents/Developer ]]; then
  export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
fi
echo "==> DEVELOPER_DIR=${DEVELOPER_DIR:-<unset>}"
echo "==> version=$VERSION owner=$OWNER repo=$REPO"

FRAMEWORK_NAME="SharedRecipes"
OUT_DIR="$REPO_ROOT/build/spm"
ZIP_PATH="$OUT_DIR/$FRAMEWORK_NAME.xcframework.zip"

GRADLE_ARGS=(--console=plain)
if [[ -n "${KONAN_DATA_DIR:-}" ]]; then
  GRADLE_ARGS+=("-Pkonan.data.dir=$KONAN_DATA_DIR")
fi

echo "==> Linking release frameworks"
./gradlew :recipes:linkReleaseFrameworkIosArm64 \
          :recipes:linkReleaseFrameworkIosSimulatorArm64 \
          "${GRADLE_ARGS[@]}"

echo "==> Assembling XCFramework"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

xcodebuild -create-xcframework \
  -framework "recipes/build/bin/iosArm64/releaseFramework/$FRAMEWORK_NAME.framework" \
  -framework "recipes/build/bin/iosSimulatorArm64/releaseFramework/$FRAMEWORK_NAME.framework" \
  -output "$OUT_DIR/$FRAMEWORK_NAME.xcframework"

echo "==> Zipping"
# ditto keeps the symlinks and permissions an XCFramework needs.
ditto -c -k --sequesterRsrc --keepParent \
  "$OUT_DIR/$FRAMEWORK_NAME.xcframework" \
  "$ZIP_PATH"

CHECKSUM="$(swift package compute-checksum "$ZIP_PATH")"
printf '%s' "$CHECKSUM" > "$ZIP_PATH.sha256"
echo "==> checksum=$CHECKSUM"

echo "==> Rendering Package.swift"
sed \
  -e "s/__VERSION__/$VERSION/g" \
  -e "s/__OWNER__/$OWNER/g" \
  -e "s/__REPO__/$REPO/g" \
  -e "s/__CHECKSUM__/$CHECKSUM/g" \
  "$REPO_ROOT/spm/Package.swift.template" > "$OUT_DIR/Package.swift"

echo "==> Done"
echo "    asset:   $ZIP_PATH"
echo "    package: $OUT_DIR/Package.swift"
