package com.guanjiazhushou.assistant.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.guanjiazhushou.assistant.data.local.converters.Converters
import com.guanjiazhushou.assistant.data.local.dao.AppPreferenceDao
import com.guanjiazhushou.assistant.data.local.dao.BuildingDao
import com.guanjiazhushou.assistant.data.local.dao.CommunityDao
import com.guanjiazhushou.assistant.data.local.dao.FeatureTagDao
import com.guanjiazhushou.assistant.data.local.dao.LivingStatusSettingDao
import com.guanjiazhushou.assistant.data.local.dao.ReminderDao
import com.guanjiazhushou.assistant.data.local.dao.RoomCommunicationDao
import com.guanjiazhushou.assistant.data.local.dao.RoomDao
import com.guanjiazhushou.assistant.data.local.dao.RoomEventDao
import com.guanjiazhushou.assistant.data.local.dao.SearchDao
import com.guanjiazhushou.assistant.data.local.dao.ShareGroupDao
import com.guanjiazhushou.assistant.data.local.dao.StatisticsDao
import com.guanjiazhushou.assistant.data.local.entity.AppPreferenceEntity
import com.guanjiazhushou.assistant.data.local.entity.BuildingEntity
import com.guanjiazhushou.assistant.data.local.entity.CommunityEntity
import com.guanjiazhushou.assistant.data.local.entity.FeatureTagEntity
import com.guanjiazhushou.assistant.data.local.entity.LivingStatusSettingEntity
import com.guanjiazhushou.assistant.data.local.entity.ReminderEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomCommunicationEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomEventEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomFeatureCrossRef
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupBuildingEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupMemberEntity

@Database(
    entities = [
        CommunityEntity::class,
        BuildingEntity::class,
        RoomEntity::class,
        RoomCommunicationEntity::class,
        FeatureTagEntity::class,
        RoomFeatureCrossRef::class,
        LivingStatusSettingEntity::class,
        AppPreferenceEntity::class,
        ShareGroupEntity::class,
        ShareGroupMemberEntity::class,
        ShareGroupBuildingEntity::class,
        RoomEventEntity::class,
        ReminderEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun communityDao(): CommunityDao
    abstract fun buildingDao(): BuildingDao
    abstract fun roomDao(): RoomDao
    abstract fun roomCommunicationDao(): RoomCommunicationDao
    abstract fun featureTagDao(): FeatureTagDao
    abstract fun livingStatusSettingDao(): LivingStatusSettingDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun shareGroupDao(): ShareGroupDao
    abstract fun statisticsDao(): StatisticsDao

    abstract fun searchDao(): SearchDao

    abstract fun roomEventDao(): RoomEventDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "guanjia_assistant.db"
    }
}
