# Offline ASR assets (M5)

Place sherpa-onnx Zipformer-zh files under:

`android/app/src/main/assets/asr/`

Required filenames (see `SherpaOfflineAsrEngine.REQUIRED_ASSET_FILES`):

- `tokens.txt`
- `encoder.onnx`
- `decoder.onnx`
- `joiner.onnx`

Also add the sherpa-onnx Android AAR / JNI to the app module. Until both assets and native library are present, `SherpaOfflineAsrEngine.start()` throws an explicit error and does **not** fall back to cloud or fake ASR.

Text simulation for development remains available via the grid「模拟口述」entry (M3).
