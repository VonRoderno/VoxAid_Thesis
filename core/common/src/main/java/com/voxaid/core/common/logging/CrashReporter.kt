package com.voxaid.core.common.logging

import timber.log.Timber
import javax.inject.Inject

/**
 * Interface for crash reporting.
 * Abstracts Crashlytics/Sentry for testability.
 */
interface CrashReporter {
    fun logException(throwable: Throwable, message: String? = null)
    fun logMessage(message: String, priority: Int = android.util.Log.INFO)
    fun setUserProperty(key: String, value: String)
    fun setUserId(userId: String)
}

/**
 * Timber-based crash reporter stub.
 * Replace with Firebase Crashlytics or Sentry in production.
 *
 * TO INTEGRATE FIREBASE CRASHLYTICS:
 *
 * 1. Add dependencies:
 *    ```gradle
 *    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
 *    implementation("com.google.firebase:firebase-crashlytics-ktx")
 *    ```
 *
 * 2. Add google-services.json to app/
 *
 * 3. Implement CrashlyticsReporter:
 *    ```kotlin
 *    class CrashlyticsReporter : CrashReporter {
 *        private val crashlytics = FirebaseCrashlytics.getInstance()
 *
 *        override fun logException(throwable: Throwable, message: String?) {
 *            message?.let { crashlytics.log(it) }
 *            crashlytics.recordException(throwable)
 *        }
 *
 *        override fun logMessage(message: String, priority: Int) {
 *            crashlytics.log(message)
 *        }
 *
 *        override fun setUserProperty(key: String, value: String) {
 *            crashlytics.setCustomKey(key, value)
 *        }
 *
 *        override fun setUserId(userId: String) {
 *            crashlytics.setUserId(userId)
 *        }
 *    }
 *    ```
 *
 * 4. Update DI module to provide CrashlyticsReporter
 */
class TimberCrashReporter @Inject constructor(): CrashReporter {

    override fun logException(throwable: Throwable, message: String?) {
        message?.let { Timber.e(throwable, it) } ?: Timber.e(throwable)
    }

    override fun logMessage(message: String, priority: Int) {
        when (priority) {
            android.util.Log.VERBOSE -> Timber.v(message)
            android.util.Log.DEBUG -> Timber.d(message)
            android.util.Log.INFO -> Timber.i(message)
            android.util.Log.WARN -> Timber.w(message)
            android.util.Log.ERROR -> Timber.e(message)
            else -> Timber.d(message)
        }
    }

    override fun setUserProperty(key: String, value: String) {
        Timber.d("User property: $key = $value")
    }

    override fun setUserId(userId: String) {
        Timber.d("User ID: $userId")
    }
}

/**
 * Custom Timber tree that logs to crash reporter.
 */
class CrashReportingTree(
    private val crashReporter: CrashReporter
) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.VERBOSE || priority == android.util.Log.DEBUG) {
            return // Don't log verbose/debug to crash reporter
        }

        crashReporter.logMessage(message, priority)

        t?.let {
            crashReporter.logException(it, message)
        }
    }
}