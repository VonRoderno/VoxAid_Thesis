package com.voxaid.core.common.di

import com.google.gson.Gson
import com.voxaid.core.common.logging.CrashReporter
import com.voxaid.core.common.logging.TimberCrashReporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for common utilities.
 */
@Module
@InstallIn(SingletonComponent::class)
object CommonModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

/**
 * Hilt module for logging.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {

    /**
     * Binds crash reporter implementation.
     *
     * For development: TimberCrashReporter (logs only)
     * For production: CrashlyticsReporter (Firebase Crashlytics)
     */
    @Binds
    @Singleton
    abstract fun bindCrashReporter(
        timberCrashReporter: TimberCrashReporter
    ): CrashReporter
}