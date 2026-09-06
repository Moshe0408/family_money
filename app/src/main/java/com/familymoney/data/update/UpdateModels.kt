package com.familymoney.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One row of `latest_release` in Supabase. */
@Serializable
data class ReleaseInfo(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("apk_url") val apkUrl: String,
    @SerialName("file_size") val fileSize: Long = 0,
    val sha256: String? = null,
    @SerialName("release_notes") val releaseNotes: String = "",
    val mandatory: Boolean = false,
    @SerialName("min_sdk") val minSdk: Int = 26,
    @SerialName("created_at") val createdAt: String = ""
)

/** Where the updater currently is, start to finish. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState

    /** Already on the newest version. */
    data class UpToDate(val currentVersion: String) : UpdateState

    data class Available(val release: ReleaseInfo) : UpdateState

    data class Downloading(
        val release: ReleaseInfo,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState {
        val percent: Int
            get() = if (totalBytes <= 0) 0
            else ((bytesDownloaded * 100) / totalBytes).toInt().coerceIn(0, 100)
    }

    /** Downloaded and verified; waiting for the user to confirm the install. */
    data class ReadyToInstall(val release: ReleaseInfo, val filePath: String) : UpdateState

    data class Failed(val message: String, val release: ReleaseInfo? = null) : UpdateState
}
