package com.guanjiazhushou.assistant.presentation.legal

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guanjiazhushou.assistant.domain.model.LegalDocumentType
import com.guanjiazhushou.assistant.ui.theme.AppThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentScreen(
    documentType: LegalDocumentType,
    onBack: () -> Unit,
    viewModel: LegalDocumentViewModel = hiltViewModel(),
) {
    val palette = AppThemeExtras.palette
    val content = remember(documentType) { viewModel.load(documentType) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(documentType.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Text(
            text = content,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
        )
    }
}
