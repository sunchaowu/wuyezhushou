package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.SearchMatch
import com.guanjiazhushou.assistant.domain.repository.SearchRepository
import javax.inject.Inject

class SearchContentUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    suspend operator fun invoke(keyword: String): List<SearchMatch> =
        searchRepository.search(keyword)
}
