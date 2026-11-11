#!/usr/bin/env bash
# 校验 release.jks 与 keystore.properties 中的密码是否一致
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
PROPS_FILE="$ANDROID_DIR/keystore.properties"

read_prop() {
  local key="$1"
  grep -E "^${key}=" "$PROPS_FILE" | tail -1 | cut -d= -f2- | tr -d '\r'
}

is_keytool() {
  local candidate="$1"
  [[ -n "$candidate" ]] && { [[ -x "$candidate" ]] || [[ -f "$candidate" ]]; }
}

find_keytool() {
  local candidate=""
  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidate="$JAVA_HOME/bin/keytool.exe"
    is_keytool "$candidate" && { echo "$candidate"; return 0; }
  fi
  if command -v keytool >/dev/null 2>&1; then
    echo "keytool"
    return 0
  fi
  candidate="/c/Program Files/Java/jdk-17/bin/keytool.exe"
  is_keytool "$candidate" && { echo "$candidate"; return 0; }
  return 1
}

if [[ ! -f "$PROPS_FILE" ]]; then
  echo "错误：未找到 $PROPS_FILE"
  exit 1
fi

KEYTOOL="$(find_keytool || true)"
if [[ -z "$KEYTOOL" ]]; then
  echo "错误：未找到 keytool"
  exit 1
fi

store_file_rel="$(read_prop storeFile)"
store_password="$(read_prop storePassword)"
key_alias="$(read_prop keyAlias)"
key_password="$(read_prop keyPassword)"
store_file="$ANDROID_DIR/$store_file_rel"

if [[ -z "$store_file_rel" || -z "$store_password" || -z "$key_alias" || -z "$key_password" ]]; then
  echo "错误：keystore.properties 缺少 storeFile/storePassword/keyAlias/keyPassword"
  exit 1
fi

if [[ ! -f "$store_file" ]]; then
  echo "错误：密钥库不存在: $store_file"
  exit 1
fi

if ! "$KEYTOOL" -list -keystore "$store_file" -storepass "$store_password" >/dev/null 2>&1; then
  echo "错误：storePassword 与密钥库不匹配。"
  echo "请编辑 $PROPS_FILE 填写正确密码，或删除密钥库后重新运行:"
  echo "  rm -f \"$store_file\" \"$PROPS_FILE\""
  echo "  ./scripts/setup-release-signing.sh"
  exit 1
fi

if ! "$KEYTOOL" -list -keystore "$store_file" -alias "$key_alias" -storepass "$store_password" -keypass "$key_password" >/dev/null 2>&1; then
  echo "错误：keyAlias 或 keyPassword 不正确。"
  exit 1
fi

echo "签名配置校验通过: $store_file (alias=$key_alias)"
