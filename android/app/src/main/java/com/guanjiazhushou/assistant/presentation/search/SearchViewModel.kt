package com.guanjiazhushou.assistant.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.SearchMatch
import com.guanjiazhushou.assistant.domain.usecase.SearchContentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchMatch> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchContentUseCase: SearchContentUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(),
    )

    init {
        viewModelScope.launch {
            _uiState
                .map { it.query }
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _uiState.update {
                            it.copy(
                                results = emptyList(),
                                isSearching = false,
                                hasSearched = false,
                            )
                        }
                        return@collect
                    }
                    _uiState.update { it.copy(isSearching = true) }
                    val results = withContext(Dispatchers.IO) {
                        searchContentUseCase(query)
                    }
                    _uiState.update {
                        it.copy(
                            results = results,
                            isSearching = false,
                            hasSearched = true,
                        )
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }
}
