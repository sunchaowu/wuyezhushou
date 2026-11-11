#!/usr/bin/env python3
"""Export fine-tuned listen intent model to ONNX (+ INT8) and Android assets.

Import onnx before torch on Windows to avoid DLL/onnxscript issues (Python 3.8).
"""

from __future__ import annotations

import json
import os
import shutil
import sys
import tempfile
from datetime import datetime
from pathlib import Path

import onnx  # noqa: F401  # must precede torch on this environment
import torch
import torch.nn as nn
from torch.onnx import utils as onnx_utils
from transformers import AutoTokenizer

sys.path.insert(0, str(Path(__file__).resolve().parent))
from train_listen_intent import (  # noqa: E402
    ARTIFACTS,
    BASE_MODEL,
    INTENT_LABELS,
    ListenIntentModel,
    MAX_LENGTH,
)

ROOT = Path(__file__).resolve().parents[2]
EXPORT_DIR = Path(__file__).resolve().parent / "artifacts" / "export"
ASSETS = ROOT / "android" / "app" / "src" / "main" / "assets" / "nlu"


def export_fp32(model: nn.Module, out_path: Path) -> None:
    model.eval()

    class Wrapper(nn.Module):
        def __init__(self, inner: ListenIntentModel):
            super().__init__()
            self.inner = inner

        def forward(self, input_ids, attention_mask):
            return self.inner(input_ids, attention_mask)

    wrapped = Wrapper(model)
    dummy_ids = torch.ones(1, MAX_LENGTH, dtype=torch.long)
    dummy_mask = torch.ones(1, MAX_LENGTH, dtype=torch.long)
    onnx_utils.export(
        wrapped,
        (dummy_ids, dummy_mask),
        str(out_path),
        input_names=["input_ids", "attention_mask"],
        output_names=["intent_logits", "should_act_logit"],
        dynamic_axes={
            "input_ids": {0: "batch"},
            "attention_mask": {0: "batch"},
            "intent_logits": {0: "batch"},
            "should_act_logit": {0: "batch"},
        },
        opset_version=14,
    )


def write_tokenizer_assets(tokenizer, assets: Path) -> None:
    assets.mkdir(parents=True, exist_ok=True)
    id2tok = {i: t for t, i in tokenizer.get_vocab().items()}
    lines = [id2tok[i] for i in range(len(id2tok))]
    (assets / "vocab.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    meta = {
        "unk_token": str(tokenizer.unk_token),
        "cls_token": str(tokenizer.cls_token),
        "sep_token": str(tokenizer.sep_token),
        "pad_token": str(tokenizer.pad_token),
        "max_length": MAX_LENGTH,
        "intent_labels": INTENT_LABELS,
        "base_model": BASE_MODEL,
        "exported_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }
    (assets / "tokenizer_meta.json").write_text(
        json.dumps(meta, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def main() -> None:
    print(f"export start {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    assert (ARTIFACTS / "pytorch_model.bin").exists(), "run train_listen_intent.py first"
    EXPORT_DIR.mkdir(parents=True, exist_ok=True)

    tokenizer = AutoTokenizer.from_pretrained(ARTIFACTS)
    model = ListenIntentModel(BASE_MODEL)
    state = torch.load(ARTIFACTS / "pytorch_model.bin", map_location="cpu", weights_only=True)
    model.load_state_dict(state)
    model.eval()

    # Quantization shape-infer writes temp files via resolved paths; avoid non-ASCII paths.
    tmpdir = Path(tempfile.mkdtemp(prefix="listen_nlu_"))
    fp32_tmp = tmpdir / "listen_intent_fp32.onnx"
    int8_tmp = tmpdir / "listen_intent_int8.onnx"
    export_fp32(model, fp32_tmp)
    from onnxruntime.quantization import QuantType, quantize_dynamic

    prev = os.getcwd()
    try:
        os.chdir(tmpdir)
        quantize_dynamic(str(fp32_tmp), str(int8_tmp), weight_type=QuantType.QInt8)
    finally:
        os.chdir(prev)

    shutil.copy2(fp32_tmp, EXPORT_DIR / "listen_intent_fp32.onnx")
    shutil.copy2(int8_tmp, EXPORT_DIR / "listen_intent_int8.onnx")

    ASSETS.mkdir(parents=True, exist_ok=True)
    target = ASSETS / "listen_intent.onnx"
    shutil.copy2(int8_tmp, target)
    write_tokenizer_assets(tokenizer, ASSETS)

    size = target.stat().st_size
    print(f"wrote {target} ({size} bytes)")
    if size > 5_000_000:
        print(
            f"NOTE: package size {size} exceeds 5MB target; "
            "still shipping INT8 text encoder (no 727B stub)."
        )
    print(f"export end {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


if __name__ == "__main__":
    main()
