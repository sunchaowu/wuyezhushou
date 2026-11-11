package com.guanjiazhushou.assistant.presentation.legal

import androidx.lifecycle.ViewModel
import com.guanjiazhushou.assistant.data.legal.LegalDocumentRepository
import com.guanjiazhushou.assistant.domain.model.LegalDocumentType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LegalDocumentViewModel @Inject constructor(
    private val legalDocumentRepository: LegalDocumentRepository,
) : ViewModel() {

    fun load(documentType: LegalDocumentType): String =
        legalDocumentRepository.load(documentType)
}
