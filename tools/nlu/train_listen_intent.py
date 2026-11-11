#!/usr/bin/env python3
"""Fine-tune tiny Chinese encoder for listen intent + should_act. Run from repo root."""

from __future__ import annotations

import json
import time
from datetime import datetime
from pathlib import Path

import torch
import torch.nn as nn
from torch.utils.data import DataLoader, Dataset
from transformers import AutoModel, AutoTokenizer

ROOT = Path(__file__).resolve().parents[2]
CORPUS = Path(__file__).resolve().parent / "corpus" / "seed_listen_intent.jsonl"
ARTIFACTS = Path(__file__).resolve().parent / "artifacts" / "checkpoint"
# Spec allows uer/roberta-tiny-wwm-chinese or smaller equivalent; L-2_H-128 is
# downloadable via hf-mirror when the original tiny-wwm id is unavailable.
BASE_MODEL = "uer/chinese_roberta_L-2_H-128"
INTENT_LABELS = [
    "payment",
    "event",
    "feature_tag",
    "navigate_context",
    "chitchat",
    "unclear",
]
LABEL2ID = {k: i for i, k in enumerate(INTENT_LABELS)}
MAX_LENGTH = 32
EPOCHS = 40
BATCH = 8
LR = 5e-5


class JsonlDataset(Dataset):
    def __init__(self, path: Path, tokenizer):
        self.rows = [
            json.loads(line)
            for line in path.read_text(encoding="utf-8").splitlines()
            if line.strip()
        ]
        self.tokenizer = tokenizer

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, idx):
        row = self.rows[idx]
        enc = self.tokenizer(
            row["text"],
            truncation=True,
            padding="max_length",
            max_length=MAX_LENGTH,
            return_tensors="pt",
        )
        return {
            "input_ids": enc["input_ids"].squeeze(0),
            "attention_mask": enc["attention_mask"].squeeze(0),
            "intent": torch.tensor(LABEL2ID[row["intent"]], dtype=torch.long),
            "should_act": torch.tensor(1.0 if row["should_act"] else 0.0, dtype=torch.float),
        }


class ListenIntentModel(nn.Module):
    def __init__(self, base_name: str):
        super().__init__()
        self.encoder = AutoModel.from_pretrained(base_name)
        hidden = self.encoder.config.hidden_size
        self.intent_head = nn.Linear(hidden, len(INTENT_LABELS))
        self.act_head = nn.Linear(hidden, 1)

    def forward(self, input_ids, attention_mask):
        out = self.encoder(input_ids=input_ids, attention_mask=attention_mask)
        cls = out.last_hidden_state[:, 0]
        intent_logits = self.intent_head(cls)
        should_act_logit = self.act_head(cls)
        return intent_logits, should_act_logit


def main() -> None:
    started = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"train start {started}")
    assert CORPUS.exists(), CORPUS
    ARTIFACTS.mkdir(parents=True, exist_ok=True)

    tokenizer = AutoTokenizer.from_pretrained(BASE_MODEL)
    model = ListenIntentModel(BASE_MODEL)
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model.to(device)

    dataset = JsonlDataset(CORPUS, tokenizer)
    loader = DataLoader(dataset, batch_size=BATCH, shuffle=True)
    optim = torch.optim.AdamW(model.parameters(), lr=LR)
    ce = nn.CrossEntropyLoss()
    bce = nn.BCEWithLogitsLoss()

    model.train()
    for epoch in range(EPOCHS):
        total = 0.0
        for batch in loader:
            optim.zero_grad()
            input_ids = batch["input_ids"].to(device)
            attention_mask = batch["attention_mask"].to(device)
            intent_logits, act_logit = model(input_ids, attention_mask)
            loss = ce(intent_logits, batch["intent"].to(device)) + bce(
                act_logit.squeeze(-1),
                batch["should_act"].to(device),
            )
            loss.backward()
            optim.step()
            total += float(loss.item())
        print(f"epoch {epoch + 1}/{EPOCHS} loss={total / max(len(loader), 1):.4f}")

    model.cpu()
    torch.save(model.state_dict(), ARTIFACTS / "pytorch_model.bin")
    tokenizer.save_pretrained(ARTIFACTS)
    meta = {
        "base_model": BASE_MODEL,
        "intent_labels": INTENT_LABELS,
        "max_length": MAX_LENGTH,
        "trained_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
    }
    (ARTIFACTS / "listen_intent_meta.json").write_text(
        json.dumps(meta, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"saved checkpoint -> {ARTIFACTS}")
    print(f"train end {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")


if __name__ == "__main__":
    main()
