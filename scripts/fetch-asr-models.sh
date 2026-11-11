#!/usr/bin/env bash
# Download offline Zipformer-zh streaming ASR assets into android app assets/asr.
# Models are NOT downloaded at app runtime (offline constraint).
#
# Official sherpa catalog type=9 (zh-14M-2023-02-23):
#   encoder int8 + decoder float32 + joiner int8
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ASSETS_DIR="$ROOT/android/app/src/main/assets/asr"
TMP_DIR="${TMPDIR:-/tmp}/guanjia-asr-$$"
MODEL_URL="${ASR_MODEL_URL:-https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23.tar.bz2}"
MODEL_NAME="sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"

mkdir -p "$ASSETS_DIR" "$TMP_DIR"
cd "$TMP_DIR"

echo "Downloading $MODEL_URL ..."
curl -L --fail --connect-timeout 30 --max-time 600 -o model.tar.bz2 "$MODEL_URL"
tar xjf model.tar.bz2

SRC="$TMP_DIR/$MODEL_NAME"
if [[ ! -d "$SRC" ]]; then
  SRC="$(find "$TMP_DIR" -maxdepth 1 -type d -name 'sherpa-onnx-streaming-zipformer*' | head -1)"
fi
test -d "$SRC"

copy_exact() {
  local src_name="$1"
  local dest_name="$2"
  local found="$SRC/$src_name"
  if [[ ! -f "$found" ]]; then
    echo "Missing required model file: $found" >&2
    exit 1
  fi
  cp -f "$found" "$ASSETS_DIR/$dest_name"
  echo " -> $dest_name ($(wc -c < "$ASSETS_DIR/$dest_name") bytes)"
}

# Match sherpa-onnx OnlineRecognizer getModelConfig(type=9)
copy_exact 'tokens.txt' 'tokens.txt'
copy_exact 'encoder-epoch-99-avg-1.int8.onnx' 'encoder.onnx'
copy_exact 'decoder-epoch-99-avg-1.onnx' 'decoder.onnx'
copy_exact 'joiner-epoch-99-avg-1.int8.onnx' 'joiner.onnx'

rm -rf "$TMP_DIR"
echo "ASR assets ready under $ASSETS_DIR"
ls -lh "$ASSETS_DIR"
