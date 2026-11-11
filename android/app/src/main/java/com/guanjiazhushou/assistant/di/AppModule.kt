package com.guanjiazhushou.assistant.di

import com.guanjiazhushou.assistant.data.lan.LanHttpClientProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(provider: LanHttpClientProvider): OkHttpClient = provider.provide()
}
