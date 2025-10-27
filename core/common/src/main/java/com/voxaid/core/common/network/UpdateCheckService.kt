package com.voxaid.core.common.network

import com.voxaid.core.common.model.UpdateCheckResult
import com.voxaid.core.common.model.UpdateInfo
import retrofit2.Response
import retrofit2.http.GET
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit API interface for update checks.
 */
interface UpdateApi {
    @GET("voxaid/update-info.json")
    suspend fun getUpdateInfo(): Response<UpdateInfo>
}

/**
 * Service to check for app updates from remote endpoint.
 * TODO: Replace placeholder URL with actual update endpoint.
 */
@Singleton
class UpdateCheckService @Inject constructor(
    private val updateApi: UpdateApi
) {
    companion object {
        // Current app version - should match build.gradle.kts versionName
        private const val CURRENT_VERSION = "1.0.0"
    }

    /**
     * Checks if an app update is available.
     * Compares current version with remote latest version.
     */
    suspend fun checkForUpdate(): UpdateCheckResult {
        return try {
            val response = updateApi.getUpdateInfo()

            if (response.isSuccessful) {
                val updateInfo = response.body()
                if (updateInfo != null) {
                    when {
                        isUpdateRequired(updateInfo.minimumVersion) -> {
                            Timber.i("Mandatory update required: ${updateInfo.latestVersion}")
                            UpdateCheckResult.UpdateAvailable(
                                updateInfo.copy(isMandatory = true)
                            )
                        }
                        isUpdateAvailable(updateInfo.latestVersion) -> {
                            Timber.i("Optional update available: ${updateInfo.latestVersion}")
                            UpdateCheckResult.UpdateAvailable(updateInfo)
                        }
                        else -> {
                            Timber.d("App is up to date")
                            UpdateCheckResult.NoUpdateNeeded
                        }
                    }
                } else {
                    Timber.w("Empty response from update endpoint")
                    UpdateCheckResult.Error("Invalid response")
                }
            } else {
                Timber.w("Update check failed: ${response.code()}")
                UpdateCheckResult.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Update check exception")
            // Don't block app if update check fails
            UpdateCheckResult.NoUpdateNeeded
        }
    }

    /**
     * Checks if current version is below minimum required version.
     */
    private fun isUpdateRequired(minimumVersion: String): Boolean {
        return compareVersions(CURRENT_VERSION, minimumVersion) < 0
    }

    /**
     * Checks if a newer version is available.
     */
    private fun isUpdateAvailable(latestVersion: String): Boolean {
        return compareVersions(CURRENT_VERSION, latestVersion) < 0
    }

    /**
     * Compares two semantic version strings (e.g., "1.0.0").
     * Returns: negative if v1 < v2, zero if equal, positive if v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = parts1.getOrNull(i) ?: 0
            val p2 = parts2.getOrNull(i) ?: 0

            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }

        return 0
    }
}

/**
 * Example JSON response for update endpoint:
 *
 * {
 *   "latest_version": "1.1.0",
 *   "minimum_version": "1.0.0",
 *   "update_url": "https://play.google.com/store/apps/details?id=com.voxaid.app",
 *   "release_notes": "Bug fixes and performance improvements",
 *   "is_mandatory": false
 * }
 */