package com.guanjiazhushou.assistant.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.guanjiazhushou.assistant.data.local.AppDatabase
import com.guanjiazhushou.assistant.data.local.dao.BuildingDao
import com.guanjiazhushou.assistant.data.local.dao.CommunityDao
import com.guanjiazhushou.assistant.data.local.dao.FeatureTagDao
import com.guanjiazhushou.assistant.data.local.dao.RoomDao
import com.guanjiazhushou.assistant.data.local.entity.BuildingEntity
import com.guanjiazhushou.assistant.data.local.entity.CommunityEntity
import com.guanjiazhushou.assistant.data.local.entity.FeatureTagEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomFeatureCrossRef
import com.guanjiazhushou.assistant.domain.exception.InvalidVerifyCodeException
import com.guanjiazhushou.assistant.domain.model.PmexBuildingSummary
import com.guanjiazhushou.assistant.domain.model.PmexExportResult
import com.guanjiazhushou.assistant.domain.model.PmexImportResult
import com.guanjiazhushou.assistant.domain.model.PmexManifest
import com.guanjiazhushou.assistant.domain.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PmexPackageService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val buildingDao: BuildingDao,
    private val roomDao: RoomDao,
    private val communityDao: CommunityDao,
    private val featureTagDao: FeatureTagDao,
) {
    private val featureIconsDir: File
        get() = File(context.filesDir, BackupService.FEATURE_ICONS_DIR)

    suspend fun packBuildings(
        buildingIds: List<Long>,
        senderDeviceName: String,
        groupName: String?,
        verifyCode: String,
        outputFile: File,
    ): PmexExportResult {
        require(buildingIds.isNotEmpty()) { "At least one building must be selected" }
        val workDir = File(context.cacheDir, "pmex_export_${System.currentTimeMillis()}")
        val packageDir = File(workDir, BackupService.PACKAGE_ROOT)
        try {
            packageDir.mkdirs()
            val buildings = buildingIds.mapNotNull { buildingDao.getById(it) }
            val communityIds = buildings.map { it.communityId }.distinct()
            val communities = communityIds.mapNotNull { communityDao.getById(it) }
            val rooms = roomDao.getByBuildingIds(buildingIds)
            val featureTagIds = collectFeatureTagIds(rooms)
            val featureTags = featureTagDao.getAll().filter { it.id in featureTagIds }
            val crossRefs = collectCrossRefs(rooms)

            val buildingSummaries = buildings.map {
                PmexBuildingSummary(name = it.name, roomCount = rooms.count { room -> room.buildingId == it.id })
            }
            val includesCustomIcons = featureTags.any { it.iconType == "custom_image" && !it.iconPath.isNullOrBlank() }

            val exportedAt = System.currentTimeMillis()
            val manifestJson = buildPmexManifest(
                senderDeviceName = senderDeviceName,
                groupName = groupName,
                verifyCode = verifyCode,
                buildings = buildingSummaries,
                includesCustomIcons = includesCustomIcons,
                exportedAt = exportedAt,
            )

            writeDataPayload(
                packageDir = packageDir,
                communities = communities,
                buildings = buildings,
                rooms = rooms,
                featureTags = featureTags,
                crossRefs = crossRefs,
            )
            copyCustomIcons(packageDir, featureTags)
            File(packageDir, BackupService.MANIFEST_FILE).writeText(manifestJson.toString(2))

            val zipFile = File(workDir, "payload.zip")
            PackageIO.zipDirectory(packageDir, zipFile)
            val checksum = PackageIO.sha256Hex(zipFile)
            manifestJson.put("checksum_sha256", checksum)
            File(packageDir, BackupService.MANIFEST_FILE).writeText(manifestJson.toString(2))
            PackageIO.zipDirectory(packageDir, zipFile)

            outputFile.parentFile?.mkdirs()
            zipFile.copyTo(outputFile, overwrite = true)

            val manifest = parseManifest(manifestJson, checksum)
            return PmexExportResult(
                filePath = outputFile.absolutePath,
                manifest = manifest,
                fileSizeBytes = outputFile.length(),
            )
        } finally {
            PackageIO.deleteRecursively(workDir)
        }
    }

    suspend fun unpackAndImport(
        inputFile: File,
        verifyCode: String,
        sourceGroupId: Long? = null,
    ): PmexImportResult {
        val workDir = File(context.cacheDir, "pmex_import_${System.currentTimeMillis()}")
        try {
            val extractDir = File(workDir, "extracted")
            PackageIO.unzipToDirectory(inputFile, extractDir)
            val packageDir = File(extractDir, BackupService.PACKAGE_ROOT)
                .takeIf { it.exists() }
                ?: extractDir

            val manifestFile = File(packageDir, BackupService.MANIFEST_FILE)
            if (!manifestFile.exists()) {
                throw InvalidVerifyCodeException()
            }
            val manifestJson = JSONObject(manifestFile.readText())
            val manifestCode = manifestJson.optString("verify_code")
            if (manifestCode != verifyCode) {
                throw InvalidVerifyCodeException()
            }

            val dataJson = JSONObject(File(packageDir, DATA_FILE).readText())
            val senderDevice = manifestJson.optString("sender_device_name")
            val communities = parseCommunities(dataJson.getJSONArray("communities"))
            val buildings = parseBuildings(dataJson.getJSONArray("buildings"))
            val rooms = parseRooms(dataJson.getJSONArray("rooms"))
            val featureTags = parseFeatureTags(dataJson.getJSONArray("feature_tags"))
            val crossRefs = parseCrossRefs(dataJson.getJSONArray("room_features"))

            restoreCustomIcons(packageDir, featureTags)

            var overwritten = false
            val importedBuildingIds = mutableListOf<Long>()

            database.withTransaction {
                communities.forEach { community ->
                    val existing = communityDao.getAll().find { it.name == community.name }
                    val communityId = existing?.id ?: communityDao.insert(community)
                    val communityBuildings = buildings.filter { it.communityId == community.id }
                    communityBuildings.forEach { building ->
                        val importedBuilding = building.copy(
                            id = 0,
                            communityId = communityId,
                            sourceType = SourceType.IMPORTED.key,
                            isReadonly = true,
                            sourceDevice = senderDevice,
                            sourceGroupId = sourceGroupId,
                            updatedAt = System.currentTimeMillis(),
                        )
                        val existingBuilding = buildingDao.getImportedBuildings()
                            .find { it.sourceDevice == senderDevice && it.name == building.name && it.communityId == communityId }
                        val buildingId = if (existingBuilding != null) {
                            overwritten = true
                            roomDao.deleteByBuilding(existingBuilding.id)
                            buildingDao.deleteById(existingBuilding.id)
                            buildingDao.insert(importedBuilding)
                        } else {
                            buildingDao.insert(importedBuilding)
                        }
                        importedBuildingIds.add(buildingId)
                        val sourceRooms = rooms.filter { it.buildingId == building.id }
                        val oldRoomIdToNumber = sourceRooms.associate { it.id to it.roomNumber }
                        val roomsToInsert = sourceRooms.map { it.copy(id = 0, buildingId = buildingId) }
                        roomDao.insertAll(roomsToInsert)

                        val newRoomByNumber = roomDao.getByBuilding(buildingId)
                            .associateBy { it.roomNumber }
                        crossRefs.filter { oldRoomIdToNumber.containsKey(it.roomId) }
                            .forEach { crossRef ->
                                val roomNumber = oldRoomIdToNumber[crossRef.roomId] ?: return@forEach
                                val newRoomId = newRoomByNumber[roomNumber]?.id ?: return@forEach
                                val tag = featureTags.find { it.id == crossRef.featureTagId } ?: return@forEach
                                val tagId = ensureFeatureTag(tag)
                                roomDao.insertRoomFeatures(
                                    listOf(RoomFeatureCrossRef(roomId = newRoomId, featureTagId = tagId)),
                                )
                            }
                    }
                }
            }

            return PmexImportResult(
                importedBuildingIds = importedBuildingIds,
                overwritten = overwritten,
                roomCount = rooms.size,
            )
        } finally {
            PackageIO.deleteRecursively(workDir)
        }
    }

    private suspend fun collectFeatureTagIds(rooms: List<RoomEntity>): Set<Long> {
        val ids = mutableSetOf<Long>()
        rooms.forEach { room ->
            ids.addAll(roomDao.getFeatureTagIdsForRoom(room.id))
        }
        return ids
    }

    private suspend fun collectCrossRefs(rooms: List<RoomEntity>): List<RoomFeatureCrossRef> =
        rooms.flatMap { room ->
            roomDao.getFeatureTagIdsForRoom(room.id).map { tagId ->
                RoomFeatureCrossRef(roomId = room.id, featureTagId = tagId)
            }
        }

    private fun writeDataPayload(
        packageDir: File,
        communities: List<CommunityEntity>,
        buildings: List<BuildingEntity>,
        rooms: List<RoomEntity>,
        featureTags: List<FeatureTagEntity>,
        crossRefs: List<RoomFeatureCrossRef>,
    ) {
        val payload = JSONObject()
            .put("communities", JSONArray(communities.map { it.toJson() }))
            .put("buildings", JSONArray(buildings.map { it.toJson() }))
            .put("rooms", JSONArray(rooms.map { it.toJson() }))
            .put("feature_tags", JSONArray(featureTags.map { it.toJson() }))
            .put("room_features", JSONArray(crossRefs.map { it.toJson() }))
        File(packageDir, DATA_FILE).writeText(payload.toString(2))
    }

    private fun copyCustomIcons(packageDir: File, featureTags: List<FeatureTagEntity>) {
        val targetDir = File(packageDir, "${BackupService.ASSETS_DIR}/${BackupService.FEATURE_ICONS_DIR}")
        featureTags
            .filter { it.iconType == "custom_image" && !it.iconPath.isNullOrBlank() }
            .forEach { tag ->
                val source = File(tag.iconPath!!)
                if (source.exists()) {
                    val target = File(targetDir, source.name)
                    target.parentFile?.mkdirs()
                    source.copyTo(target, overwrite = true)
                }
            }
    }

    private fun restoreCustomIcons(packageDir: File, featureTags: List<FeatureTagEntity>) {
        val sourceDir = File(packageDir, "${BackupService.ASSETS_DIR}/${BackupService.FEATURE_ICONS_DIR}")
        if (!sourceDir.exists()) return
        featureIconsDir.mkdirs()
        featureTags
            .filter { it.iconType == "custom_image" }
            .forEach { tag ->
                val iconName = tag.iconPath?.let { File(it).name } ?: return@forEach
                val source = File(sourceDir, iconName)
                if (source.exists()) {
                    val target = File(featureIconsDir, iconName)
                    source.copyTo(target, overwrite = true)
                }
            }
    }

    private suspend fun ensureFeatureTag(tag: FeatureTagEntity): Long {
        val existing = featureTagDao.getAll().find { it.name == tag.name }
        return existing?.id ?: featureTagDao.insert(
            tag.copy(
                id = 0,
                iconPath = tag.iconPath?.let { path ->
                    File(path).name.let { name -> File(featureIconsDir, name).absolutePath }
                },
            ),
        )
    }

    private fun buildPmexManifest(
        senderDeviceName: String,
        groupName: String?,
        verifyCode: String,
        buildings: List<PmexBuildingSummary>,
        includesCustomIcons: Boolean,
        exportedAt: Long,
    ): JSONObject {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())
        val buildingsJson = JSONArray()
        buildings.forEach { summary ->
            buildingsJson.put(
                JSONObject()
                    .put("name", summary.name)
                    .put("room_count", summary.roomCount),
            )
        }
        return JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("package_type", "group_share")
            .put("app_version", APP_VERSION)
            .put("exported_at", formatter.format(Instant.ofEpochMilli(exportedAt)))
            .put("sender_device_name", senderDeviceName)
            .put("group_name", groupName)
            .put("verify_code", verifyCode)
            .put("buildings", buildingsJson)
            .put("includes_custom_icons", includesCustomIcons)
    }

    private fun parseManifest(json: JSONObject, checksum: String): PmexManifest {
        val buildings = json.getJSONArray("buildings").let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        PmexBuildingSummary(
                            name = item.getString("name"),
                            roomCount = item.getInt("room_count"),
                        ),
                    )
                }
            }
        }
        return PmexManifest(
            formatVersion = json.getInt("format_version"),
            packageType = json.getString("package_type"),
            appVersion = json.getString("app_version"),
            exportedAt = json.getString("exported_at"),
            senderDeviceName = json.getString("sender_device_name"),
            groupName = json.optString("group_name").takeIf { it.isNotBlank() },
            verifyCode = json.getString("verify_code"),
            buildings = buildings,
            includesCustomIcons = json.optBoolean("includes_custom_icons"),
            checksumSha256 = checksum,
        )
    }

    private fun parseCommunities(array: JSONArray): List<CommunityEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    CommunityEntity(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        createdAt = json.getLong("created_at"),
                        updatedAt = json.getLong("updated_at"),
                    ),
                )
            }
        }

    private fun parseBuildings(array: JSONArray): List<BuildingEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    BuildingEntity(
                        id = json.getLong("id"),
                        communityId = json.getLong("community_id"),
                        buildingKind = json.optString("building_kind", "residential"),
                        name = json.getString("name"),
                        unitsPerFloor = json.getInt("units_per_floor"),
                        maxFloor = json.getInt("max_floor"),
                        startFloor = json.getInt("start_floor"),
                        refugeFloors = json.optString("refuge_floors", ""),
                        excludeRooms = json.optString("exclude_rooms", ""),
                        shopRooms = json.optString("shop_rooms", ""),
                        sourceType = json.optString("source_type", SourceType.LOCAL.key),
                        isReadonly = json.optBoolean("is_readonly", false),
                        sourceDevice = json.optString("source_device").takeIf { it.isNotBlank() },
                        sourceGroupId = json.optLong("source_group_id").takeIf { it > 0 },
                        createdAt = json.getLong("created_at"),
                        updatedAt = json.getLong("updated_at"),
                    ),
                )
            }
        }

    private fun parseRooms(array: JSONArray): List<RoomEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    RoomEntity(
                        id = json.getLong("id"),
                        buildingId = json.getLong("building_id"),
                        floor = json.getInt("floor"),
                        roomNumber = json.getString("room_number"),
                        roomType = json.optString("room_type", "residential"),
                        paymentType = json.optString("payment_type", "arrears"),
                        paymentMonths = json.getInt("payment_months"),
                        ownerFeedback = json.optString("owner_feedback", ""),
                        livingStatus = json.optString("living_status", "vacant_undelivered"),
                        otherNotes = json.optString("other_notes", ""),
                        updatedAt = json.getLong("updated_at"),
                    ),
                )
            }
        }

    private fun parseFeatureTags(array: JSONArray): List<FeatureTagEntity> =
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    FeatureTagEntity(
                        id = json.getLong("id"),
                        name = json.getString("name"),
                        iconType = json.getString("icon_type"),
                        iconKey = json.optString("icon_key").takeIf { it.isNotBlank() },
                        iconPath = json.optString("icon_path").takeIf { it.isNotBlank() },
                        isBuiltin = json.optBoolean("is_builtin", false),
                        sortOrder = json.optInt("sort_order", 0),
                        createdAt = json.getLong("created_at"),
                        description = json.optString("description").takeIf { it.isNotBlank() },
                    ),
                )
            }
        }

    private fun parseCrossRefs(array: JSONArray): List<RoomFeatureCrossRef> =
        buildList {
            for (index in 0 until array.length()) {
                val json = array.getJSONObject(index)
                add(
                    RoomFeatureCrossRef(
                        roomId = json.getLong("room_id"),
                        featureTagId = json.getLong("feature_tag_id"),
                    ),
                )
            }
        }

    private fun CommunityEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun BuildingEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("community_id", communityId)
        .put("building_kind", buildingKind)
        .put("name", name)
        .put("units_per_floor", unitsPerFloor)
        .put("max_floor", maxFloor)
        .put("start_floor", startFloor)
        .put("refuge_floors", refugeFloors)
        .put("exclude_rooms", excludeRooms)
        .put("shop_rooms", shopRooms)
        .put("source_type", sourceType)
        .put("is_readonly", isReadonly)
        .put("source_device", sourceDevice)
        .put("source_group_id", sourceGroupId)
        .put("created_at", createdAt)
        .put("updated_at", updatedAt)

    private fun RoomEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("building_id", buildingId)
        .put("floor", floor)
        .put("room_number", roomNumber)
        .put("room_type", roomType)
        .put("payment_type", paymentType)
        .put("payment_months", paymentMonths)
        .put("owner_feedback", ownerFeedback)
        .put("living_status", livingStatus)
        .put("other_notes", otherNotes)
        .put("updated_at", updatedAt)

    private fun FeatureTagEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("icon_type", iconType)
        .put("icon_key", iconKey)
        .put("icon_path", iconPath)
        .put("is_builtin", isBuiltin)
        .put("sort_order", sortOrder)
        .put("created_at", createdAt)
        .put("description", description)

    private fun RoomFeatureCrossRef.toJson(): JSONObject = JSONObject()
        .put("room_id", roomId)
        .put("feature_tag_id", featureTagId)

    companion object {
        private const val DATA_FILE = "data.json"
        private const val FORMAT_VERSION = 1
        private const val APP_VERSION = "1.0.0"
    }
}
