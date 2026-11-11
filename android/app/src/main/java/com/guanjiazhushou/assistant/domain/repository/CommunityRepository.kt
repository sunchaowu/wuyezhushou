package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.Community
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {
    fun observeAll(): Flow<List<Community>>
    suspend fun getAll(): List<Community>
    suspend fun getById(id: Long): Community?
    suspend fun getOrCreateDefault(): Community
    suspend fun getOrCreateByName(name: String): Community
}
