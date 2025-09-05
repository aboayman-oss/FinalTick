package com.aboayman.finaltick

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Global app-wide settings managed by Jetpack DataStore.
 * This is the single source of truth for non-widget preferences such as theme, haptics, etc.
 */
object GlobalSettingsManager {

    // Backed by its own DataStore file to avoid key collisions with widget settings
    private val Context.globalDataStore by preferencesDataStore(name = "global_settings")

    // Enums for typed settings
    enum class ThemeMode { System, Light, Dark }

    // Preference keys
    private val KEY_THEME = stringPreferencesKey("theme")
    private val KEY_HAPTICS = booleanPreferencesKey("haptics_enabled")
    private val KEY_TIMER_WEIGHT = intPreferencesKey("default_timer_weight")
    private val KEY_TIMER_COLOR = intPreferencesKey("default_timer_color")
    private val KEY_TIMER_DYNAMIC = booleanPreferencesKey("default_timer_dynamic")
    private val KEY_CONFIRM_ON_EXIT = booleanPreferencesKey("confirm_on_exit")

    // Flows for settings
    fun themeFlow(context: Context): Flow<ThemeMode> =
        context.globalDataStore.data.map { prefs ->
            val name = prefs[KEY_THEME] ?: ThemeMode.System.name
            runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.System)
        }

    fun hapticsEnabledFlow(context: Context): Flow<Boolean> =
        context.globalDataStore.data.map { it[KEY_HAPTICS] ?: true }

    fun defaultTimerWeightFlow(context: Context): Flow<Int> =
        context.globalDataStore.data.map { it[KEY_TIMER_WEIGHT] ?: 600 }

    fun defaultTimerColorFlow(context: Context): Flow<Int> =
        context.globalDataStore.data.map {
            it[KEY_TIMER_COLOR] ?: 0xFF2196F3.toInt() /* Fallback */
        }

    fun defaultTimerDynamicFlow(context: Context): Flow<Boolean> =
        context.globalDataStore.data.map { it[KEY_TIMER_DYNAMIC] ?: false }

    fun confirmOnExitFlow(context: Context): Flow<Boolean> =
        context.globalDataStore.data.map { it[KEY_CONFIRM_ON_EXIT] ?: false }

    // Suspend getters (useful for non-Compose callers)
    suspend fun getHapticsEnabled(context: Context) = hapticsEnabledFlow(context).first()
    suspend fun getTheme(context: Context) = themeFlow(context).first()
    suspend fun getTimerWeight(context: Context) = defaultTimerWeightFlow(context).first()
    suspend fun getTimerColor(context: Context) = defaultTimerColorFlow(context).first()
    suspend fun getTimerDynamic(context: Context) = defaultTimerDynamicFlow(context).first()
    suspend fun getConfirmOnExit(context: Context) = confirmOnExitFlow(context).first()

    // Setters
    suspend fun setTheme(context: Context, mode: ThemeMode) {
        context.globalDataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setHapticsEnabled(context: Context, enabled: Boolean) {
        context.globalDataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun setDefaultTimerWeight(context: Context, weight: Int) {
        context.globalDataStore.edit { it[KEY_TIMER_WEIGHT] = weight }
    }

    suspend fun setDefaultTimerColor(context: Context, color: Int) {
        context.globalDataStore.edit { it[KEY_TIMER_COLOR] = color }
    }

    suspend fun setDefaultTimerDynamic(context: Context, dynamic: Boolean) {
        context.globalDataStore.edit { it[KEY_TIMER_DYNAMIC] = dynamic }
    }

    suspend fun setConfirmOnExit(context: Context, enabled: Boolean) {
        context.globalDataStore.edit { it[KEY_CONFIRM_ON_EXIT] = enabled }
    }

    // Clear all global settings (used by Reset App)
    suspend fun clearAll(context: Context) {
        context.globalDataStore.edit { it.clear() }
    }
}
