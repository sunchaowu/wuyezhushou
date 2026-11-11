#!/usr/bin/env bash
# Download sherpa-onnx Android AAR into android/app/libs (parallel Range).
# Prefer local AAR over flaky JitPack Gradle resolution.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="$ROOT/android/app/libs"
OUT_AAR="$OUT_DIR/sherpa-onnx-v1.13.5.aar"
VERSION="${SHERPA_ONNX_VERSION:-v1.13.5}"
URL="${SHERPA_AAR_URL:-https://jitpack.io/com/github/k2-fsa/sherpa-onnx/sherpa-onnx/${VERSION}/sherpa-onnx-${VERSION#v}.aar}"
PARTS_DIR="${TMPDIR:-/tmp}/sherpa-aar-parts-$$"
N="${SHERPA_AAR_PARTS:-8}"

mkdir -p "$OUT_DIR" "$PARTS_DIR"
SIZE="$(curl -sI -L --fail --connect-timeout 30 --max-time 60 "$URL" | tr -d '\r' | awk 'tolower($1)=="content-length:" {print $2}' | tail -1)"
test -n "$SIZE"
echo "Downloading $URL ($SIZE bytes) in $N parts..."

CHUNK=$(( (SIZE + N - 1) / N ))
pids=()
for ((i=0; i<N; i++)); do
  start=$((i * CHUNK))
  end=$(( (i + 1) * CHUNK - 1 ))
  if (( end >= SIZE )); then end=$((SIZE - 1)); fi
  part=$(printf '%s/part%02d' "$PARTS_DIR" "$i")
  (
    curl -L --fail --connect-timeout 30 --max-time 900 \
      -H "Range: bytes=${start}-${end}" \
      -o "$part" "$URL"
  ) &
  pids+=($!)
done

fail=0
for pid in "${pids[@]}"; do
  if ! wait "$pid"; then fail=1; fi
done
if [[ "$fail" -ne 0 ]]; then
  echo "One or more part downloads failed" >&2
  rm -rf "$PARTS_DIR"
  exit 1
fi

: > "$OUT_AAR.partial"
for ((i=0; i<N; i++)); do
  cat "$(printf '%s/part%02d' "$PARTS_DIR" "$i")" >> "$OUT_AAR.partial"
done
ACTUAL="$(wc -c < "$OUT_AAR.partial" | tr -d ' ')"
if [[ "$ACTUAL" != "$SIZE" ]]; then
  echo "Size mismatch: got $ACTUAL expected $SIZE" >&2
  exit 1
fi
unzip -t "$OUT_AAR.partial" >/dev/null
mv -f "$OUT_AAR.partial" "$OUT_AAR"
rm -rf "$PARTS_DIR"
echo "Sherpa AAR ready: $OUT_AAR ($SIZE bytes)"
ls -lh "$OUT_AAR"
