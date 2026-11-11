package com.guanjiazhushou.assistant.di

import com.guanjiazhushou.assistant.data.repository.BackupRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.BuildingRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.CommunityRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.FeatureTagRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.ReminderRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.RoomCommunicationRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.RoomEventRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.RoomRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.SearchRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.SettingsRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.ShareRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.StatisticsRepositoryImpl
import com.guanjiazhushou.assistant.data.repository.ThemeRepositoryImpl
import com.guanjiazhushou.assistant.domain.repository.BackupRepository
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.CommunityRepository
import com.guanjiazhushou.assistant.domain.repository.FeatureTagRepository
import com.guanjiazhushou.assistant.domain.repository.ReminderRepository
import com.guanjiazhushou.assistant.domain.repository.RoomCommunicationRepository
import com.guanjiazhushou.assistant.domain.repository.RoomEventRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import com.guanjiazhushou.assistant.domain.repository.SearchRepository
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import com.guanjiazhushou.assistant.domain.repository.StatisticsRepository
import com.guanjiazhushou.assistant.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBuildingRepository(impl: BuildingRepositoryImpl): BuildingRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(impl: CommunityRepositoryImpl): CommunityRepository

    @Binds
    @Singleton
    abstract fun bindRoomRepository(impl: RoomRepositoryImpl): RoomRepository

    @Binds
    @Singleton
    abstract fun bindRoomCommunicationRepository(
        impl: RoomCommunicationRepositoryImpl,
    ): RoomCommunicationRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(impl: StatisticsRepositoryImpl): StatisticsRepository

    @Binds
    @Singleton
    abstract fun bindFeatureTagRepository(impl: FeatureTagRepositoryImpl): FeatureTagRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindShareRepository(impl: ShareRepositoryImpl): ShareRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindRoomEventRepository(impl: RoomEventRepositoryImpl): RoomEventRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository
}
