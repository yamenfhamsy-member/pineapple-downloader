package com.pineapple.downloader.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pineapple.downloader.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * نسخة DataStore وحيدة مشتركة للإعدادات — يجب ألا تُنشأ نسخة ثانية
 * لنفس الملف في أي مكان آخر (تلف محتمل عند الكتابة المتزامنة).
 */
private val Context.prefs by preferencesDataStore(name = "settings")
private val THEME_KEY = stringPreferencesKey("theme_mode")

object SettingsStore {

    fun themeMode(context: Context): Flow<ThemeMode> =
        context.prefs.data.map { prefs ->
            runCatching { ThemeMode.valueOf(prefs[THEME_KEY] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM)
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.prefs.edit { it[THEME_KEY] = mode.name }
    }
}
