#!/usr/bin/env python3
"""Verify listen_intent ONNX I/O contract and sample predictions."""

from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path

import numpy as np
import onnxruntime as ort
from transformers import AutoTokenizer

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "android" / "app" / "src" / "main" / "assets" / "nlu"
ARTIFACTS = Path(__file__).resolve().parent / "artifacts" / "checkpoint"
INTENT_LABELS = [
    "payment",
    "event",
    "feature_tag",
    "navigate_context",
    "chitchat",
    "unclear",
]
MAX_LENGTH = 32
SHOULD_ACT_THRESHOLD = 0.45

CASES = [
    ("601欠费三个月", "payment", True),
    ("602很吵", "event", True),
    ("有宠物", "feature_tag", True),
    ("说有钱但其实是开玩笑", None, False),
]


def sigmoid(x: float) -> float:
    return float(1.0 / (1.0 + np.exp(-x)))


def main() -> None:
    onnx_path = ASSETS / "listen_intent.onnx"
    meta_path = ASSETS / "tokenizer_meta.json"
    vocab_path = ASSETS / "vocab.txt"
    assert onnx_path.exists(), onnx_path
    assert meta_path.exists(), meta_path
    assert vocab_path.exists(), vocab_path
    assert onnx_path.stat().st_size > 50_000, onnx_path.stat().st_size

    sess = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    in_names = [i.name for i in sess.get_inputs()]
    out_names = [o.name for o in sess.get_outputs()]
    assert in_names == ["input_ids", "attention_mask"], in_names
    assert out_names == ["intent_logits", "should_act_logit"], out_names

    tokenizer = AutoTokenizer.from_pretrained(ARTIFACTS)
    for text, expect_intent, expect_act in CASES:
        enc = tokenizer(
            text,
            truncation=True,
            padding="max_length",
            max_length=MAX_LENGTH,
            return_tensors="np",
        )
        intent_logits, act_logit = sess.run(
            None,
            {
                "input_ids": enc["input_ids"].astype(np.int64),
                "attention_mask": enc["attention_mask"].astype(np.int64),
            },
        )
        intent_idx = int(np.argmax(intent_logits[0]))
        intent = INTENT_LABELS[intent_idx]
        act_prob = sigmoid(float(np.array(act_logit).reshape(-1)[0]))
        should_act = act_prob >= SHOULD_ACT_THRESHOLD and intent != "chitchat"
        print(f"case={text!r} intent={intent} act_prob={act_prob:.3f} should_act={should_act}")
        assert should_act is expect_act, (text, should_act, expect_act)
        if expect_act:
            assert intent == expect_intent, (text, intent, expect_intent)

    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    assert meta["max_length"] == MAX_LENGTH
    print(f"contract ok {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


if __name__ == "__main__":
    main()
