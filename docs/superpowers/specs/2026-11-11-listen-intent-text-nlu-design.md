# 听记助手：文本意图 NLU 复核 + 提案卡展示 — 设计说明

| 项 | 内容 |
|---|---|
| 版本 | v1.0.0 |
| 状态 | 已实现（2026-11-11 17:30:20） |
| 创建时间 | 2026-11-11 16:42:47 |
| 关联 | `doc/物业管家助手/听记助手与定时提醒-解决方案.md` §5.2；实施计划 M3.5 |
| 决策摘要 | 两者都要（可见 + 强化）；强化档位=中等（Tiny 文本编码器 1–5 MB）；展示仅在提案卡抬头；技术路线 A（规则候选 → 文本编码器复核） |

## 1. 背景与目标

当前 `assets/nlu/listen_intent.onnx`（727 B）为手工特征线性分类器，ORT 可加载推理，但语义能力弱。端侧已有 `NluVerifier` / `OnDeviceNluVerifier` 与听记流水线，但 `intentLabel` 未上 UI，领域模型缺少 `ListenIntent`。

本设计目标：

1. 用 Tiny 中文文本意图 ONNX（约 1–5 MB）替换 727 B 线性 stub，降低易混句误提案。
2. 在提案确认卡抬头展示意图中文名与置信度。
3. 保持全离线、写入前确认、模型缺失硬失败（不降级回线性 stub）。

## 2. 非目标

- 状态条 / 实时字幕展示意图
- 端侧 LLM、联网 ASR/NLU、SenseVoice
- 用模型替代规则抽槽（房号、金额、标签仍由 `RuleSemanticParser` 负责）
- 用模型替代人工确认写入

## 3. 架构与数据流

```
ASR onFinal / 文本模拟 processTranscript
  → RuleSemanticParser（粗筛 CandidateAction；Ignore 丢弃）
  → 若无非 Ignore 候选：结束（不跑 NLU）
  → OnDeviceNluVerifier（WordPiece → ORT）
  → should_act=false 或过滤后无提案：结束
  → ListenUiEvent(proposals, intent)
  → ListenProposalSheet 抬头展示意图
```

约束：

- 第一级规则职责不变：抽槽与候选类型。
- 第二级只做意图判别与 `should_act`；输入为定稿句文本，不再使用 10 维手工特征向量作为模型主输入。
- 与 ASR 错峰：仅 `onFinal` / 文本模拟触发；partial 不跑 NLU。
- 资产缺失或 ORT 失败：写入 `ListenSessionState.lastError`，本句不出提案。

## 4. 模型资产与训练/导出

### 4.1 运行时契约

| 项 | 约定 |
|---|---|
| 资产路径 | `android/app/src/main/assets/nlu/listen_intent.onnx`（替换现文件） |
| 分词资产 | `android/app/src/main/assets/nlu/vocab.txt`；特殊 token 约定写入同目录 `tokenizer_meta.json`（`unk_token`/`cls_token`/`sep_token`/`pad_token`/`max_length`） |
| 输入 | `input_ids[1, L]`、`attention_mask[1, L]`，`L=32` |
| 输出 | `intent_logits[1,6]`、`should_act_logit[1,1]` |
| 意图标签顺序 | `payment`, `event`, `feature_tag`, `navigate_context`, `chitchat`, `unclear` |
| `should_act` | `sigmoid(should_act_logit) >= 0.45` 且意图不是 `chitchat` |
| 底座 | `uer/roberta-tiny-wwm-chinese`（或同等更小 MobileBERT）微调后 ONNX INT8 |
| 包体目标 | 约 1–5 MB（含 vocab） |
| 缺失策略 | 硬失败，不回退 727 B 线性模型 |

### 4.2 仓库工具链

目录：`tools/nlu/`

| 产物 | 职责 |
|---|---|
| 种子语料 | 物业口述正例 + 易混负例（玩笑提「钱」、反问、否定、闲聊）；首期数百条，可扩至 500–2000 |
| 训练脚本 | 微调分类头（intent 6 类 + should_act） |
| 导出脚本 | 导出 ONNX INT8，校验输入/输出名与 shape |
| 契约自检 | 本地 ORT 跑通样例句，断言标签与 should_act |
| 旧脚本 | `export_listen_intent_onnx.py` 退役为测试 stub 或删除；不得再覆盖正式资产 |

### 4.3 端侧实现要点

- 类名保持 `OnDeviceNluVerifier`，DI 仍绑 `NluVerifier`：
  - 加载 onnx + `vocab.txt` + `tokenizer_meta.json`
  - 对 `rawText` 做 WordPiece，padding/truncation 到 `L=32`
  - `session.run` → argmax intent、sigmoid should_act、softmax 置信度
  - 按意图过滤 `candidates`（沿用现有 `filterByIntent` 语义）
- **过滤后为空**：即使 `shouldAct` 曾为 true，也 **不出提案**（安全优先）。须删除当前 `filtered.ifEmpty { if (shouldAct) input.candidates else emptyList() }` 回退逻辑。
- 删除 `FEATURE_DIM=10` 的 `extractFeatures` 主路径。

## 5. 领域模型与 UI

### 5.1 `ListenIntent`

新建 `domain/voice/ListenIntent.kt`：

| 字段 | 类型 | 说明 |
|---|---|---|
| `label` | `String` | 英文标签，与模型一致 |
| `displayName` | `String` | 中文展示名 |
| `confidence` | `Float` | 0–1 |
| `shouldAct` | `Boolean` | 是否允许动作 |

中文映射：

| label | displayName |
|---|---|
| payment | 收费跟进 |
| event | 事件登记 |
| feature_tag | 特征标签 |
| navigate_context | 切房上下文 |
| chitchat | 闲聊 |
| unclear | 不明确 |

### 5.2 `ListenUiEvent`

增加 `intent: ListenIntent? = null`。仅在 `shouldAct` 且提案列表非空时赋值；`consumeProposals` / `clearUiEvent` 一并清空。

### 5.3 提案卡

`ListenProposalSheet` 增加参数 `intent: ListenIntent?`。对话框标题区一行：

`意图：{displayName} · {confidence 百分比}`

- 仅提案弹卡时可见；仅切房/无提案不展示。
- `ListenStatusBar` 不改意图展示。

## 6. 错误处理

| 情况 | 行为 |
|---|---|
| onnx / vocab 缺失或损坏 | 初始化或首次 verify 抛错 → Controller 记 `lastError`，本句无提案 |
| ORT 推理异常 | 同上 |
| `should_act=false` / `chitchat` | 静默不出提案（非错误） |
| 意图过滤后无提案 | 静默不出提案 |

## 7. 测试与验收

| 用例 | 期望 |
|---|---|
| 「601欠费三个月」 | 提案卡抬头「收费跟进」，确认可写入 |
| 玩笑/否定类提「钱」负例 | 不出提案 |
| 「602很吵」 | 抬头「事件登记」 |
| 「有宠物」且词表命中 | 抬头「特征标签」 |
| 删除 NLU 资产后安装 | 听记报 NLU 错误，不误提案 |
| 文本模拟入口 | 与真 ASR 同路径可验收 |
| 单元测试 | 分词 padding、标签映射、过滤空则不出卡、资产缺失硬失败 |

## 8. 主要改动面

| 路径 | 变更 |
|---|---|
| `domain/voice/ListenIntent.kt` | 新增 |
| `presentation/voice/ListenSessionController.kt` | `ListenUiEvent.intent`；透传 NLU 结果 |
| `presentation/voice/ListenProposalSheet.kt` | 抬头展示 |
| `presentation/building/BuildingGridScreen.kt` | 传 `intent` |
| `data/voice/nlu/OnDeviceNluVerifier.kt` | 文本编码器路径 |
| `assets/nlu/*` | 新 onnx + vocab |
| `tools/nlu/*` | 语料、训练、导出、自检 |
| 解决方案 / 实施计划 | 实现后同步修订记录（本设计不替代其版本流程） |

## 9. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 首期语料不足导致易混句仍误判 | 种子集覆盖方案难例表；验收用例锁定；后续扩标注 |
| 包体增大 | INT8 + `L=32`；仅 arm64 主路径；文档标明增量 |
| 分词与训练不一致 | 导出流水线绑定同一 vocab；契约自检 |
| 训练环境不可用导致无法出正式模型 | 阻塞发布正式强化版；**禁止**用 727 B stub 顶替上线 |

## 10. 决策记录

| 时间 | 决策 |
|---|---|
| 2026-11-11 16:27:00 | 确认 727 B 可跑但语义弱 |
| 2026-11-11 16:27:30 | 用户选「两者都要」 |
| 2026-11-11 16:28:30 | 强化档位选中等（文本编码器 1–5 MB） |
| 2026-11-11 16:30:30 | 展示仅提案卡抬头 |
| 2026-11-11 16:37:30 | 技术路线 A |
| 2026-11-11 16:38:00–16:42:00 | 架构 / 模型 / UI 三节设计获批 |
| 2026-11-11 16:42:47 | 设计说明落盘 |
