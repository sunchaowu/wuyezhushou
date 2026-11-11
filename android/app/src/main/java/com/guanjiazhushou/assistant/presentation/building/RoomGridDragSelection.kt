package com.guanjiazhushou.assistant.presentation.building

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput

internal fun findRoomAt(bounds: Map<Long, Rect>, position: Offset): Long? =
    bounds.entries.firstOrNull { (_, rect) -> rect.contains(position) }?.key

internal fun Modifier.roomGridDragSelect(
    enabled: Boolean,
    roomBounds: Map<Long, Rect>,
    onSelectionStart: (Long) -> Unit,
    onSelectionAdd: (Long) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(roomBounds.keys) {
        var lastAdded: Long? = null
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                lastAdded = null
                findRoomAt(roomBounds, offset)?.let { roomId ->
                    lastAdded = roomId
                    onSelectionStart(roomId)
                }
            },
            onDrag = { change, _ ->
                findRoomAt(roomBounds, change.position)?.let { roomId ->
                    if (roomId != lastAdded) {
                        lastAdded = roomId
                        onSelectionAdd(roomId)
                    }
                }
            },
            onDragEnd = { lastAdded = null },
            onDragCancel = { lastAdded = null },
        )
    }
}
