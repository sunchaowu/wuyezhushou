package com.guanjiazhushou.assistant.data.legal

import android.content.Context
import com.guanjiazhushou.assistant.R
import com.guanjiazhushou.assistant.domain.model.LegalDocumentType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalDocumentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun load(documentType: LegalDocumentType): String {
        val resourceId = when (documentType) {
            LegalDocumentType.APP_INTRO -> R.raw.app_intro
            LegalDocumentType.PRIVACY_POLICY -> R.raw.privacy_policy
            LegalDocumentType.TERMS_OF_SERVICE -> R.raw.terms_of_service
        }
        return context.resources.openRawResource(resourceId)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }
}
