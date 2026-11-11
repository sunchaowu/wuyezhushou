package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.ParseContext
import com.guanjiazhushou.assistant.domain.voice.ParseResult
import com.guanjiazhushou.assistant.domain.voice.SpeechAct

class RuleSemanticParser {
    fun parse(rawText: String, context: ParseContext): ParseResult {
        val text = normalize(rawText)
        if (text.isBlank()) {
            return ParseResult(listOf(CandidateAction.Ignore))
        }

        val pending = context.pendingVerification
        if (pending != null) {
            when {
                containsAny(text, ListenLexicon.answerNo) -> {
                    return ParseResult(
                        listOf(
                            CandidateAction.ProposePayment(
                                roomNumber = pending.roomNumber ?: context.focusedRoomNumber,
                                paymentType = PaymentType.ARREARS,
                                months = null,
                                amountYuan = null,
                                speechAct = SpeechAct.ANSWER_NO,
                                rawText = rawText,
                            ),
                        ),
                    )
                }
                containsAny(text, ListenLexicon.answerYes) -> {
                    return ParseResult(
                        listOf(
                            CandidateAction.ProposePayment(
                                roomNumber = pending.roomNumber ?: context.focusedRoomNumber,
                                paymentType = PaymentType.NORMAL,
                                months = null,
                                amountYuan = null,
                                speechAct = SpeechAct.ANSWER_YES,
                                rawText = rawText,
                            ),
                        ),
                    )
                }
            }
        }

        val roomNumber = extractRoomNumber(text, context)
        val hasBusiness = containsAny(text, ListenLexicon.businessWords) ||
            matchFeatureTags(text, context).isNotEmpty()
        val actions = mutableListOf<CandidateAction>()

        if (roomNumber != null && hasBusiness) {
            val hitWord = ListenLexicon.businessWords.firstOrNull { text.contains(it) }
                ?: "feature_tag"
            actions += CandidateAction.NavigateToRoom(
                roomNumber = roomNumber,
                unitHint = extractUnitHint(text),
                reasonBusinessWord = hitWord,
            )
        }

        val speechAct = detectSpeechAct(text)
        if (containsAny(text, ListenLexicon.paymentWords)) {
            if (speechAct == SpeechAct.QUESTION || speechAct == SpeechAct.HEARSAY) {
                actions += CandidateAction.PendingVerify(
                    roomNumber = roomNumber ?: context.focusedRoomNumber,
                    topic = "payment",
                    rawText = rawText,
                )
            } else {
                val amount = extractAmountYuan(text)
                val months = extractMonths(text)
                val paymentType = inferPaymentType(text)
                actions += CandidateAction.ProposePayment(
                    roomNumber = roomNumber ?: context.focusedRoomNumber,
                    paymentType = paymentType,
                    months = months,
                    amountYuan = amount,
                    speechAct = speechAct,
                    rawText = rawText,
                    needsAmountClarification = amount != null && months == null,
                )
            }
        }

        if (containsAny(text, ListenLexicon.repairWords) || containsAny(text, ListenLexicon.noiseWords)) {
            val summary = when {
                containsAny(text, ListenLexicon.noiseWords) -> "噪音扰民"
                else -> "维修报修"
            }
            actions += CandidateAction.ProposeEvent(
                roomNumber = roomNumber ?: context.focusedRoomNumber,
                eventType = if (summary == "噪音扰民") "complaint" else "repair",
                summary = summary,
                rawText = rawText,
            )
        }

        val matchedTags = matchFeatureTags(text, context)
        if (matchedTags.isNotEmpty()) {
            val remove = containsAny(text, ListenLexicon.polarityRemove) &&
                (text.contains("没") || text.contains("不") || text.contains("取消") || text.contains("去掉"))
            actions += CandidateAction.ProposeFeatureTags(
                roomNumber = roomNumber ?: context.focusedRoomNumber,
                tagIds = matchedTags,
                remove = remove,
                rawText = rawText,
            )
        }

        if (actions.isEmpty()) {
            actions += CandidateAction.Ignore
        }
        return ParseResult(actions)
    }

    private fun normalize(text: String): String =
        text.trim()
            .replace("　", "")
            .replace(Regex("\\s+"), "")

    private fun containsAny(text: String, words: Set<String>): Boolean =
        words.any { text.contains(it) }

    private fun detectSpeechAct(text: String): SpeechAct = when {
        containsAny(text, ListenLexicon.hearsayMarkers) -> SpeechAct.HEARSAY
        containsAny(text, ListenLexicon.questionMarkers) || text.endsWith("？") || text.endsWith("?") ->
            SpeechAct.QUESTION
        else -> SpeechAct.ASSERT
    }

    private fun inferPaymentType(text: String): PaymentType? = when {
        text.contains("欠") || text.contains("没交") || text.contains("未交") -> PaymentType.ARREARS
        text.contains("预存") -> PaymentType.PREPAID
        text.contains("收到") || text.contains("交了") || text.contains("已交") || text.contains("到账") ->
            PaymentType.NORMAL
        else -> null
    }

    private fun extractMonths(text: String): Int? {
        val match = Regex("(\\d+|一|二|两|三|四|五|六|七|八|九|十)个?月").find(text) ?: return null
        return chineseOrDigitToInt(match.groupValues[1])
    }

    private fun extractAmountYuan(text: String): Int? {
        val match = Regex("(\\d+)\\s*元").find(text) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun extractUnitHint(text: String): Int? {
        val match = Regex("([一二三四五六七八九十\\d]+)单元").find(text) ?: return null
        return chineseOrDigitToInt(match.groupValues[1])
    }

    fun extractRoomNumber(text: String, context: ParseContext): String? {
        val patterns = listOf(
            Regex("(\\d{3,4})号?房?"),
            Regex("([一二三四五六七八九十])单元(\\d{3,4})"),
        )
        for (pattern in patterns) {
            val match = pattern.find(text) ?: continue
            val candidate = when (match.groupValues.size) {
                3 -> match.groupValues[2]
                else -> match.groupValues[1]
            }
            if (context.knownRoomNumbers.isEmpty() || candidate in context.knownRoomNumbers) {
                return candidate
            }
        }
        // Prefer longest known room number contained in text
        context.knownRoomNumbers
            .sortedByDescending { it.length }
            .firstOrNull { text.contains(it) }
            ?.let { return it }
        return null
    }

    private fun matchFeatureTags(text: String, context: ParseContext): List<Long> {
        val hits = mutableListOf<Pair<Long, Int>>()
        for (entry in context.tagLexicon) {
            val matched = entry.matchTerms
                .filter { term -> term.length >= 2 && text.contains(term) }
                .maxOfOrNull { it.length }
                ?: continue
            // Prefer name exact over weak description bigram: name match scores higher
            val score = if (text.contains(entry.name)) matched + 10 else matched
            hits += entry.tagId to score
        }
        return hits
            .sortedByDescending { it.second }
            .map { it.first }
            .distinct()
    }

    private fun chineseOrDigitToInt(raw: String): Int? {
        raw.toIntOrNull()?.let { return it }
        return when (raw) {
            "一" -> 1
            "二", "两" -> 2
            "三" -> 3
            "四" -> 4
            "五" -> 5
            "六" -> 6
            "七" -> 7
            "八" -> 8
            "九" -> 9
            "十" -> 10
            else -> null
        }
    }
}
