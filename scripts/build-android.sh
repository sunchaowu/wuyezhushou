#!/usr/bin/env bash
# 物业管家助手 — Android 构建脚本
# 用法:
#   ./scripts/build-android.sh          # 默认 debug：编译，有在线设备时自动安装
#   ./scripts/build-android.sh debug    # 同上
#   ./scripts/build-android.sh release  # 小版本 +1 后编译已签名的 Release APK
#   ./scripts/setup-release-signing.sh  # 首次发布：生成密钥库与 keystore.properties
#   ./scripts/bump-app-version.sh       # 仅递增版本号（versionPatch/versionCode）
#   ./scripts/bump-app-version.sh --show
#   ./scripts/build-android.sh test     # 运行单元测试
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
MODE="${1:-debug}"
APK_DEBUG="$ANDROID_DIR/app/build/outputs/apk/debug/app-debug.apk"
APK_RELEASE="$ANDROID_DIR/app/build/outputs/apk/release/app-release.apk"
KEYSTORE_PROPS="$ANDROID_DIR/keystore.properties"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
if [[ ! -d "$ANDROID_HOME" ]]; then
  export ANDROID_HOME="C:/Users/sunch/android-sdk"
fi
if [[ ! -d "$ANDROID_HOME" ]]; then
  export ANDROID_HOME="C:/Program Files (x86)/Android/android-sdk"
fi
export PATH="$ANDROID_HOME/platform-tools:$PATH"

cd "$ANDROID_DIR"

GRADLE_CMD="./gradlew"
if [[ ! -x "$GRADLE_CMD" ]]; then
  GRADLE_CMD="/c/Program Files/Gradle/gradle-9.1.0/bin/gradle"
fi

has_online_device() {
  adb devices 2>/dev/null | awk 'NR>1 && $2=="device" { found=1 } END { exit !found }'
}

try_install_debug() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "未找到 adb，跳过安装。请确认 ANDROID_HOME/platform-tools 在 PATH 中。"
    return 1
  fi

  # 尝试恢复 offline 设备
  if adb devices 2>/dev/null | grep -q "offline"; then
    echo "检测到设备 offline，正在重启 adb..."
    adb kill-server >/dev/null 2>&1 || true
    sleep 1
    adb start-server >/dev/null 2>&1 || true
    sleep 1
  fi

  if has_online_device; then
    adb install -r "$APK_DEBUG"
    echo "Debug 已安装到设备。"
    return 0
  fi

  echo "未发现在线设备，跳过安装。"
  adb devices -l 2>/dev/null || true
  echo "请检查："
  echo "  1. 手机已用 USB 连接并开启「USB 调试」"
  echo "  2. 手机上已授权此电脑的调试请求"
  echo "  3. 若为 offline，可拔插数据线或执行: adb kill-server && adb start-server"
  echo "  4. 安装包已生成，可手动安装: adb install -r \"$APK_DEBUG\""
  return 1
}

case "$MODE" in
  debug|install)
    "$GRADLE_CMD" assembleDebug
    echo "APK: $APK_DEBUG"
    try_install_debug || true
    ;;
  release)
    if [[ ! -f "$KEYSTORE_PROPS" ]]; then
      echo "错误：未找到 Release 签名配置 $KEYSTORE_PROPS"
      echo "请先执行: ./scripts/setup-release-signing.sh"
      echo "或复制 android/keystore.properties.example 为 keystore.properties 并填写密码。"
      exit 1
    fi
    "$ROOT_DIR/scripts/verify-release-signing.sh"
    "$GRADLE_CMD" assembleRelease
    "$ROOT_DIR/scripts/bump-app-version.sh"
    echo "Release 已构建（已签名，未安装到设备）。"
    if [[ -f "$APK_RELEASE" ]]; then
      echo "APK: $APK_RELEASE"
    else
      echo "输出目录: $ANDROID_DIR/app/build/outputs/apk/release/"
    fi
    ;;
  test)
    "$GRADLE_CMD" testDebugUnitTest
    ;;
  *)
    echo "未知模式: $MODE (debug|release|test)"
    exit 1
    ;;
esac
