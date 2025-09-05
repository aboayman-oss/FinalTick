package com.aboayman.finaltick

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel bridging GlobalSettingsManager with the UI.
 * Exposes StateFlows for Compose and handles side-effects (theme + reset logic).
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application get() = getApplication()

    // Exposed settings state
    val theme: StateFlow<GlobalSettingsManager.ThemeMode> =
        GlobalSettingsManager.themeFlow(app).stateIn(
            viewModelScope, SharingStarted.Eagerly, GlobalSettingsManager.ThemeMode.System
        )

    val hapticsEnabled: StateFlow<Boolean> =
        GlobalSettingsManager.hapticsEnabledFlow(app).stateIn(
            viewModelScope, SharingStarted.Eagerly, true
        )

    val timerWeight: StateFlow<Int> =
        GlobalSettingsManager.defaultTimerWeightFlow(app).stateIn(
            viewModelScope, SharingStarted.Eagerly, 600
        )

    val timerColor: StateFlow<Int> =
        GlobalSettingsManager.defaultTimerColorFlow(app).stateIn(
            viewModelScope, SharingStarted.Eagerly, 0xFF2196F3.toInt()
        )

    val timerDynamic: StateFlow<Boolean> =
        GlobalSettingsManager.defaultTimerDynamicFlow(app).stateIn(
            viewModelScope, SharingStarted.Eagerly, false
        )

    val confirmOnExit: StateFlow<Boolean> =
        GlobalSettingsManager.confirmOnExitFlow(app).stateIn(
            viewModelScope, SharingStarted.Eagerly, false
        )

    // One-off state to signal UI reset completion
    private val _resetInProgress = MutableStateFlow(false)
    val resetInProgress: StateFlow<Boolean> = _resetInProgress

    // Update handlers
    fun onThemeSelected(mode: GlobalSettingsManager.ThemeMode) {
        viewModelScope.launch {
            GlobalSettingsManager.setTheme(app, mode)
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    GlobalSettingsManager.ThemeMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    GlobalSettingsManager.ThemeMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
                    GlobalSettingsManager.ThemeMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
                }
            )
        }
    }

    fun onHapticsToggled(enabled: Boolean) {
        viewModelScope.launch { GlobalSettingsManager.setHapticsEnabled(app, enabled) }
    }

    fun onTimerWeightSelected(weight: Int) {
        viewModelScope.launch { GlobalSettingsManager.setDefaultTimerWeight(app, weight) }
    }

    fun onTimerColorSelected(color: Int) {
        viewModelScope.launch { GlobalSettingsManager.setDefaultTimerColor(app, color) }
    }

    fun onTimerDynamicToggled(enabled: Boolean) {
        viewModelScope.launch { GlobalSettingsManager.setDefaultTimerDynamic(app, enabled) }
    }

    fun onConfirmOnExitToggled(enabled: Boolean) {
        viewModelScope.launch { GlobalSettingsManager.setConfirmOnExit(app, enabled) }
    }

    /**
     * Reset the application data.
     * - Clears SharedPreferences (legacy)
     * - Clears both DataStores (global + widget)
     * - Deletes widget SharedPreferences files
     * - Resets all widgets via CountdownWidget
     */
    fun resetApp() {
        viewModelScope.launch(Dispatchers.IO) {
            _resetInProgress.value = true

            // Clear legacy SharedPreferences
            app.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()

            // Clear DataStores
            GlobalSettingsManager.clearAll(app)
            SettingsManager.clearAll(app)

            // Delete widget_*.xml in shared_prefs
            val prefsDir = File(app.filesDir.parent, "shared_prefs")
            prefsDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("widget_") && file.name.endsWith(".xml")) {
                    runCatching { file.delete() }
                }
            }

            // Reset all widgets
            val widgetManager = AppWidgetManager.getInstance(app)
            val component = ComponentName(app, CountdownWidget::class.java)
            val ids = widgetManager.getAppWidgetIds(component)
            ids.forEach { CountdownWidget.resetWidget(app, it) }

            _resetInProgress.value = false
        }
    }
}
