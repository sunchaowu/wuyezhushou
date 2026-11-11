package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.ReminderDao
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.data.local.mapper.toEntity
import com.guanjiazhushou.assistant.domain.model.ReminderItem
import com.guanjiazhushou.assistant.domain.model.ReminderStatus
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val reminderDao: ReminderDao,
) : ReminderRepository {
    override fun observeAll(): Flow<List<ReminderItem>> =
        reminderDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeForTarget(
        targetType: ReminderTargetType,
        targetId: Long,
    ): Flow<List<ReminderItem>> =
        reminderDao.observeForTarget(targetType.key, targetId)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): ReminderItem? =
        reminderDao.getById(id)?.toDomain()

    override suspend fun getScheduled(): List<ReminderItem> =
        reminderDao.getScheduled().map { it.toDomain() }

    override suspend fun insert(item: ReminderItem): Long =
        reminderDao.insert(item.toEntity())

    override suspend fun update(item: ReminderItem) {
        reminderDao.update(item.toEntity())
    }

    override suspend fun markFired(id: Long) {
        reminderDao.updateStatus(id, ReminderStatus.FIRED.key)
    }

    override suspend fun cancel(id: Long) {
        reminderDao.updateStatus(id, ReminderStatus.CANCELLED.key)
    }

    override suspend fun cancelForTarget(targetType: ReminderTargetType, targetId: Long) {
        reminderDao.cancelForTarget(targetType.key, targetId)
    }
}
