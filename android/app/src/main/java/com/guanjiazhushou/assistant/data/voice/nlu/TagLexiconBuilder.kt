package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.voice.TagLexiconEntry

object TagLexiconBuilder {
    private val descriptionStopWords: Set<String> = setOf(
        "的", "了", "和", "或", "与", "及", "在", "有", "是", "为", "等", "把", "被",
        "这", "那", "一个", "一些", "可以", "进行", "相关", "情况",
    )

    fun build(tags: List<FeatureTag>): List<TagLexiconEntry> =
        tags.map { tag ->
            val terms = linkedSetOf<String>()
            terms += tag.name.trim()
            BuiltinFeatureTagSynonyms.synonymsFor(tag.name).forEach { terms += it }
            extractDescriptionKeywords(tag.description).forEach { terms += it }
            TagLexiconEntry(
                tagId = tag.id,
                name = tag.name,
                matchTerms = terms.filter { it.isNotBlank() }.distinct(),
            )
        }

    fun extractDescriptionKeywords(description: String?): List<String> {
        val text = description?.trim().orEmpty()
        if (text.isEmpty()) return emptyList()
        val chunks = text
            .split(Regex("[\\s,，。；;、/|]+"))
            .map { it.trim() }
            .filter { it.length >= 2 && it !in descriptionStopWords }
        val bigrams = mutableListOf<String>()
        val compact = text.replace(Regex("\\s+"), "")
        if (compact.length >= 4) {
            var i = 0
            while (i + 2 <= compact.length) {
                val gram = compact.substring(i, i + 2)
                if (gram !in descriptionStopWords) {
                    bigrams += gram
                }
                i += 2
            }
        }
        return (chunks + bigrams).distinct()
    }
}
