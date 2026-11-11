package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.PmexExportResult
import com.guanjiazhushou.assistant.domain.model.PmexImportResult
import com.guanjiazhushou.assistant.domain.model.ShareGroup
import com.guanjiazhushou.assistant.domain.model.ShareGroupBuilding
import com.guanjiazhushou.assistant.domain.model.ShareGroupMember
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ShareRepository {
    fun observeGroups(): Flow<List<ShareGroup>>

    suspend fun getById(id: Long): ShareGroup?

    suspend fun getActiveGroup(): ShareGroup?

    suspend fun getMembers(groupId: Long): List<ShareGroupMember>

    suspend fun getSharedBuildings(groupId: Long): List<ShareGroupBuilding>

    suspend fun createGroup(name: String, hostDeviceName: String, verifyCode: String): Long

    suspend fun joinGroup(groupId: Long, memberDeviceName: String)

    suspend fun joinRemoteGroup(
        name: String,
        hostDeviceName: String,
        verifyCode: String,
        memberDeviceName: String,
    ): Long

    suspend fun dissolveGroup(groupId: Long)

    suspend fun leaveGroup(groupId: Long, memberDeviceName: String)

    suspend fun exportPmex(
        buildingIds: List<Long>,
        senderDeviceName: String,
        groupName: String?,
        verifyCode: String,
        outputFile: File,
    ): PmexExportResult

    suspend fun importPmex(
        inputFile: File,
        verifyCode: String,
        sourceGroupId: Long? = null,
    ): PmexImportResult
}
