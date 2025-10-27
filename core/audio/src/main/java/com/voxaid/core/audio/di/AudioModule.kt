package com.voxaid.core.audio.di

import com.voxaid.core.audio.AsrManager
import com.voxaid.core.audio.vosk.VoskAsrManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for audio dependencies.
 * Provides ASR manager implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    /**
     * Binds ASR manager implementation to Vosk.
     * Real speech recognition with vosk-model-small-en-us-0.15
     */
    @Binds
    @Singleton
    abstract fun bindAsrManager(
        voskAsrManager: VoskAsrManager
    ): AsrManager
}