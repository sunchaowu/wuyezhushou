package com.guanjiazhushou.assistant.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.ui.theme.SemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    title: String,
    rooms: List<Room>,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (rooms.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "暂无数据",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomListItem(room = room)
                }
            }
        }
    }
}

@Composable
private fun RoomListItem(room: Room) {
    val paymentColor = when (room.paymentType) {
        com.guanjiazhushou.assistant.domain.model.PaymentType.ARREARS -> SemanticColors.Error
        com.guanjiazhushou.assistant.domain.model.PaymentType.PREPAID -> SemanticColors.Success
        com.guanjiazhushou.assistant.domain.model.PaymentType.NORMAL -> SemanticColors.Neutral
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = room.roomNumber,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${room.paymentType.label} ${room.paymentMonths}${room.paymentType.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = paymentColor,
            )
            Text(
                text = LivingStatus.resolveLabel(room.livingStatusKey),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
