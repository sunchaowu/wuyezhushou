package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.RoomCommunicationRecord
import kotlinx.coroutines.flow.Flow
import java.io.File

interface RoomCommunicationRepository {
    fun observeByRoom(roomId: Long): Flow<List<RoomCommunicationRecord>>
    suspend fun addTextRecord(roomId: Long, content: String): Long
    suspend fun addAudioRecord(roomId: Long, audioPath: String, durationMs: Long): Long
    fun resolveAudioFile(relativePath: String): File
}
