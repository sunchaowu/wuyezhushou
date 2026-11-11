package com.guanjiazhushou.assistant.data.repository

import android.content.Context
import com.guanjiazhushou.assistant.data.local.dao.RoomCommunicationDao
import com.guanjiazhushou.assistant.data.local.entity.RoomCommunicationEntity
import com.guanjiazhushou.assistant.data.local.mapper.toDomainRecords
import com.guanjiazhushou.assistant.domain.model.CommunicationRecordType
import com.guanjiazhushou.assistant.domain.model.RoomCommunicationRecord
import com.guanjiazhushou.assistant.domain.repository.RoomCommunicationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCommunicationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val roomCommunicationDao: RoomCommunicationDao,
) : RoomCommunicationRepository {

    override fun observeByRoom(roomId: Long): Flow<List<RoomCommunicationRecord>> =
        roomCommunicationDao.observeByRoom(roomId).map { it.toDomainRecords() }

    override suspend fun addTextRecord(roomId: Long, content: String): Long {
        val trimmed = content.trim()
        require(trimmed.isNotEmpty()) { "Text content must not be blank" }
        return roomCommunicationDao.insert(
            RoomCommunicationEntity(
                roomId = roomId,
                recordType = CommunicationRecordType.TEXT.key,
                content = trimmed,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun addAudioRecord(roomId: Long, audioPath: String, durationMs: Long): Long =
        roomCommunicationDao.insert(
            RoomCommunicationEntity(
                roomId = roomId,
                recordType = CommunicationRecordType.AUDIO.key,
                audioPath = audioPath,
                durationMs = durationMs.coerceAtLeast(0),
                createdAt = System.currentTimeMillis(),
            ),
        )

    override fun resolveAudioFile(relativePath: String): File =
        File(context.filesDir, relativePath)
}
