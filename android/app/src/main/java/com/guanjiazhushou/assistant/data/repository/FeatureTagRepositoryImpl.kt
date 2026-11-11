package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.FeatureTagDao
import com.guanjiazhushou.assistant.data.local.entity.FeatureTagEntity
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.data.local.mapper.toDomainFeatureTags
import com.guanjiazhushou.assistant.data.local.mapper.toEntity
import com.guanjiazhushou.assistant.domain.model.BuiltinFeatureTags
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.IconType
import com.guanjiazhushou.assistant.domain.repository.FeatureTagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureTagRepositoryImpl @Inject constructor(
    private val featureTagDao: FeatureTagDao,
) : FeatureTagRepository {
    override fun observeAll(): Flow<List<FeatureTag>> =
        featureTagDao.observeAll().map { it.toDomainFeatureTags() }

    override suspend fun getAll(): List<FeatureTag> =
        featureTagDao.getAll().toDomainFeatureTags()

    override suspend fun getById(id: Long): FeatureTag? =
        featureTagDao.getById(id)?.toDomain()

    override suspend fun insert(tag: FeatureTag): Long =
        featureTagDao.insert(tag.toEntity())

    override suspend fun update(tag: FeatureTag) {
        featureTagDao.update(tag.toEntity())
    }

    override suspend fun deleteCustom(id: Long): Boolean =
        featureTagDao.deleteCustomById(id) > 0

    override suspend fun existsByName(name: String, excludeId: Long?): Boolean {
        val trimmed = name.trim()
        return if (excludeId == null) {
            featureTagDao.countByName(trimmed) > 0
        } else {
            featureTagDao.countByNameExcluding(trimmed, excludeId) > 0
        }
    }

    override suspend fun ensureBuiltinTags() {
        val existing = featureTagDao.getAll()
        val now = System.currentTimeMillis()
        BuiltinFeatureTags.definitions.forEach { definition ->
            val match = existing.find { entity ->
                entity.isBuiltin &&
                    (entity.iconKey == definition.iconKey || entity.sortOrder == definition.sortOrder)
            }
            if (match == null) {
                if (featureTagDao.countByName(definition.name) == 0) {
                    featureTagDao.insert(
                        FeatureTagEntity(
                            name = definition.name,
                            iconType = IconType.BUILTIN.key,
                            iconKey = definition.iconKey,
                            isBuiltin = true,
                            sortOrder = definition.sortOrder,
                            createdAt = now,
                        ),
                    )
                }
            } else if (
                match.iconType != IconType.BUILTIN.key ||
                match.iconKey != definition.iconKey ||
                match.iconPath != null
            ) {
                featureTagDao.update(
                    match.copy(
                        iconType = IconType.BUILTIN.key,
                        iconKey = definition.iconKey,
                        iconPath = null,
                        isBuiltin = true,
                    ),
                )
            }
        }
    }
}
