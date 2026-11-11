#!/usr/bin/env bash
# 生成 Release 签名密钥库并创建 keystore.properties（密码仅保存在本机，不提交 Git）
#
# 用法:
#   ./scripts/setup-release-signing.sh
#
# 非交互（CI/自动化）可预先设置环境变量：
#   KEYSTORE_PASSWORD=xxx KEY_PASSWORD=xxx ./scripts/setup-release-signing.sh
#
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
KEYSTORE_DIR="$ANDROID_DIR/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.jks"
PROPS_FILE="$ANDROID_DIR/keystore.properties"
EXAMPLE_FILE="$ANDROID_DIR/keystore.properties.example"
KEY_ALIAS="${KEY_ALIAS:-guanjiazhushou}"
VALIDITY_DAYS="${VALIDITY_DAYS:-10000}"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
if [[ ! -d "$ANDROID_HOME" ]]; then
  export ANDROID_HOME="C:/Users/sunch/android-sdk"
fi
if [[ ! -d "$ANDROID_HOME" ]]; then
  export ANDROID_HOME="C:/Program Files (x86)/Android/android-sdk"
fi

is_keytool() {
  local candidate="$1"
  [[ -n "$candidate" ]] && { [[ -x "$candidate" ]] || [[ -f "$candidate" ]]; }
}

find_keytool() {
  local candidate=""

  if [[ -n "${JAVA_HOME:-}" ]]; then
    candidate="$JAVA_HOME/bin/keytool"
    is_keytool "$candidate" && { echo "$candidate"; return 0; }
    candidate="$JAVA_HOME/bin/keytool.exe"
    is_keytool "$candidate" && { echo "$candidate"; return 0; }
  fi

  if command -v keytool >/dev/null 2>&1; then
    echo "keytool"
    return 0
  fi

  local -a fixed_candidates=(
    "$ANDROID_HOME/jbr/bin/keytool"
    "$ANDROID_HOME/jbr/bin/keytool.exe"
    "/c/Program Files/Android/Android Studio/jbr/bin/keytool.exe"
    "/c/Program Files/Android/Android Studio/jbr/bin/keytool"
    "/c/Program Files/Java/jdk-17/bin/keytool.exe"
    "/c/Program Files/Java/jdk-21/bin/keytool.exe"
    "/c/Program Files/Java/jdk-11/bin/keytool.exe"
    "/c/Program Files/Java/jdk1.8.0_361/bin/keytool.exe"
    "/c/Program Files/Android/jdk/jdk-8.0.302.8-hotspot/jdk8u302-b08/bin/keytool.exe"
  )
  for candidate in "${fixed_candidates[@]}"; do
    if is_keytool "$candidate"; then
      echo "$candidate"
      return 0
    fi
  done

  local java_root="/c/Program Files/Java"
  if [[ -d "$java_root" ]]; then
    for candidate in "$java_root"/jdk*/bin/keytool.exe; do
      if is_keytool "$candidate"; then
        echo "$candidate"
        return 0
      fi
    done
  fi

  return 1
}

KEYTOOL="$(find_keytool || true)"
if [[ -z "$KEYTOOL" ]]; then
  echo "错误：未找到 keytool。"
  echo "请任选一种方式："
  echo "  1. 安装 JDK 17 并设置 JAVA_HOME（推荐）"
  echo "  2. 安装 Android Studio（自带 JBR 含 keytool）"
  echo "  3. 将 JDK 的 bin 目录加入 PATH"
  echo ""
  echo "Windows 常见路径示例："
  echo "  C:/Program Files/Java/jdk-17/bin/keytool.exe"
  echo "  C:/Program Files/Android/Android Studio/jbr/bin/keytool.exe"
  exit 1
fi
echo "使用 keytool: $KEYTOOL"

mkdir -p "$KEYSTORE_DIR"

if [[ -f "$KEYSTORE_FILE" ]]; then
  echo "密钥库已存在: $KEYSTORE_FILE"
  if [[ -f "$PROPS_FILE" ]]; then
    if "$ROOT_DIR/scripts/verify-release-signing.sh" >/dev/null 2>&1; then
      echo "签名配置校验通过，无需重新生成。"
      exit 0
    fi
    echo "警告：keystore.properties 中的密码与现有密钥库不匹配。"
    echo "请直接编辑 $PROPS_FILE 修正 storePassword/keyPassword。"
    echo "或删除后重新生成:"
    echo "  rm -f \"$KEYSTORE_FILE\" \"$PROPS_FILE\""
    exit 1
  fi
  echo "将仅为现有密钥库创建 keystore.properties。"
else
  if [[ -z "${KEYSTORE_PASSWORD:-}" ]]; then
    read -r -s -p "请输入密钥库密码（storePassword，至少 6 位）: " KEYSTORE_PASSWORD
    echo
    read -r -s -p "请再次输入密钥库密码: " KEYSTORE_PASSWORD_CONFIRM
    echo
    if [[ "$KEYSTORE_PASSWORD" != "$KEYSTORE_PASSWORD_CONFIRM" ]]; then
      echo "错误：两次输入的密钥库密码不一致。"
      exit 1
    fi
    if [[ ${#KEYSTORE_PASSWORD} -lt 6 ]]; then
      echo "错误：密钥库密码至少 6 位。"
      exit 1
    fi
  fi

  if [[ -z "${KEY_PASSWORD:-}" ]]; then
    read -r -s -p "请输入密钥密码（keyPassword，可与密钥库密码相同，直接回车则相同）: " KEY_PASSWORD
    echo
    if [[ -z "$KEY_PASSWORD" ]]; then
      KEY_PASSWORD="$KEYSTORE_PASSWORD"
    fi
  fi

  DNAME="${KEYSTORE_DNAME:-CN=物业管家助手, OU=Mobile, O=GuanjiaZhushou, L=Unknown, ST=Unknown, C=CN}"

  "$KEYTOOL" -genkeypair -v \
    -keystore "$KEYSTORE_FILE" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEY_PASSWORD" \
    -dname "$DNAME"

  echo "已生成密钥库: $KEYSTORE_FILE"
fi

if [[ -f "$PROPS_FILE" ]]; then
  echo "配置文件已存在: $PROPS_FILE"
  echo "如需更新密码，请直接编辑该文件。"
  exit 0
fi

if [[ -z "${KEYSTORE_PASSWORD:-}" || -z "${KEY_PASSWORD:-}" ]]; then
  echo ""
  echo "请为 keystore.properties 填写密码（若刚已输入生成密钥库，请再输入一次以写入配置）："
  read -r -s -p "storePassword: " KEYSTORE_PASSWORD
  echo
  read -r -s -p "keyPassword（直接回车则与 storePassword 相同）: " KEY_PASSWORD
  echo
  if [[ -z "$KEY_PASSWORD" ]]; then
    KEY_PASSWORD="$KEYSTORE_PASSWORD"
  fi
fi

cat >"$PROPS_FILE" <<EOF
# 本文件含签名密码，已加入 .gitignore，请勿提交 Git
storeFile=keystore/release.jks
storePassword=${KEYSTORE_PASSWORD}
keyAlias=${KEY_ALIAS}
keyPassword=${KEY_PASSWORD}
EOF

echo "已创建: $PROPS_FILE"
echo ""
echo "下一步："
echo "  ./scripts/build-android.sh release"
echo ""
echo "小米/应用商店上架请妥善保管："
echo "  - 密钥库文件: $KEYSTORE_FILE"
echo "  - 密钥库密码 storePassword"
echo "  - 密钥别名 keyAlias=$KEY_ALIAS"
echo "  - 密钥密码 keyPassword"
