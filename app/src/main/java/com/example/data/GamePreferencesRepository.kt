package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "temple_run_prefs")

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAS_CUSTOM_AVATAR = booleanPreferencesKey("has_custom_avatar")
    }

    private val avatarFile: File
        get() = File(context.filesDir, "player_avatar.jpg")

    val highScoreFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.HIGH_SCORE] ?: 0
    }

    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    suspend fun saveHighScoreIfHigher(score: Int): Boolean {
        var isNewRecord = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.HIGH_SCORE] ?: 0
            if (score > current) {
                prefs[Keys.HIGH_SCORE] = score
                isNewRecord = true
            }
        }
        return isNewRecord
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun saveAvatarFromUri(uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = decodeScaledSquareBitmap(uri) ?: return@withContext null
            FileOutputStream(avatarFile).use { out ->
                decoded.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            context.dataStore.edit { prefs ->
                prefs[Keys.HAS_CUSTOM_AVATAR] = true
            }
            decoded.asImageBitmap()
        }.getOrNull()
    }

    suspend fun loadSavedAvatar(): ImageBitmap? = withContext(Dispatchers.IO) {
        if (!avatarFile.exists()) return@withContext null
        runCatching {
            BitmapFactory.decodeFile(avatarFile.absolutePath)?.asImageBitmap()
        }.getOrNull()
    }

    suspend fun clearSavedAvatar() = withContext(Dispatchers.IO) {
        if (avatarFile.exists()) {
            avatarFile.delete()
        }
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_CUSTOM_AVATAR] = false
        }
    }

    private fun decodeScaledSquareBitmap(uri: Uri): Bitmap? {
        val rawBitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxDim = maxOf(info.size.width, info.size.height)
                if (maxDim > 512) {
                    val scale = 512f / maxDim
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var sampleSize = 1
            while (options.outWidth / sampleSize > 512 || options.outHeight / sampleSize > 512) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null
        }

        val size = min(rawBitmap.width, rawBitmap.height)
        val xOffset = (rawBitmap.width - size) / 2
        val yOffset = (rawBitmap.height - size) / 2
        val cropped = Bitmap.createBitmap(rawBitmap, xOffset, yOffset, size, size)
        return Bitmap.createScaledBitmap(cropped, 256, 256, true)
    }
}
