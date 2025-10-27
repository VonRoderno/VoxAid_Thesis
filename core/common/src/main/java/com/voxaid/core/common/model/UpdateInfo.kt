package com.voxaid.core.common.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for app update information.
 * Retrieved from remote endpoint to check for updates.
 */
data class UpdateInfo(
    @SerializedName("latest_version")
    val latestVersion: String,

    @SerializedName("minimum_version")
    val minimumVersion: String,

    @SerializedName("update_url")
    val updateUrl: String,

    @SerializedName("release_notes")
    val releaseNotes: String,

    @SerializedName("is_mandatory")
    val isMandatory: Boolean = false
)

/**
 * Result wrapper for update check operations.
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
    data object NoUpdateNeeded : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}