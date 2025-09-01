package com.aboayman.finaltick

import android.content.Context
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "widget_prefs"))
    }
)

object SettingsManager {

    // Key builders (names kept compatible with legacy SharedPreferences)
    private fun keyDeadline(appWidgetId: Int) = longPreferencesKey("widget_${appWidgetId}_deadline")
    private fun keyFormat(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_format")
    private fun keyTitle(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_title")
    private fun keyToggle(appWidgetId: Int, key: String) =
        booleanPreferencesKey("widget_${appWidgetId}_${key}")

    private fun keyCreatedAt(appWidgetId: Int) =
        longPreferencesKey("widget_${appWidgetId}_createdAt")

    private fun keyColor(appWidgetId: Int, key: String) =
        intPreferencesKey("widget_${appWidgetId}_${key}")

    private fun keyPrefsVersion() = intPreferencesKey("widget_prefs_version")
    private fun keyTimeStyle(appWidgetId: Int) =
        stringPreferencesKey("widget_${appWidgetId}_time_style")

    private fun keyShape(appWidgetId: Int) = stringPreferencesKey("widget_${appWidgetId}_shape")
    private fun keyProgressStyle(appWidgetId: Int) =
        stringPreferencesKey("widget_${appWidgetId}_progress_style")
    private fun keyString(appWidgetId: Int, key: String) =
        stringPreferencesKey("widget_${appWidgetId}_${key}")

    // Flows
    fun deadlineFlow(context: Context, appWidgetId: Int): Flow<Long> =
        context.dataStore.data.map { it[keyDeadline(appWidgetId)] ?: -1L }

    fun titleFlow(context: Context, appWidgetId: Int): Flow<String> =
        context.dataStore.data.map { it[keyTitle(appWidgetId)] ?: "(Title not set)" }

    fun formatFlow(context: Context, appWidgetId: Int): Flow<String> =
        context.dataStore.data.map { it[keyFormat(appWidgetId)] ?: "DD:HH:MM:SS" }

    fun createdAtFlow(context: Context, appWidgetId: Int): Flow<Long> =
        context.dataStore.data.map { it[keyCreatedAt(appWidgetId)] ?: System.currentTimeMillis() }

    fun toggleFlow(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: Boolean = true
    ): Flow<Boolean> =
        context.dataStore.data.map { it[keyToggle(appWidgetId, key)] ?: default }

    fun colorFlow(context: Context, appWidgetId: Int, key: String, defaultColor: Int): Flow<Int> =
        context.dataStore.data.map { it[keyColor(appWidgetId, key)] ?: defaultColor }

    fun prefsVersionFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { it[keyPrefsVersion()] ?: 1 }

    fun timeStyleFlow(context: Context, appWidgetId: Int): Flow<TimeDisplayStyle> =
        context.dataStore.data.map { prefs ->
            val name = prefs[keyTimeStyle(appWidgetId)] ?: TimeDisplayStyle.COLON.name
            runCatching { TimeDisplayStyle.valueOf(name) }.getOrDefault(TimeDisplayStyle.COLON)
        }

    fun shapeFlow(context: Context, appWidgetId: Int): Flow<String> =
        context.dataStore.data.map { it[keyShape(appWidgetId)] ?: "rounded" }

    fun progressStyleFlow(context: Context, appWidgetId: Int): Flow<String> =
        context.dataStore.data.map { it[keyProgressStyle(appWidgetId)] ?: "solid" }

    fun stringFlow(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: String
    ): Flow<String> =
        context.dataStore.data.map { it[keyString(appWidgetId, key)] ?: default }

    // Suspend getters
    suspend fun getDeadline(context: Context, appWidgetId: Int) =
        deadlineFlow(context, appWidgetId).first()

    suspend fun getTitle(context: Context, appWidgetId: Int) =
        titleFlow(context, appWidgetId).first()

    suspend fun getFormat(context: Context, appWidgetId: Int) =
        formatFlow(context, appWidgetId).first()

    suspend fun getCreatedAt(context: Context, appWidgetId: Int) =
        createdAtFlow(context, appWidgetId).first()

    suspend fun getToggle(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: Boolean = true
    ) =
        toggleFlow(context, appWidgetId, key, default).first()

    suspend fun getColor(context: Context, appWidgetId: Int, key: String, defaultColor: Int) =
        colorFlow(context, appWidgetId, key, defaultColor).first()

    suspend fun getPrefsVersion(context: Context) = prefsVersionFlow(context).first()
    suspend fun getTimeStyle(context: Context, appWidgetId: Int) =
        timeStyleFlow(context, appWidgetId).first()

    suspend fun getShape(context: Context, appWidgetId: Int) =
        shapeFlow(context, appWidgetId).first()

    suspend fun getProgressStyle(context: Context, appWidgetId: Int) =
        progressStyleFlow(context, appWidgetId).first()

    suspend fun getString(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: String
    ) = stringFlow(context, appWidgetId, key, default).first()

    // Suspend setters
    suspend fun setDeadline(context: Context, appWidgetId: Int, millis: Long) {
        context.dataStore.edit { it[keyDeadline(appWidgetId)] = millis }
    }

    suspend fun setTitle(context: Context, appWidgetId: Int, title: String) {
        context.dataStore.edit { it[keyTitle(appWidgetId)] = title }
    }

    suspend fun setFormat(context: Context, appWidgetId: Int, format: String) {
        context.dataStore.edit { it[keyFormat(appWidgetId)] = format }
    }

    suspend fun setCreatedAt(context: Context, appWidgetId: Int, createdAt: Long) {
        context.dataStore.edit { it[keyCreatedAt(appWidgetId)] = createdAt }
    }

    suspend fun setToggle(context: Context, appWidgetId: Int, key: String, value: Boolean) {
        context.dataStore.edit { it[keyToggle(appWidgetId, key)] = value }
    }

    suspend fun setColor(context: Context, appWidgetId: Int, key: String, color: Int) {
        context.dataStore.edit { it[keyColor(appWidgetId, key)] = color }
    }

    suspend fun setPrefsVersion(context: Context, version: Int) {
        context.dataStore.edit { it[keyPrefsVersion()] = version }
    }

    suspend fun setTimeStyle(context: Context, appWidgetId: Int, style: TimeDisplayStyle) {
        context.dataStore.edit { it[keyTimeStyle(appWidgetId)] = style.name }
    }

    suspend fun setShape(context: Context, appWidgetId: Int, shape: String) {
        context.dataStore.edit { it[keyShape(appWidgetId)] = shape }
    }

    suspend fun setProgressStyle(context: Context, appWidgetId: Int, style: String) {
        context.dataStore.edit { it[keyProgressStyle(appWidgetId)] = style }
    }

    suspend fun setString(
        context: Context,
        appWidgetId: Int,
        key: String,
        value: String
    ) {
        context.dataStore.edit { it[keyString(appWidgetId, key)] = value }
    }

    // Removal helpers (try all supported types with same name)
    suspend fun removeKey(context: Context, appWidgetId: Int, key: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey("widget_${appWidgetId}_${key}"))
            prefs.remove(intPreferencesKey("widget_${appWidgetId}_${key}"))
            prefs.remove(longPreferencesKey("widget_${appWidgetId}_${key}"))
            prefs.remove(booleanPreferencesKey("widget_${appWidgetId}_${key}"))
        }
    }

    suspend fun deleteWidgetSettings(context: Context, appWidgetId: Int) {
        // Best-effort cleanup of known keys for an appWidgetId
        context.dataStore.edit { prefs ->
            prefs.remove(keyDeadline(appWidgetId))
            prefs.remove(keyFormat(appWidgetId))
            prefs.remove(keyTitle(appWidgetId))
            prefs.remove(keyCreatedAt(appWidgetId))
            // Feature-specific keys are left to callers via removeKey
        }
    }

    // Global clear (used for app reset)
    suspend fun clearAll(context: Context) {
        context.dataStore.edit { it.clear() }
    }
}
