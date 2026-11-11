package com.guanjiazhushou.assistant.di

import android.content.Context
import androidx.room.Room
import com.guanjiazhushou.assistant.data.local.AppDatabase
import com.guanjiazhushou.assistant.data.local.DatabaseCallback
import com.guanjiazhushou.assistant.data.local.migration.MIGRATION_1_2
import com.guanjiazhushou.assistant.data.local.migration.MIGRATION_2_3
import com.guanjiazhushou.assistant.data.local.migration.MIGRATION_3_4
import com.guanjiazhushou.assistant.data.local.migration.MIGRATION_4_5
import com.guanjiazhushou.assistant.data.local.migration.MIGRATION_5_6
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        .addCallback(DatabaseCallback())
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )
        .build()

    @Provides fun provideCommunityDao(database: AppDatabase): CommunityDao = database.communityDao()

    @Provides fun provideBuildingDao(database: AppDatabase): BuildingDao = database.buildingDao()

    @Provides fun provideRoomDao(database: AppDatabase): RoomDao = database.roomDao()

    @Provides
    fun provideRoomCommunicationDao(database: AppDatabase): RoomCommunicationDao =
        database.roomCommunicationDao()

    @Provides fun provideFeatureTagDao(database: AppDatabase): FeatureTagDao = database.featureTagDao()

    @Provides
    fun provideLivingStatusSettingDao(database: AppDatabase): LivingStatusSettingDao =
        database.livingStatusSettingDao()

    @Provides
    fun provideAppPreferenceDao(database: AppDatabase): AppPreferenceDao = database.appPreferenceDao()

    @Provides fun provideShareGroupDao(database: AppDatabase): ShareGroupDao = database.shareGroupDao()

    @Provides fun provideStatisticsDao(database: AppDatabase): StatisticsDao = database.statisticsDao()

    @Provides fun provideSearchDao(database: AppDatabase): SearchDao = database.searchDao()

    @Provides fun provideRoomEventDao(database: AppDatabase): RoomEventDao = database.roomEventDao()

    @Provides fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()
}
