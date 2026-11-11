package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.SearchPattern
import com.guanjiazhushou.assistant.data.local.dao.SearchDao
import com.guanjiazhushou.assistant.data.local.mapper.toDomainMatches
import com.guanjiazhushou.assistant.domain.model.SearchMatch
import com.guanjiazhushou.assistant.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchDao: SearchDao,
) : SearchRepository {
    override suspend fun search(keyword: String): List<SearchMatch> {
        val pattern = SearchPattern.toLikePattern(keyword) ?: return emptyList()
        return searchDao.search(pattern).toDomainMatches()
    }
}
