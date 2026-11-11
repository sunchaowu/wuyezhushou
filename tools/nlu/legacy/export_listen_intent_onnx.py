"""Do not overwrite android/app/src/main/assets/nlu/listen_intent.onnx — linear stub only."""
#!/usr/bin/env python3
"""Export tiny offline listen NLU ONNX (intent + should_act). Run from repo root."""

from pathlib import Path

import numpy as np
import onnx
from onnx import TensorProto, helper, numpy_helper

FEATURE_DIM = 10
INTENT_DIM = 6

W = np.zeros((FEATURE_DIM, INTENT_DIM), dtype=np.float32)
W[0, 0] = 3.0
W[1, 1] = 3.0
W[2, 1] = 3.0
W[7, 2] = 3.0
W[8, 3] = 2.5
W[3, 5] = 1.5
W[4, 5] = 1.5
W[9, 4] = 2.0
b = np.array([-0.5, -0.5, -0.5, -0.5, 0.2, -0.2], dtype=np.float32)
W_act = np.array(
    [[2.0], [2.0], [2.0], [-1.0], [-1.0], [1.5], [1.5], [2.0], [1.0], [-2.0]],
    dtype=np.float32,
)
b_act = np.array([-0.5], dtype=np.float32)

X = helper.make_tensor_value_info("features", TensorProto.FLOAT, [1, FEATURE_DIM])
Y_intent = helper.make_tensor_value_info("intent_logits", TensorProto.FLOAT, [1, INTENT_DIM])
Y_act = helper.make_tensor_value_info("should_act_logit", TensorProto.FLOAT, [1, 1])
nodes = [
    helper.make_node(
        "Gemm",
        ["features", "W_intent", "b_intent"],
        ["intent_logits"],
        name="intent_gemm",
    ),
    helper.make_node(
        "Gemm",
        ["features", "W_act", "b_act"],
        ["should_act_logit"],
        name="act_gemm",
    ),
]
graph = helper.make_graph(
    nodes,
    "listen_tiny_nlu",
    [X],
    [Y_intent, Y_act],
    [
        numpy_helper.from_array(W, "W_intent"),
        numpy_helper.from_array(b, "b_intent"),
        numpy_helper.from_array(W_act, "W_act"),
        numpy_helper.from_array(b_act, "b_act"),
    ],
)
model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 13)])
model.ir_version = 8
onnx.checker.check_model(model)
out = Path("android/app/src/main/assets/nlu/listen_intent.onnx")
out.parent.mkdir(parents=True, exist_ok=True)
onnx.save(model, str(out))
print(f"saved {out} ({out.stat().st_size} bytes)")
