package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.FeatureTag
import kotlinx.coroutines.flow.Flow

interface FeatureTagRepository {
    fun observeAll(): Flow<List<FeatureTag>>
    suspend fun getAll(): List<FeatureTag>
    suspend fun getById(id: Long): FeatureTag?
    suspend fun insert(tag: FeatureTag): Long
    suspend fun update(tag: FeatureTag)
    suspend fun deleteCustom(id: Long): Boolean
    suspend fun existsByName(name: String, excludeId: Long? = null): Boolean
    suspend fun ensureBuiltinTags()
}
