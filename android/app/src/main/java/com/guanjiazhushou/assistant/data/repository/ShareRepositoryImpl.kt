package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.backup.PmexPackageService
import com.guanjiazhushou.assistant.data.local.dao.ShareGroupDao
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupMemberEntity
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.data.local.mapper.toDomainShareGroups
import com.guanjiazhushou.assistant.domain.exception.ActiveShareGroupExistsException
import com.guanjiazhushou.assistant.domain.model.PmexExportResult
import com.guanjiazhushou.assistant.domain.model.PmexImportResult
import com.guanjiazhushou.assistant.domain.model.ShareGroup
import com.guanjiazhushou.assistant.domain.model.ShareGroupBuilding
import com.guanjiazhushou.assistant.domain.model.ShareGroupMember
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepositoryImpl @Inject constructor(
    private val shareGroupDao: ShareGroupDao,
    private val pmexPackageService: PmexPackageService,
) : ShareRepository {
    override fun observeGroups(): Flow<List<ShareGroup>> =
        shareGroupDao.observeAll().map { it.toDomainShareGroups() }

    override suspend fun getById(id: Long): ShareGroup? =
        shareGroupDao.getById(id)?.toDomain()

    override suspend fun getActiveGroup(): ShareGroup? =
        shareGroupDao.getActiveGroup()?.toDomain()

    override suspend fun getMembers(groupId: Long): List<ShareGroupMember> =
        shareGroupDao.getMembers(groupId).map { it.toDomain() }

    override suspend fun getSharedBuildings(groupId: Long): List<ShareGroupBuilding> =
        shareGroupDao.getSharedBuildings(groupId).map { it.toDomain() }

    override suspend fun createGroup(
        name: String,
        hostDeviceName: String,
        verifyCode: String,
    ): Long {
        val now = System.currentTimeMillis()
        val groupId = shareGroupDao.insertGroup(
            ShareGroupEntity(
                name = name,
                hostDeviceName = hostDeviceName,
                verifyCode = verifyCode,
                status = "active",
                createdAt = now,
            ),
        )
        shareGroupDao.insertMember(
            ShareGroupMemberEntity(
                groupId = groupId,
                memberDeviceName = hostDeviceName,
                joinedAt = now,
            ),
        )
        return groupId
    }

    override suspend fun joinGroup(groupId: Long, memberDeviceName: String) {
        shareGroupDao.insertMember(
            ShareGroupMemberEntity(
                groupId = groupId,
                memberDeviceName = memberDeviceName,
                joinedAt = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun joinRemoteGroup(
        name: String,
        hostDeviceName: String,
        verifyCode: String,
        memberDeviceName: String,
    ): Long {
        if (getActiveGroup() != null) {
            throw ActiveShareGroupExistsException()
        }
        val now = System.currentTimeMillis()
        val groupId = shareGroupDao.insertGroup(
            ShareGroupEntity(
                name = name,
                hostDeviceName = hostDeviceName,
                verifyCode = verifyCode,
                status = "active",
                createdAt = now,
            ),
        )
        shareGroupDao.insertMember(
            ShareGroupMemberEntity(
                groupId = groupId,
                memberDeviceName = memberDeviceName,
                joinedAt = now,
            ),
        )
        return groupId
    }

    override suspend fun dissolveGroup(groupId: Long) {
        shareGroupDao.dissolveAndCleanup(groupId, System.currentTimeMillis())
    }

    override suspend fun leaveGroup(groupId: Long, memberDeviceName: String) {
        shareGroupDao.markMemberLeft(
            groupId = groupId,
            memberDeviceName = memberDeviceName,
            leftAt = System.currentTimeMillis(),
        )
    }

    override suspend fun exportPmex(
        buildingIds: List<Long>,
        senderDeviceName: String,
        groupName: String?,
        verifyCode: String,
        outputFile: File,
    ): PmexExportResult = pmexPackageService.packBuildings(
        buildingIds = buildingIds,
        senderDeviceName = senderDeviceName,
        groupName = groupName,
        verifyCode = verifyCode,
        outputFile = outputFile,
    )

    override suspend fun importPmex(
        inputFile: File,
        verifyCode: String,
        sourceGroupId: Long?,
    ): PmexImportResult = pmexPackageService.unpackAndImport(
        inputFile = inputFile,
        verifyCode = verifyCode,
        sourceGroupId = sourceGroupId,
    )
}
