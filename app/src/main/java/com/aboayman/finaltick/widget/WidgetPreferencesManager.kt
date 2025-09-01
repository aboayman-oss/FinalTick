package com.aboayman.finaltick.widget

import android.content.Context
import com.aboayman.finaltick.SettingsManager

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

    enum class FontChoice {
        ROBOTO,            // Default mapping
        ROBOTO_REGULAR,    // sans-serif
        ROBOTO_MEDIUM,     // sans-serif-medium
        ROBOTO_LIGHT,      // sans-serif-light
        ROBOTO_CONDENSED,  // sans-serif-condensed
        ROBOTO_BLACK,      // sans-serif-black
        ROBOTO_THIN,       // sans-serif-thin
        SERIF,             // serif
        MONOSPACE          // monospace
    }

    // New DataStore-backed APIs via SettingsManager (all suspend)
    suspend fun saveDeadline(context: Context, appWidgetId: Int, deadlineMillis: Long) =
        SettingsManager.setDeadline(context, appWidgetId, deadlineMillis)

    suspend fun getDeadline(context: Context, appWidgetId: Int): Long =
        SettingsManager.getDeadline(context, appWidgetId)

    suspend fun saveFormat(context: Context, appWidgetId: Int, format: String) =
        SettingsManager.setFormat(context, appWidgetId, format)

    suspend fun getFormat(context: Context, appWidgetId: Int): String =
        SettingsManager.getFormat(context, appWidgetId)

    suspend fun deleteWidgetSettings(context: Context, appWidgetId: Int) =
        SettingsManager.deleteWidgetSettings(context, appWidgetId)

    suspend fun savePrefsVersion(context: Context) =
        SettingsManager.setPrefsVersion(context, CURRENT_PREFS_VERSION)

    suspend fun getPrefsVersion(context: Context): Int =
        SettingsManager.getPrefsVersion(context)

    suspend fun saveTitle(context: Context, appWidgetId: Int, title: String) =
        SettingsManager.setTitle(context, appWidgetId, title)

    suspend fun getTitle(context: Context, appWidgetId: Int): String =
        SettingsManager.getTitle(context, appWidgetId)

    suspend fun saveToggle(context: Context, appWidgetId: Int, key: String, value: Boolean) =
        SettingsManager.setToggle(context, appWidgetId, key, value)

    suspend fun getToggle(
        context: Context,
        appWidgetId: Int,
        key: String,
        default: Boolean = true
    ): Boolean = SettingsManager.getToggle(context, appWidgetId, key, default)

    suspend fun saveCreatedAt(context: Context, appWidgetId: Int, createdAt: Long) =
        SettingsManager.setCreatedAt(context, appWidgetId, createdAt)

    suspend fun getCreatedAt(context: Context, appWidgetId: Int): Long =
        SettingsManager.getCreatedAt(context, appWidgetId)

    suspend fun saveColor(context: Context, appWidgetId: Int, key: String, color: Int) =
        SettingsManager.setColor(context, appWidgetId, key, color)

    suspend fun getColor(context: Context, appWidgetId: Int, key: String, defaultColor: Int): Int =
        SettingsManager.getColor(context, appWidgetId, key, defaultColor)

    suspend fun removeKey(context: Context, appWidgetId: Int, key: String) =
        SettingsManager.removeKey(context, appWidgetId, key)

    private fun timeStyleKey(appWidgetId: Int) = "widget_${appWidgetId}_time_style"

    suspend fun saveTimeDisplayStyle(context: Context, appWidgetId: Int, style: TimeDisplayStyle) =
        SettingsManager.setTimeStyle(context, appWidgetId, style)

    suspend fun getTimeDisplayStyle(context: Context, appWidgetId: Int): TimeDisplayStyle =
        SettingsManager.getTimeStyle(context, appWidgetId)

    // Shape style: "rounded" | "pill" | "square"
    suspend fun saveShape(context: Context, appWidgetId: Int, shape: String) =
        SettingsManager.setShape(context, appWidgetId, shape)

    suspend fun getShape(context: Context, appWidgetId: Int): String =
        SettingsManager.getShape(context, appWidgetId)

    private fun fontKeyTitle(appWidgetId: Int) = "font_title"
    private fun fontKeyDate(appWidgetId: Int) = "font_date"
    private fun fontKeyTimer(appWidgetId: Int) = "font_timer"
    private fun fontKeyPercent(appWidgetId: Int) = "font_percentage"

    suspend fun saveTitleFont(context: Context, appWidgetId: Int, font: FontChoice) =
        SettingsManager.setString(context, appWidgetId, fontKeyTitle(appWidgetId), font.name)

    suspend fun saveDateFont(context: Context, appWidgetId: Int, font: FontChoice) =
        SettingsManager.setString(context, appWidgetId, fontKeyDate(appWidgetId), font.name)

    suspend fun saveTimerFont(context: Context, appWidgetId: Int, font: FontChoice) =
        SettingsManager.setString(context, appWidgetId, fontKeyTimer(appWidgetId), font.name)

    suspend fun savePercentFont(context: Context, appWidgetId: Int, font: FontChoice) =
        SettingsManager.setString(context, appWidgetId, fontKeyPercent(appWidgetId), font.name)

    suspend fun getTitleFont(context: Context, appWidgetId: Int): FontChoice =
        runCatching {
            FontChoice.valueOf(
                SettingsManager.getString(
                    context,
                    appWidgetId,
                    fontKeyTitle(appWidgetId),
                    FontChoice.ROBOTO.name
                )
            )
        }.getOrDefault(FontChoice.ROBOTO)

    suspend fun getDateFont(context: Context, appWidgetId: Int): FontChoice =
        runCatching {
            FontChoice.valueOf(
                SettingsManager.getString(
                    context,
                    appWidgetId,
                    fontKeyDate(appWidgetId),
                    FontChoice.ROBOTO.name
                )
            )
        }.getOrDefault(FontChoice.ROBOTO)

    suspend fun getTimerFont(context: Context, appWidgetId: Int): FontChoice =
        runCatching {
            FontChoice.valueOf(
                SettingsManager.getString(
                    context,
                    appWidgetId,
                    fontKeyTimer(appWidgetId),
                    FontChoice.ROBOTO.name
                )
            )
        }.getOrDefault(FontChoice.ROBOTO)

    suspend fun getPercentFont(context: Context, appWidgetId: Int): FontChoice =
        runCatching {
            FontChoice.valueOf(
                SettingsManager.getString(
                    context,
                    appWidgetId,
                    fontKeyPercent(appWidgetId),
                    FontChoice.ROBOTO.name
                )
            )
        }.getOrDefault(FontChoice.ROBOTO)
}
