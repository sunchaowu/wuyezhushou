# 听记文本意图 NLU 复核 + 提案卡展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Tiny 中文文本编码器 ONNX 替换 727B 线性 stub，并在听记提案卡抬头展示意图与置信度。

**Architecture:** 保持「规则粗筛 → 端侧 NLU 复核 → 确认写入」。`OnDeviceNluVerifier` 改为 WordPiece + ORT 文本编码器；`ListenIntent` 经 `ListenUiEvent` 传到 `ListenProposalSheet` 抬头。模型缺失硬失败，禁止回退线性 stub。

**Tech Stack:** Kotlin / Compose / Hilt / ONNX Runtime Android 1.17.3；Python + PyTorch + transformers + onnx + onnxruntime（训练导出）；Gradle 单元测试。

**Spec:** `docs/superpowers/specs/2026-11-11-listen-intent-text-nlu-design.md`

## Global Constraints

- 全离线；运行期不下载模型
- 资产缺失 / ORT 失败 → 硬失败并写 `lastError`，本句不出提案；**禁止**降级到 727B 特征线性模型
- NLU 输入输出契约：`input_ids[1,32]`、`attention_mask[1,32]` → `intent_logits[1,6]`、`should_act_logit[1,1]`
- 意图顺序固定：`payment`, `event`, `feature_tag`, `navigate_context`, `chitchat`, `unclear`
- `should_act`：`sigmoid(logit) >= 0.45` 且意图不是 `chitchat`
- 过滤后候选为空 → 不出提案（删除 `filtered.ifEmpty { candidates }` 回退）
- 意图仅在提案卡抬头展示；状态条不展示
- 编码与注释用英文；用户可见文案用简体中文
- 文档时间戳精确到秒；统一配置文件改动需审批（本计划不改统一配置）
- 启动服务用统一脚本；验证用 `scripts/build-android.sh` / 现有 Gradle 测试入口
- 工作区若无 git：跳过 commit 步骤，在任务末记录变更文件清单

---

## File Structure

| 路径 | 职责 |
|---|---|
| `android/.../domain/voice/ListenIntent.kt` | 意图领域模型 + 中文 displayName 映射 |
| `android/.../data/voice/nlu/WordPieceTokenizer.kt` | 端侧 WordPiece（读 vocab + meta） |
| `android/.../data/voice/nlu/OnDeviceNluVerifier.kt` | 文本编码器 ORT 复核 |
| `android/.../data/voice/nlu/NluVerifier.kt` | 接口不变（结果字段已够用） |
| `android/.../presentation/voice/ListenSessionController.kt` | `ListenUiEvent.intent` 透传 |
| `android/.../presentation/voice/ListenProposalSheet.kt` | 抬头展示意图 |
| `android/.../presentation/building/BuildingGridScreen.kt` | 传入 `intent` |
| `android/app/src/main/assets/nlu/listen_intent.onnx` | 正式文本编码器模型 |
| `android/app/src/main/assets/nlu/vocab.txt` | WordPiece 词表 |
| `android/app/src/main/assets/nlu/tokenizer_meta.json` | 特殊 token / max_length |
| `tools/nlu/corpus/seed_listen_intent.jsonl` | 种子语料 |
| `tools/nlu/train_listen_intent.py` | 微调 |
| `tools/nlu/export_text_intent_onnx.py` | 导出 INT8 ONNX + 拷贝资产 |
| `tools/nlu/verify_listen_intent_contract.py` | 契约自检 |
| `tools/nlu/export_listen_intent_onnx.py` | 退役（删除或移入 `tools/nlu/legacy/`） |
| `android/app/src/test/.../ListenIntentTest.kt` | 映射单测 |
| `android/app/src/test/.../WordPieceTokenizerTest.kt` | 分词单测 |
| `android/app/src/test/.../OnDeviceNluVerifierLogicTest.kt` | 过滤/阈值逻辑（可测纯函数部分） |

---

### Task 1: `ListenIntent` 领域模型

**Files:**
- Create: `android/app/src/main/java/com/guanjiazhushou/assistant/domain/voice/ListenIntent.kt`
- Test: `android/app/src/test/java/com/guanjiazhushou/assistant/domain/voice/ListenIntentTest.kt`

**Interfaces:**
- Produces: `data class ListenIntent(label, displayName, confidence, shouldAct)`；`ListenIntent.fromLabel(label, confidence, shouldAct)`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.guanjiazhushou.assistant.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenIntentTest {
    @Test
    fun fromLabel_mapsPaymentDisplayName() {
        val intent = ListenIntent.fromLabel("payment", confidence = 0.82f, shouldAct = true)
        assertEquals("payment", intent.label)
        assertEquals("收费跟进", intent.displayName)
        assertEquals(0.82f, intent.confidence, 0.001f)
        assertEquals(true, intent.shouldAct)
    }

    @Test
    fun fromLabel_unknownFallsBackToUnclear() {
        val intent = ListenIntent.fromLabel("nope", confidence = 0.1f, shouldAct = false)
        assertEquals("unclear", intent.label)
        assertEquals("不明确", intent.displayName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests com.guanjiazhushou.assistant.domain.voice.ListenIntentTest`  
（Windows Git Bash 同命令；若中文路径需先 `subst` 再按项目惯例执行）  
Expected: FAIL（`ListenIntent` 未定义）

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.guanjiazhushou.assistant.domain.voice

data class ListenIntent(
    val label: String,
    val displayName: String,
    val confidence: Float,
    val shouldAct: Boolean,
) {
    companion object {
        private val DISPLAY = mapOf(
            "payment" to "收费跟进",
            "event" to "事件登记",
            "feature_tag" to "特征标签",
            "navigate_context" to "切房上下文",
            "chitchat" to "闲聊",
            "unclear" to "不明确",
        )

        fun fromLabel(label: String, confidence: Float, shouldAct: Boolean): ListenIntent {
            val normalized = if (DISPLAY.containsKey(label)) label else "unclear"
            return ListenIntent(
                label = normalized,
                displayName = DISPLAY.getValue(normalized),
                confidence = confidence,
                shouldAct = shouldAct,
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: same as Step 2  
Expected: PASS

- [ ] **Step 5: Commit（有 git 时）**

```bash
git add android/app/src/main/java/com/guanjiazhushou/assistant/domain/voice/ListenIntent.kt \
  android/app/src/test/java/com/guanjiazhushou/assistant/domain/voice/ListenIntentTest.kt
git commit -m "$(cat <<'EOF'
feat(voice): add ListenIntent domain model for NLU display

EOF
)"
```

---

### Task 2: WordPiece 分词器

**Files:**
- Create: `android/app/src/main/java/com/guanjiazhushou/assistant/data/voice/nlu/WordPieceTokenizer.kt`
- Create: `android/app/src/main/assets/nlu/tokenizer_meta.json`（可先用占位 meta；vocab 在 Task 4 由导出脚本写入正式文件后覆盖确认）
- Test: `android/app/src/test/java/com/guanjiazhushou/assistant/data/voice/nlu/WordPieceTokenizerTest.kt`

**Interfaces:**
- Produces: `class WordPieceTokenizer(vocab: Map<String, Int>, meta: TokenizerMeta)`；`fun encode(text: String): EncodedInput` where `EncodedInput(inputIds: LongArray, attentionMask: LongArray)`；`data class TokenizerMeta(unkToken, clsToken, sepToken, padToken, maxLength: Int)`

- [ ] **Step 1: Write the failing test**（用内存小词表，不依赖正式 onnx）

```kotlin
package com.guanjiazhushou.assistant.data.voice.nlu

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WordPieceTokenizerTest {
    @Test
    fun encode_padsToMaxLengthWithClsSep() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 1,
            "[CLS]" to 2,
            "[SEP]" to 3,
            "欠" to 10,
            "费" to 11,
            "##三" to 12,
            "个" to 13,
            "月" to 14,
        )
        val tok = WordPieceTokenizer(
            vocab = vocab,
            meta = TokenizerMeta(
                unkToken = "[UNK]",
                clsToken = "[CLS]",
                sepToken = "[SEP]",
                padToken = "[PAD]",
                maxLength = 8,
            ),
        )
        val encoded = tok.encode("欠费")
        assertEquals(8, encoded.inputIds.size)
        assertEquals(2L, encoded.inputIds[0]) // CLS
        assertEquals(3L, encoded.inputIds[3]) // SEP after 欠 费
        assertEquals(0L, encoded.inputIds[7]) // PAD
        assertArrayEquals(
            longArrayOf(1, 1, 1, 1, 0, 0, 0, 0),
            encoded.attentionMask,
        )
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests com.guanjiazhushou.assistant.data.voice.nlu.WordPieceTokenizerTest`

- [ ] **Step 3: Implement `WordPieceTokenizer`**

实现要点：

1. 解析 `tokenizer_meta.json` 的工厂：`WordPieceTokenizer.fromAssets(context)`（Task 5 再用）；本任务先支持构造器注入。
2. 基本 Bert WordPiece：按字符贪心最长匹配；未知 → `unk` id。
3. 输出固定 `maxLength`：`[CLS] + pieces + [SEP] + PAD...`；超长截断 pieces。
4. `attentionMask`：真实 token 为 1，pad 为 0。

```kotlin
package com.guanjiazhushou.assistant.data.voice.nlu

data class TokenizerMeta(
    val unkToken: String,
    val clsToken: String,
    val sepToken: String,
    val padToken: String,
    val maxLength: Int,
)

data class EncodedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
)

class WordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val meta: TokenizerMeta,
) {
    private val unkId = vocab.getValue(meta.unkToken)
    private val clsId = vocab.getValue(meta.clsToken)
    private val sepId = vocab.getValue(meta.sepToken)
    private val padId = vocab.getValue(meta.padToken)

    fun encode(text: String): EncodedInput {
        val normalized = text.replace(Regex("\\s+"), "")
        val pieces = tokenize(normalized)
        val maxBody = (meta.maxLength - 2).coerceAtLeast(0)
        val body = pieces.take(maxBody)
        val ids = LongArray(meta.maxLength) { padId.toLong() }
        val mask = LongArray(meta.maxLength)
        ids[0] = clsId.toLong()
        mask[0] = 1
        var i = 1
        for (p in body) {
            ids[i] = p.toLong()
            mask[i] = 1
            i++
        }
        if (i < meta.maxLength) {
            ids[i] = sepId.toLong()
            mask[i] = 1
        }
        return EncodedInput(ids, mask)
    }

    private fun tokenize(text: String): List<Int> {
        // Greedy WordPiece over Unicode code points; prefix "##" for continuation.
        // Exact algorithm must match tools/nlu export tokenizer (document in comment).
        val out = ArrayList<Int>()
        var start = 0
        while (start < text.length) {
            var end = text.length
            var found: Int? = null
            while (end > start) {
                val raw = text.substring(start, end)
                val piece = if (start == 0 || out.isEmpty()) raw else "##$raw"
                // First char of utterance uses raw; continuation uses ## — align with HF BertTokenizer for Chinese.
                val key = if (start == 0) raw else "##$raw"
                val id = vocab[if (start == 0) raw else key] ?: vocab[raw]
                if (id != null) {
                    found = id
                    break
                }
                end--
            }
            if (found == null) {
                out.add(unkId)
                start++
            } else {
                out.add(found)
                start = end
            }
        }
        return out
    }
}
```

> 实现时以 `tools/nlu` 导出所用 HuggingFace `BertTokenizer` 行为为准；若单测与 HF 不一致，修正 Kotlin 算法而非放宽测试。上列草稿中 `piece`/`key` 需在实现时整理为单一正确分支。

- [ ] **Step 4: Run test — expect PASS**（必要时调整测试词表以匹配最终算法）

- [ ] **Step 5: Commit（有 git 时）**

```bash
git add android/app/src/main/java/com/guanjiazhushou/assistant/data/voice/nlu/WordPieceTokenizer.kt \
  android/app/src/test/java/com/guanjiazhushou/assistant/data/voice/nlu/WordPieceTokenizerTest.kt
git commit -m "$(cat <<'EOF'
feat(voice): add on-device WordPiece tokenizer for listen NLU

EOF
)"
```

---

### Task 3: 种子语料

**Files:**
- Create: `tools/nlu/corpus/seed_listen_intent.jsonl`

**Interfaces:**
- Produces: 每行 JSON：`{"text":"...","intent":"payment|event|feature_tag|navigate_context|chitchat|unclear","should_act":true|false}`

- [ ] **Step 1: 写入种子语料（≥120 条，覆盖正例与易混负例）**

至少包含（可复制扩展同义改写）：

```jsonl
{"text":"601欠费三个月","intent":"payment","should_act":true}
{"text":"602物业费还没交","intent":"payment","should_act":true}
{"text":"说有钱但其实是开玩笑","intent":"chitchat","should_act":false}
{"text":"欠费了吗","intent":"unclear","should_act":false}
{"text":"没有欠费","intent":"chitchat","should_act":false}
{"text":"602很吵","intent":"event","should_act":true}
{"text":"楼上漏水了","intent":"event","should_act":true}
{"text":"有宠物","intent":"feature_tag","should_act":true}
{"text":"家里请了保姆住","intent":"feature_tag","should_act":true}
{"text":"去601看看缴费","intent":"navigate_context","should_act":true}
{"text":"今天天气不错","intent":"chitchat","should_act":false}
{"text":"随便聊聊","intent":"chitchat","should_act":false}
```

继续补充至 ≥120 行：每类意图 ≥15 条；`should_act=false` 的 payment/event 易混句 ≥20 条。

- [ ] **Step 2: 校验行数与标签**

```bash
python - <<'PY'
import json
from pathlib import Path
from collections import Counter
p = Path('tools/nlu/corpus/seed_listen_intent.jsonl')
rows = [json.loads(l) for l in p.read_text(encoding='utf-8').splitlines() if l.strip()]
assert len(rows) >= 120, len(rows)
labels = {"payment","event","feature_tag","navigate_context","chitchat","unclear"}
c = Counter(r['intent'] for r in rows)
assert set(c) <= labels
assert all(isinstance(r['should_act'], bool) for r in rows)
print('ok', len(rows), dict(c))
PY
```

Expected: `ok 120+ {...}`

- [ ] **Step 3: Commit（有 git 时）**

```bash
git add tools/nlu/corpus/seed_listen_intent.jsonl
git commit -m "$(cat <<'EOF'
data(nlu): add seed corpus for listen intent text classifier

EOF
)"
```

---

### Task 4: 训练、导出正式 ONNX 资产、契约自检

**Files:**
- Create: `tools/nlu/train_listen_intent.py`
- Create: `tools/nlu/export_text_intent_onnx.py`
- Create: `tools/nlu/verify_listen_intent_contract.py`
- Create/Overwrite: `android/app/src/main/assets/nlu/listen_intent.onnx`
- Create/Overwrite: `android/app/src/main/assets/nlu/vocab.txt`
- Create/Overwrite: `android/app/src/main/assets/nlu/tokenizer_meta.json`
- Modify: move/delete `tools/nlu/export_listen_intent_onnx.py` → `tools/nlu/legacy/export_listen_intent_onnx.py`（并在文件头注明不得覆盖正式资产）

**Interfaces:**
- Produces: 资产满足契约；`verify_listen_intent_contract.py` exit 0
- Consumes: `seed_listen_intent.jsonl`；底座 `uer/roberta-tiny-wwm-chinese`

- [ ] **Step 1: 实现 `train_listen_intent.py`**

要求：

- 读取 jsonl；`AutoTokenizer` + `AutoModel` 底座 `uer/roberta-tiny-wwm-chinese`
- 双头：intent 6 类 CE + should_act BCE
- 保存 `tools/nlu/artifacts/checkpoint/`（权重 + tokenizer）
- 记录训练结束真实时间到 stdout（精确到秒）

- [ ] **Step 2: 实现 `export_text_intent_onnx.py`**

要求：

- 加载 checkpoint；导出 ONNX，输入名 `input_ids`/`attention_mask`，输出名 `intent_logits`/`should_act_logit`，动态轴仅 batch（序列固定 32 亦可）
- INT8 动态量化（`onnxruntime.quantization`）后写入 `android/app/src/main/assets/nlu/listen_intent.onnx`
- 复制 `vocab.txt`；写 `tokenizer_meta.json`：

```json
{
  "unk_token": "[UNK]",
  "cls_token": "[CLS]",
  "sep_token": "[SEP]",
  "pad_token": "[PAD]",
  "max_length": 32
}
```

- 打印文件大小；目标约 1–5 MB。若 INT8 后仍 >5 MB：继续尝试 ort 量化选项并记录实际字节数到日志；**仍不得**用 727B stub 顶替。超过 5 MB 但功能正确时，在实现笔记写明实际大小，不阻塞功能合入，但需在解决方案修订记录标注包体。

- [ ] **Step 3: 实现并运行 `verify_listen_intent_contract.py`**

```python
# Pseudo-structure the real script must implement:
# - load onnx with onnxruntime
# - assert input/output names and shapes
# - tokenize with the same HF tokenizer used in training (max_length=32)
# - run cases:
CASES = [
    ("601欠费三个月", "payment", True),
    ("602很吵", "event", True),
    ("有宠物", "feature_tag", True),
    ("说有钱但其实是开玩笑", None, False),  # should_act False; intent may be chitchat/unclear
]
# - assert should_act and (if should_act) intent label
```

Run: `python tools/nlu/verify_listen_intent_contract.py`  
Expected: exit 0；打印 `contract ok` 与时间戳

- [ ] **Step 4: 退役旧导出脚本**

```bash
mkdir -p tools/nlu/legacy
git mv tools/nlu/export_listen_intent_onnx.py tools/nlu/legacy/export_listen_intent_onnx.py 2>/dev/null || mv tools/nlu/export_listen_intent_onnx.py tools/nlu/legacy/
```

在 legacy 文件顶部加注释：`Do not overwrite android/app/src/main/assets/nlu/listen_intent.onnx — linear stub only.`

- [ ] **Step 5: 确认资产非 727B**

```bash
python - <<'PY'
from pathlib import Path
p = Path('android/app/src/main/assets/nlu/listen_intent.onnx')
assert p.exists()
assert p.stat().st_size > 50_000, p.stat().st_size  # must not be 727B stub
assert Path('android/app/src/main/assets/nlu/vocab.txt').exists()
assert Path('android/app/src/main/assets/nlu/tokenizer_meta.json').exists()
print('assets ok', p.stat().st_size)
PY
```

- [ ] **Step 6: Commit（有 git 时）**

```bash
git add tools/nlu android/app/src/main/assets/nlu
git commit -m "$(cat <<'EOF'
feat(nlu): train and ship text-encoder listen_intent ONNX assets

EOF
)"
```

---

### Task 5: 重写 `OnDeviceNluVerifier`

**Files:**
- Modify: `android/app/src/main/java/com/guanjiazhushou/assistant/data/voice/nlu/OnDeviceNluVerifier.kt`
- Create: `android/app/src/test/java/com/guanjiazhushou/assistant/data/voice/nlu/OnDeviceNluFilterTest.kt`（纯过滤逻辑可抽 `internal`/`object NluIntentFilter`）

**Interfaces:**
- Consumes: `WordPieceTokenizer` + assets `nlu/listen_intent.onnx` / `vocab.txt` / `tokenizer_meta.json`
- Produces: 仍实现 `NluVerifier.verify`；**无** `filtered.ifEmpty { candidates }` 回退

- [ ] **Step 1: 抽过滤逻辑并写失败测试**

```kotlin
package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.SpeechAct
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceNluFilterTest {
    @Test
    fun filterByIntent_paymentKeepsPaymentOnly() {
        val candidates = listOf(
            CandidateAction.ProposePayment(
                roomNumber = "601",
                months = 3,
                amountYuan = null,
                paymentType = null,
                speechAct = SpeechAct.ASSERT,
                rawText = "601欠费三个月",
                needsAmountClarification = false,
            ),
            CandidateAction.ProposeEvent(
                roomNumber = "601",
                eventType = "noise",
                summary = "吵",
                rawText = "x",
            ),
        )
        val filtered = NluIntentFilter.filter(candidates, intentIndex = 0) // payment
        assertTrue(filtered.all { it is CandidateAction.ProposePayment || it is CandidateAction.NavigateToRoom })
        assertTrue(filtered.any { it is CandidateAction.ProposePayment })
    }

    @Test
    fun emptyFilterMeansNoFallback() {
        val filtered = NluIntentFilter.filter(emptyList(), intentIndex = 0)
        assertTrue(filtered.isEmpty())
    }
}
```

> 若 `CandidateAction` / `SpeechAct` 构造签名与仓库不一致，以 `domain/voice/CandidateAction.kt` 实际字段为准改写测试，不得编造字段。

- [ ] **Step 2: Run — expect FAIL**

- [ ] **Step 3: 实现 `NluIntentFilter` + 重写 verifier**

`OnDeviceNluVerifier` 要点：

```kotlin
init {
    requireAsset("nlu/listen_intent.onnx")
    requireAsset("nlu/vocab.txt")
    requireAsset("nlu/tokenizer_meta.json")
    session = env.createSession(readAssetBytes("nlu/listen_intent.onnx"))
    tokenizer = WordPieceTokenizer.fromAssets(context)
}

override fun verify(input: NluVerificationInput): NluVerificationResult {
    val encoded = tokenizer.encode(input.rawText)
    // ORT run with input_ids + attention_mask (long tensors shape [1,32])
    // intentIndex = argmax; shouldAct = sigmoid >= 0.45 && intentIndex != CHITCHAT
    val filtered = NluIntentFilter.filter(input.candidates, intentIndex)
    val shouldActFinal = shouldAct && filtered.isNotEmpty()
    return NluVerificationResult(
        shouldAct = shouldActFinal,
        confidence = softmaxMax(intentLogits),
        filteredActions = if (shouldActFinal) filtered else emptyList(),
        intentLabel = INTENT_LABELS[intentIndex],
    )
}
```

删除 `FEATURE_DIM` / `extractFeatures` / 旧 `features` 输入名。

- [ ] **Step 4: Run unit tests PASS**

```bash
cd android && ./gradlew :app:testDebugUnitTest --tests com.guanjiazhushou.assistant.data.voice.nlu.OnDeviceNluFilterTest
```

- [ ] **Step 5: Commit（有 git 时）**

```bash
git add android/app/src/main/java/com/guanjiazhushou/assistant/data/voice/nlu \
  android/app/src/test/java/com/guanjiazhushou/assistant/data/voice/nlu
git commit -m "$(cat <<'EOF'
feat(nlu): switch OnDeviceNluVerifier to text-encoder ONNX path

EOF
)"
```

---

### Task 6: `ListenUiEvent` 透传意图

**Files:**
- Modify: `android/app/src/main/java/com/guanjiazhushou/assistant/presentation/voice/ListenSessionController.kt`

**Interfaces:**
- Consumes: `NluVerificationResult.intentLabel/confidence/shouldAct` + `ListenIntent.fromLabel`
- Produces: `ListenUiEvent(proposals, navigateRoomId, ambiguousRoomNumbers, intent: ListenIntent?)`

- [ ] **Step 1: 扩展 `ListenUiEvent`**

```kotlin
data class ListenUiEvent(
    val proposals: List<CandidateAction> = emptyList(),
    val navigateRoomId: Long? = null,
    val ambiguousRoomNumbers: List<String> = emptyList(),
    val intent: ListenIntent? = null,
)
```

- [ ] **Step 2: 在 `processTranscript` 赋 intent**

在现有 `proposals` 计算之后：

```kotlin
val listenIntent = ListenIntent.fromLabel(
    label = verified.intentLabel,
    confidence = verified.confidence,
    shouldAct = verified.shouldAct,
)
if (proposals.isNotEmpty() || navigateRoomId != null || ambiguous.isNotEmpty()) {
    _uiEvent.value = ListenUiEvent(
        proposals = proposals,
        navigateRoomId = navigateRoomId,
        ambiguousRoomNumbers = ambiguous,
        intent = listenIntent.takeIf { proposals.isNotEmpty() && it.shouldAct },
    )
}
```

保持：NLU 抛错 → `lastError`，return；`!verified.shouldAct` → return（不出卡）。

- [ ] **Step 3: 编译检查**

```bash
cd android && ./gradlew :app:compileDebugKotlin
```

Expected: SUCCESS

- [ ] **Step 4: Commit（有 git 时）**

```bash
git add android/app/src/main/java/com/guanjiazhushou/assistant/presentation/voice/ListenSessionController.kt
git commit -m "$(cat <<'EOF'
feat(voice): attach ListenIntent to listen UI events

EOF
)"
```

---

### Task 7: 提案卡抬头展示

**Files:**
- Modify: `android/app/src/main/java/com/guanjiazhushou/assistant/presentation/voice/ListenProposalSheet.kt`
- Modify: `android/app/src/main/java/com/guanjiazhushou/assistant/presentation/building/BuildingGridScreen.kt`

**Interfaces:**
- Consumes: `ListenIntent?`
- Produces: 标题区文案 `意图：{displayName} · {percent}%`

- [ ] **Step 1: 扩展 `ListenProposalSheet` 参数**

```kotlin
fun ListenProposalSheet(
    proposals: List<CandidateAction>,
    intent: ListenIntent? = null,
    onConfirmPayment: (CandidateAction.ProposePayment, PaymentType, Int) -> Unit,
    onConfirmEvent: (CandidateAction.ProposeEvent) -> Unit,
    onConfirmTags: (CandidateAction.ProposeFeatureTags) -> Unit,
    onDismiss: () -> Unit,
    onSchedulePaymentFollowUp: ((CandidateAction.ProposePayment, PaymentType, Int, Long) -> Unit)? = null,
)
```

- [ ] **Step 2: 改 title**

```kotlin
title = {
    Column {
        Text("听记提案确认")
        if (intent != null) {
            val pct = (intent.confidence * 100f).toInt().coerceIn(0, 100)
            Text("意图：${intent.displayName} · ${pct}%")
        }
    }
},
```

- [ ] **Step 3: `BuildingGridScreen` 传入**

```kotlin
ListenProposalSheet(
    proposals = listenEvent.proposals,
    intent = listenEvent.intent,
    // ...existing callbacks
)
```

- [ ] **Step 4: 编译**

```bash
cd android && ./gradlew :app:compileDebugKotlin
```

Expected: SUCCESS

- [ ] **Step 5: Commit（有 git 时）**

```bash
git add android/app/src/main/java/com/guanjiazhushou/assistant/presentation/voice/ListenProposalSheet.kt \
  android/app/src/main/java/com/guanjiazhushou/assistant/presentation/building/BuildingGridScreen.kt
git commit -m "$(cat <<'EOF'
feat(voice): show NLU intent on listen proposal sheet header

EOF
)"
```

---

### Task 8: 全量单测、APK、文档修订

**Files:**
- Modify: `doc/物业管家助手/听记助手与定时提醒-解决方案.md`（修订记录）
- Modify: `doc/物业管家助手/听记助手与定时提醒-实施计划.md`（M3.5 勾选说明）
- Modify: `docs/superpowers/specs/2026-11-11-listen-intent-text-nlu-design.md` 状态 → 实现中/已完成（实现结束时）

- [ ] **Step 1: 跑 NLU/相关单测**

```bash
cd android && ./gradlew :app:testDebugUnitTest
```

Expected: PASS（既有 + 新增）

- [ ] **Step 2: 用统一脚本打 Debug APK**

```bash
./scripts/build-android.sh
```

Expected: APK 产出成功；`assets/nlu/` 含 onnx/vocab/meta

- [ ] **Step 3: 文本模拟手工验收清单**（真机或模拟器）

| 输入 | 期望 |
|---|---|
| 601欠费三个月 | 提案卡抬头含「收费跟进」 |
| 602很吵 | 「事件登记」 |
| 有宠物（词表有宠物） | 「特征标签」 |
| 说有钱但其实是开玩笑 | 不出提案 |
| 临时移走 onnx 再装 | `lastError` 含 NLU，不误提案 |

- [ ] **Step 4: 文档修订（真实时间精确到秒）**

在解决方案修订表追加一行，例如：

`| 2026-11-11 HH:MM:SS | v1.x.x | 第二级 NLU 换文本编码器 ONNX；提案卡展示 ListenIntent |`

实施计划 Task 5 / M3.5 注明资产契约与提案卡展示已落地。

- [ ] **Step 5: Commit（有 git 时）**

```bash
git add doc/物业管家助手 docs/superpowers/specs
git commit -m "$(cat <<'EOF'
docs: record text-intent NLU rollout in listen solution notes

EOF
)"
```

---

## Self-Review (plan vs spec)

| Spec 要求 | 对应任务 |
|---|---|
| 文本编码器替换 727B | Task 4–5 |
| WordPiece + meta/vocab | Task 2, 4 |
| 契约 I/O 与标签顺序 | Task 4 verify + Task 5 |
| 过滤空不出提案 / 删回退 | Task 5 |
| `ListenIntent` | Task 1 |
| `ListenUiEvent.intent` | Task 6 |
| 提案卡抬头展示 | Task 7 |
| 状态条不展示 | Task 7 不改 StatusBar |
| 硬失败不降级 | Task 4 Step 5 + Task 5 init |
| 种子语料 + 训练导出 | Task 3–4 |
| 验收用例 | Task 8 |
| 退役旧线性导出 | Task 4 Step 4 |

无 TBD/TODO 占位；类型名与 spec 一致（`ListenIntent`、`OnDeviceNluVerifier`、`tokenizer_meta.json`）。

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-11-11-listen-intent-text-nlu.md`.

**两种执行方式：**

1. **Subagent-Driven（推荐）** — 每任务派生子代理，任务间复核  
2. **Inline Execution** — 本会话按 executing-plans 批量推进并设检查点  

选哪一种？
