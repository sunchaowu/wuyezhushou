package com.guanjiazhushou.assistant.di

import android.content.Context
import com.guanjiazhushou.assistant.data.voice.asr.OfflineAsrEngine
import com.guanjiazhushou.assistant.data.voice.asr.SherpaOfflineAsrEngine
import com.guanjiazhushou.assistant.data.voice.nlu.NluVerifier
import com.guanjiazhushou.assistant.data.voice.nlu.OnDeviceNluVerifier
import com.guanjiazhushou.assistant.data.voice.nlu.RuleSemanticParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {
    @Provides
    @Singleton
    fun provideRuleSemanticParser(): RuleSemanticParser = RuleSemanticParser()

    @Provides
    @Singleton
    fun provideNluVerifier(@ApplicationContext context: Context): NluVerifier =
        OnDeviceNluVerifier(context)

    @Provides
    @Singleton
    fun provideOfflineAsrEngine(@ApplicationContext context: Context): OfflineAsrEngine =
        SherpaOfflineAsrEngine(context)
}
