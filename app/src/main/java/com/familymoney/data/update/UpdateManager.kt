package com.familymoney.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.familymoney.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Checks Supabase for a newer APK, downloads it, verifies its hash and hands it
 * to the system installer.
 *
 * The download is done with OkHttp rather than DownloadManager so progress can
 * be reported inside the app and the file lands in our own cache directory,
 * where no storage permission is needed.
 */
class UpdateManager(private val context: Context) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** False when the build has no Supabase credentials compiled in. */
    val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val currentVersionName: String get() = BuildConfig.VERSION_NAME
    val currentVersionCode: Int get() = BuildConfig.VERSION_CODE

    // ------------------------------------------------------------------ check

    suspend fun check(): UpdateState = withContext(Dispatchers.IO) {
        if (!configured) {
            return@withContext UpdateState.Failed("לא הוגדר שרת עדכונים בגרסה זו.")
                .also { _state.value = it }
        }

        _state.value = UpdateState.Checking

        val url = BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/rest/v1/latest_release?select=*&limit=1"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        val result = try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    !response.isSuccessful ->
                        UpdateState.Failed("שרת העדכונים החזיר שגיאה ${response.code}.")

                    else -> {
                        val releases = json.decodeFromString(
                            kotlinx.serialization.builtins.ListSerializer(ReleaseInfo.serializer()),
                            body
                        )
                        val latest = releases.firstOrNull()
                        when {
                            latest == null ->
                                UpdateState.UpToDate(currentVersionName)

                            latest.versionCode <= currentVersionCode ->
                                UpdateState.UpToDate(currentVersionName)

                            Build.VERSION.SDK_INT < latest.minSdk ->
                                UpdateState.Failed(
                                    "גרסה ${latest.versionName} דורשת אנדרואיד חדש יותר."
                                )

                            else -> UpdateState.Available(latest)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            UpdateState.Failed("אין חיבור לאינטרנט.")
        } catch (e: Exception) {
            UpdateState.Failed("בדיקת העדכון נכשלה: ${e.message ?: "שגיאה לא ידועה"}")
        }

        _state.value = result
        result
    }

    // --------------------------------------------------------------- download

    suspend fun download(release: ReleaseInfo): UpdateState = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Downloading(release, 0, release.fileSize)

        val target = File(updateDir(), "familymoney-${release.versionCode}.apk")
        // A previous partial download must not be mistaken for a complete one.
        target.delete()

        val result = try {
            val request = Request.Builder().url(release.apkUrl).get().build()

            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use UpdateState.Failed("ההורדה נכשלה (${response.code}).", release)
                }
                val body = response.body
                    ?: return@use UpdateState.Failed("ההורדה החזירה תוכן ריק.", release)

                val total = body.contentLength().takeIf { it > 0 } ?: release.fileSize
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                var lastReported = 0L

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            downloaded += read

                            // Throttle UI updates; every chunk would thrash recomposition.
                            if (downloaded - lastReported >= PROGRESS_STEP) {
                                lastReported = downloaded
                                _state.value = UpdateState.Downloading(release, downloaded, total)
                            }
                        }
                    }
                }

                _state.value = UpdateState.Downloading(release, downloaded, total)

                val actual = digest.digest()
                    .joinToString("") { "%02x".format(it) }

                val expected = release.sha256?.lowercase()?.trim()
                if (!expected.isNullOrBlank() && expected != actual) {
                    target.delete()
                    UpdateState.Failed(
                        "הקובץ שהתקבל אינו תואם לחתימה שפורסמה. ההתקנה בוטלה.",
                        release
                    )
                } else {
                    UpdateState.ReadyToInstall(release, target.absolutePath)
                }
            }
        } catch (e: IOException) {
            target.delete()
            UpdateState.Failed("ההורדה נקטעה. בדקו את החיבור ונסו שוב.", release)
        } catch (e: Exception) {
            target.delete()
            UpdateState.Failed("ההורדה נכשלה: ${e.message ?: "שגיאה לא ידועה"}", release)
        }

        _state.value = result
        result
    }

    // ---------------------------------------------------------------- install

    /**
     * Android will not install a package without the user's explicit consent,
     * so this opens the system installer rather than installing silently.
     */
    fun install(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            _state.value = UpdateState.Failed("קובץ העדכון לא נמצא. נסו להוריד שוב.")
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.updates",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            _state.value = UpdateState.Failed("פתיחת המתקין נכשלה: ${e.message}")
            false
        }
    }

    /** Android 8+ requires per-app permission to install packages. */
    fun canInstallPackages(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun reset() { _state.value = UpdateState.Idle }

    /** Removes APKs left over from earlier updates. */
    fun clearCache() {
        runCatching { updateDir().listFiles()?.forEach { it.delete() } }
    }

    private fun updateDir(): File =
        File(context.cacheDir, "updates").apply { mkdirs() }

    private companion object {
        const val DOWNLOAD_BUFFER = 64 * 1024
        const val PROGRESS_STEP = 128 * 1024L
    }
}
