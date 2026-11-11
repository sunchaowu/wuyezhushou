package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.ReminderItem
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeAll(): Flow<List<ReminderItem>>
    fun observeForTarget(targetType: ReminderTargetType, targetId: Long): Flow<List<ReminderItem>>
    suspend fun getById(id: Long): ReminderItem?
    suspend fun getScheduled(): List<ReminderItem>
    suspend fun insert(item: ReminderItem): Long
    suspend fun update(item: ReminderItem)
    suspend fun markFired(id: Long)
    suspend fun cancel(id: Long)
    suspend fun cancelForTarget(targetType: ReminderTargetType, targetId: Long)
}
