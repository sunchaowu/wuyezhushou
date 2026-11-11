package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.CommunityDao
import com.guanjiazhushou.assistant.data.local.entity.CommunityEntity
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.domain.model.Community
import com.guanjiazhushou.assistant.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val communityDao: CommunityDao,
) : CommunityRepository {

    override fun observeAll(): Flow<List<Community>> =
        communityDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Community> =
        communityDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: Long): Community? =
        communityDao.getById(id)?.toDomain()

    override suspend fun getOrCreateByName(name: String): Community {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "小区名称不能为空" }
        communityDao.findByName(trimmed)?.let { return it.toDomain() }
        val now = System.currentTimeMillis()
        val id = communityDao.insert(
            CommunityEntity(
                name = trimmed,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return Community(id = id, name = trimmed, createdAt = now, updatedAt = now)
    }

    override suspend fun getOrCreateDefault(): Community {
        val existing = communityDao.getAll().firstOrNull()
        if (existing != null) return existing.toDomain()
        val now = System.currentTimeMillis()
        val id = communityDao.insert(
            CommunityEntity(
                name = DEFAULT_COMMUNITY_NAME,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return Community(id = id, name = DEFAULT_COMMUNITY_NAME, createdAt = now, updatedAt = now)
    }

    companion object {
        const val DEFAULT_COMMUNITY_NAME = "默认小区"
    }
}
