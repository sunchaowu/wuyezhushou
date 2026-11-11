#!/usr/bin/env bash
# 发布前递增应用版本：versionPatch +1、versionCode +1
# 用法:
#   ./scripts/bump-app-version.sh          # 递增并写回 version.properties
#   ./scripts/bump-app-version.sh --show   # 仅显示当前版本，不递增
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
VERSION_FILE="$ROOT_DIR/android/version.properties"
APP_INTRO_FILE="$ROOT_DIR/android/app/src/main/res/raw/app_intro.txt"

read_prop() {
  local key="$1"
  local default="${2:-0}"
  if [[ -f "$VERSION_FILE" ]]; then
    local value
    value="$(grep -E "^${key}=" "$VERSION_FILE" | tail -1 | cut -d= -f2- | tr -d '\r')"
    if [[ -n "$value" ]]; then
      echo "$value"
      return
    fi
  fi
  echo "$default"
}

write_props() {
  local code="$1"
  local major="$2"
  local minor="$3"
  local patch="$4"
  cat >"$VERSION_FILE" <<EOF
# 应用发布版本（release 构建前自动将小版本 patch +1，versionCode +1）
versionCode=${code}
versionMajor=${major}
versionMinor=${minor}
versionPatch=${patch}
EOF
}

sync_app_intro_version() {
  local version_name="$1"
  if [[ -f "$APP_INTRO_FILE" ]]; then
    if grep -q '^版本：' "$APP_INTRO_FILE"; then
      sed -i "s/^版本：.*/版本：${version_name}/" "$APP_INTRO_FILE"
    fi
  fi
}

version_code="$(read_prop versionCode 1)"
version_major="$(read_prop versionMajor 1)"
version_minor="$(read_prop versionMinor 0)"
version_patch="$(read_prop versionPatch 0)"
version_name="${version_major}.${version_minor}.${version_patch}"

if [[ "${1:-}" == "--show" ]]; then
  echo "当前版本: ${version_name} (versionCode=${version_code})"
  exit 0
fi

next_code=$((version_code + 1))
next_patch=$((version_patch + 1))
next_name="${version_major}.${version_minor}.${next_patch}"

write_props "$next_code" "$version_major" "$version_minor" "$next_patch"
sync_app_intro_version "$next_name"

echo "版本已递增: ${version_name} (${version_code}) -> ${next_name} (${next_code})"
