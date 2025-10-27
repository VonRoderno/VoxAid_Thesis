package com.voxaid.app

import android.app.Application
import com.voxaid.core.common.logging.CrashReporter
import com.voxaid.core.common.logging.CrashReportingTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * VoxAid Application class.
 * Initializes Hilt for dependency injection and Timber for logging.
 */
@HiltAndroidApp
class VoxAidApplication : Application() {

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, plant a tree that logs to Crashlytics
            Timber.plant(CrashReportingTree(crashReporter))
        }

        // Set global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception on thread: ${thread.name}")
            crashReporter.logException(throwable, "Uncaught exception")

            // Re-throw to let system handle it
            throw throwable
        }

        Timber.d("VoxAid Application initialized")
    }
}