package com.aboayman.finaltick.widget

import android.content.Context
import com.aboayman.finaltick.SettingsManager
import kotlinx.coroutines.runBlocking

object WidgetPreferencesManager {

    private const val CURRENT_PREFS_VERSION = 1

    enum class TimeDisplayStyle {
        COLON,
        LETTER,
        NATURAL_LANGUAGE,
        VERBOSE_SINGLE,
        COUNTDOWN_WORDS,
        MINIMAL_PROGRESS
    }

    // New DataStore-backed APIs via SettingsManager
    fun saveDeadline(context: Context, appWidgetId: Int, deadlineMillis: Long) = runBlocking {
        SettingsManager.setDeadline(context, appWidgetId, deadlineMillis)
    }

    fun getDeadline(context: Context, appWidgetId: Int): Long = runBlocking {
        SettingsManager.getDeadline(context, appWidgetId)
    }

    fun saveFormat(context: Context, appWidgetId: Int, format: String) = runBlocking {
        SettingsManager.setFormat(context, appWidgetId, format)
    }

    fun getFormat(context: Context, appWidgetId: Int): String = runBlocking {
        SettingsManager.getFormat(context, appWidgetId)
    }

    fun deleteWidgetSettings(context: Context, appWidgetId: Int) = runBlocking {
        SettingsManager.deleteWidgetSettings(context, appWidgetId)
    }

    fun savePrefsVersion(context: Context) = runBlocking {
        SettingsManager.setPrefsVersion(context, CURRENT_PREFS_VERSION)
    }

    fun getPrefsVersion(context: Context): Int = runBlocking {
        SettingsManager.getPrefsVersion(context)
    }

    fun saveTitle(context: Context, appWidgetId: Int, title: String) = runBlocking {
        SettingsManager.setTitle(context, appWidgetId, title)
    }

    fun getTitle(context: Context, appWidgetId: Int): String = runBlocking {
        SettingsManager.getTitle(context, appWidgetId)
    }

    fun saveToggle(context: Context, appWidgetId: Int, key: String, value: Boolean) = runBlocking {
        SettingsManager.setToggle(context, appWidgetId, key, value)
    }

    fun getToggle(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: Boolean = true
    ): Boolean = runBlocking {
        SettingsManager.getToggle(context, appWidgetId, key, default)
    }

    fun saveCreatedAt(context: Context, appWidgetId: Int, createdAt: Long) = runBlocking {
        SettingsManager.setCreatedAt(context, appWidgetId, createdAt)
    }

    fun getCreatedAt(context: Context, appWidgetId: Int): Long = runBlocking {
        SettingsManager.getCreatedAt(context, appWidgetId)
    }

    fun saveColor(context: Context, appWidgetId: Int, key: String, color: Int) = runBlocking {
        SettingsManager.setColor(context, appWidgetId, key, color)
    }

    fun getColor(context: Context, appWidgetId: Int, key: String, defaultColor: Int): Int =
        runBlocking {
            SettingsManager.getColor(context, appWidgetId, key, defaultColor)
    }

    fun removeKey(context: Context, appWidgetId: Int, key: String) = runBlocking {
        SettingsManager.removeKey(context, appWidgetId, key)
    }

    private fun timeStyleKey(appWidgetId: Int) = "widget_${appWidgetId}_time_style"

    fun saveTimeDisplayStyle(context: Context, appWidgetId: Int, style: TimeDisplayStyle) =
        runBlocking {
            SettingsManager.setTimeStyle(context, appWidgetId, style)
        }

    fun getTimeDisplayStyle(context: Context, appWidgetId: Int): TimeDisplayStyle = runBlocking {
        SettingsManager.getTimeStyle(context, appWidgetId)
    }

    // Shape style: "rounded" | "pill" | "square"
    fun saveShape(context: Context, appWidgetId: Int, shape: String) = runBlocking {
        SettingsManager.setShape(context, appWidgetId, shape)
    }

    fun getShape(context: Context, appWidgetId: Int): String = runBlocking {
        SettingsManager.getShape(context, appWidgetId)
    }
}
