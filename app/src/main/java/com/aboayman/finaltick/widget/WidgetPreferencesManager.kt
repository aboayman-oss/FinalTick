package com.aboayman.finaltick.widget

import android.content.Context
import android.content.SharedPreferences

object WidgetPreferencesManager {

    private const val PREFS_NAME = "widget_prefs"
    private const val PREF_VERSION_KEY = "widget_prefs_version"
    private const val CURRENT_PREFS_VERSION = 1

    enum class TimeDisplayStyle {
        COLON,
        LETTER,
        NATURAL_LANGUAGE,
        VERBOSE_SINGLE,
        COUNTDOWN_WORDS,
        MINIMAL_PROGRESS
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveDeadline(context: Context, appWidgetId: Int, deadlineMillis: Long) {
        getPrefs(context).edit()
            .putLong("widget_${appWidgetId}_deadline", deadlineMillis)
            .apply()
    }

    fun getDeadline(context: Context, appWidgetId: Int): Long {
        return getPrefs(context).getLong("widget_${appWidgetId}_deadline", -1L)
    }

    fun saveFormat(context: Context, appWidgetId: Int, format: String) {
        getPrefs(context).edit()
            .putString("widget_${appWidgetId}_format", format)
            .apply()
    }

    fun getFormat(context: Context, appWidgetId: Int): String {
        return getPrefs(context).getString("widget_${appWidgetId}_format", "DD:HH:MM:SS")
            ?: "DD:HH:MM:SS"
    }

    fun deleteWidgetSettings(context: Context, appWidgetId: Int) {
        getPrefs(context).edit()
            .remove("widget_${appWidgetId}_deadline")
            .remove("widget_${appWidgetId}_format")
            .apply()
    }

    fun savePrefsVersion(context: Context) {
        getPrefs(context).edit()
            .putInt(PREF_VERSION_KEY, CURRENT_PREFS_VERSION)
            .apply()
    }

    fun getPrefsVersion(context: Context): Int {
        return getPrefs(context).getInt(PREF_VERSION_KEY, 1)
    }

    fun saveTitle(context: Context, appWidgetId: Int, title: String) {
        getPrefs(context).edit()
            .putString("widget_${appWidgetId}_title", title)
            .apply()
    }

    fun getTitle(context: Context, appWidgetId: Int): String {
        return getPrefs(context).getString("widget_${appWidgetId}_title", "(Title not set)")
            ?: "(Title not set)"
    }

    fun saveToggle(context: Context, appWidgetId: Int, key: String, value: Boolean) {
        getPrefs(context).edit()
            .putBoolean("widget_${appWidgetId}_${key}", value)
            .apply()
    }

    fun getToggle(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: Boolean = true
    ): Boolean {
        return getPrefs(context).getBoolean("widget_${appWidgetId}_${key}", default)
    }
    fun saveCreatedAt(context: Context, appWidgetId: Int, createdAt: Long) {
        getPrefs(context).edit()
            .putLong("widget_${appWidgetId}_createdAt", createdAt)
            .apply()
    }

    fun getCreatedAt(context: Context, appWidgetId: Int): Long {
        return getPrefs(context).getLong(
            "widget_${appWidgetId}_createdAt",
            System.currentTimeMillis()
        )
    }
    fun saveColor(context: Context, appWidgetId: Int, key: String, color: Int) {
        getPrefs(context).edit()
            .putInt("widget_${appWidgetId}_$key", color)
            .apply()
    }

    fun getColor(context: Context, appWidgetId: Int, key: String, defaultColor: Int): Int {
        return getPrefs(context).getInt("widget_${appWidgetId}_$key", defaultColor)
    }

    fun removeKey(context: Context, appWidgetId: Int, key: String) {
        getPrefs(context).edit()
            .remove("widget_${appWidgetId}_$key")
            .apply()
    }

    private fun timeStyleKey(appWidgetId: Int) = "widget_${appWidgetId}_time_style"

    fun saveTimeDisplayStyle(context: Context, appWidgetId: Int, style: TimeDisplayStyle) {
        getPrefs(context).edit()
            .putString(timeStyleKey(appWidgetId), style.name)
            .apply()
    }

    fun getTimeDisplayStyle(context: Context, appWidgetId: Int): TimeDisplayStyle {
        val name =
            getPrefs(context).getString(timeStyleKey(appWidgetId), TimeDisplayStyle.COLON.name)
        return try {
            TimeDisplayStyle.valueOf(name ?: TimeDisplayStyle.COLON.name)
        } catch (e: IllegalArgumentException) {
            TimeDisplayStyle.COLON
        }
    }
}
