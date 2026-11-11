package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.SearchMatch

interface SearchRepository {
    suspend fun search(keyword: String): List<SearchMatch>
}
