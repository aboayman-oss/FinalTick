package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.widget.WidgetPreferencesManager.FontChoice
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State holder for the redesigned Customize Widget screen.
 * - Loads/saves to DataStore via WidgetPreferencesManager/SettingsManager
 * - Debounces actual RemoteViews widget refreshes to avoid excessive updates
 * - Keeps a single source of truth for the UI (Compose preview updates instantly)
 */
class CustomizeWidgetViewModel(
    private val appContext: Context,
    private val appWidgetId: Int
) : ViewModel() {

    data class UnitToggles(
        val days: Boolean = true,
        val hours: Boolean = true,
        val minutes: Boolean = true,
        val seconds: Boolean = true
    ) {
        fun ensureAtLeastOne(): UnitToggles {
            if (days || hours || minutes || seconds) return this
            return copy(seconds = true)
        }
    }

    enum class ProgressStyle(val key: String) { Solid("solid"), Dashed("dashed"), Gradient("gradient") }
    enum class ShapeStyle(val key: String) { Rounded("rounded"), Pill("pill"), Square("square") }
    enum class FontFamilyOption { Sans, Serif, Mono }
    enum class FontWeightOption { Thin, Light, Regular, Medium, Black }
    enum class SizePreset(val cols: Int, val rows: Int) { C1x1(1, 1), C2x1(2, 1), C4x2(4, 2) }

    data class UiState(
        val isLoading: Boolean = true,
        val progressStyle: ProgressStyle = ProgressStyle.Solid,
        val shape: ShapeStyle = ShapeStyle.Rounded,
        val timeStyle: TimeDisplayStyle = TimeDisplayStyle.COLON,
        val family: FontFamilyOption = FontFamilyOption.Sans,
        val weight: FontWeightOption = FontWeightOption.Regular,
        val showTitle: Boolean = true,
        val showDate: Boolean = true,
        val showTimer: Boolean = true,
        val showProgress: Boolean = true,
        val showPercent: Boolean = true,
        val showIcon: Boolean = true,
        val units: UnitToggles = UnitToggles(),
        val textColor: Int? = null,       // null means use default
        val progressColor: Int? = null,   // stored but currently not used by RemoteViews
        val backgroundColor: Int? = null, // ARGB base tint. If null -> default surface
        val backgroundAlpha: Int = 0xCC,  // 0..255
        val sizePreset: SizePreset = SizePreset.C2x1
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private var updateJob: Job? = null

    init {
        viewModelScope.launch { loadFromStore() }
    }

    private suspend fun loadFromStore() {
        // Load persisted values with safe fallbacks
        val styleKey = WidgetPreferencesManager.getProgressStyle(appContext, appWidgetId)
        val shapeKey = WidgetPreferencesManager.getShape(appContext, appWidgetId)
        val timeStyle = WidgetPreferencesManager.getTimeDisplayStyle(appContext, appWidgetId)

        // Fonts: unify UI but persist to all relevant keys
        val titleFont = WidgetPreferencesManager.getTitleFont(appContext, appWidgetId)
        val family = when (titleFont) {
            FontChoice.MONOSPACE -> FontFamilyOption.Mono
            FontChoice.SERIF -> FontFamilyOption.Serif
            FontChoice.ROBOTO,
            FontChoice.ROBOTO_REGULAR,
            FontChoice.ROBOTO_MEDIUM,
            FontChoice.ROBOTO_LIGHT,
            FontChoice.ROBOTO_CONDENSED,
            FontChoice.ROBOTO_BLACK,
            FontChoice.ROBOTO_THIN -> FontFamilyOption.Sans
        }
        val weight = when (titleFont) {
            FontChoice.ROBOTO_THIN -> FontWeightOption.Thin
            FontChoice.ROBOTO_LIGHT -> FontWeightOption.Light
            FontChoice.ROBOTO_MEDIUM,
            FontChoice.ROBOTO -> FontWeightOption.Medium

            FontChoice.ROBOTO_BLACK -> FontWeightOption.Black
            else -> FontWeightOption.Regular
        }

        // Visibility defaults from current widget size
        val options = AppWidgetManager.getInstance(appContext).getAppWidgetOptions(appWidgetId)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH)
        val wDp = ((minW + maxW) / 2f)
        val hDp = ((minH + maxH) / 2f)
        val cfg = WidgetLayoutManager.getAdaptiveLayoutConfig(wDp, hDp)

        val showTitle =
            WidgetPreferencesManager.getToggle(appContext, appWidgetId, "show_title", cfg.showTitle)
        val showDate =
            WidgetPreferencesManager.getToggle(appContext, appWidgetId, "show_date", cfg.showDate)
        val showTimer =
            WidgetPreferencesManager.getToggle(appContext, appWidgetId, "show_timer", cfg.showTimer)
        val showProgress = WidgetPreferencesManager.getToggle(
            appContext,
            appWidgetId,
            "show_progress",
            cfg.showProgress
        )
        val showPercent = WidgetPreferencesManager.getToggle(
            appContext,
            appWidgetId,
            "show_percentage",
            cfg.showPercent
        )
        val showIcon =
            WidgetPreferencesManager.getToggle(appContext, appWidgetId, "show_icon", cfg.showIcon)

        val units = UnitToggles(
            days = WidgetPreferencesManager.getToggle(appContext, appWidgetId, "show_days", true),
            hours = WidgetPreferencesManager.getToggle(appContext, appWidgetId, "show_hours", true),
            minutes = WidgetPreferencesManager.getToggle(
                appContext,
                appWidgetId,
                "show_minutes",
                true
            ),
            seconds = WidgetPreferencesManager.getToggle(
                appContext,
                appWidgetId,
                "show_seconds",
                true
            )
        ).ensureAtLeastOne()

        val textColor =
            WidgetPreferencesManager.getColor(appContext, appWidgetId, "color_title", Int.MIN_VALUE)
                .let { if (it == Int.MIN_VALUE) null else it }
        val dateColor =
            WidgetPreferencesManager.getColor(appContext, appWidgetId, "color_date", Int.MIN_VALUE)
                .let { if (it == Int.MIN_VALUE) null else it }
        val timerColor =
            WidgetPreferencesManager.getColor(appContext, appWidgetId, "color_timer", Int.MIN_VALUE)
                .let { if (it == Int.MIN_VALUE) null else it }
        val percentColor = WidgetPreferencesManager.getColor(
            appContext,
            appWidgetId,
            "color_percentage",
            Int.MIN_VALUE
        )
            .let { if (it == Int.MIN_VALUE) null else it }
        val bgColor = WidgetPreferencesManager.getColor(
            appContext,
            appWidgetId,
            "color_background",
            Int.MIN_VALUE
        )
            .let { if (it == Int.MIN_VALUE) null else it }
        val bgAlpha =
            WidgetPreferencesManager.getColor(appContext, appWidgetId, "background_alpha", 0xCC)

        _uiState.update {
            it.copy(
                isLoading = false,
                progressStyle = when (styleKey) {
                    "dashed" -> ProgressStyle.Dashed; "gradient" -> ProgressStyle.Gradient; else -> ProgressStyle.Solid
                },
                shape = when (shapeKey) {
                    "pill" -> ShapeStyle.Pill; "square" -> ShapeStyle.Square; else -> ShapeStyle.Rounded
                },
                timeStyle = timeStyle,
                family = family,
                weight = weight,
                showTitle = showTitle,
                showDate = showDate,
                showTimer = showTimer,
                showProgress = showProgress,
                showPercent = showPercent,
                showIcon = showIcon,
                units = units,
                // If any text color keys differ, prefer timer color; otherwise title
                textColor = timerColor ?: textColor ?: dateColor,
                progressColor = percentColor,
                backgroundColor = bgColor,
                backgroundAlpha = bgAlpha
            )
        }
    }

    // --- Intent: mutate state, persist immediately, and debounce widget refresh ---
    private fun scheduleWidgetRefresh() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            delay(400)
            CountdownWidget.forceUpdateWidget(appContext, appWidgetId)
        }
    }

    fun onSizePreset(preset: SizePreset) {
        _uiState.update { it.copy(sizePreset = preset) }
    }

    fun onProgressStyleSelected(style: ProgressStyle) {
        _uiState.update { it.copy(progressStyle = style) }
        viewModelScope.launch {
            WidgetPreferencesManager.saveProgressStyle(appContext, appWidgetId, style.key)
            scheduleWidgetRefresh()
        }
    }

    fun onShapeSelected(shape: ShapeStyle) {
        _uiState.update { it.copy(shape = shape) }
        viewModelScope.launch {
            WidgetPreferencesManager.saveShape(appContext, appWidgetId, shape.key)
            scheduleWidgetRefresh()
        }
    }

    fun onFamilySelected(f: FontFamilyOption) {
        _uiState.update { it.copy(family = f) }
        persistFonts()
    }

    fun onWeightSelected(w: FontWeightOption) {
        _uiState.update { it.copy(weight = w) }
        persistFonts()
    }

    private fun persistFonts() {
        val state = _uiState.value
        val choice = computeFontChoice(state.family, state.weight)
        viewModelScope.launch {
            WidgetPreferencesManager.saveTitleFont(appContext, appWidgetId, choice)
            WidgetPreferencesManager.saveDateFont(appContext, appWidgetId, choice)
            WidgetPreferencesManager.saveTimerFont(appContext, appWidgetId, choice)
            WidgetPreferencesManager.savePercentFont(appContext, appWidgetId, choice)
            scheduleWidgetRefresh()
        }
    }

    fun onTimeStyleSelected(style: TimeDisplayStyle) {
        // If single-unit styles are selected, enforce just one unit (preserve precedence)
        val singleUnit =
            style == TimeDisplayStyle.VERBOSE_SINGLE || style == TimeDisplayStyle.COUNTDOWN_WORDS
        _uiState.update { cur ->
            val newUnits = if (!singleUnit) cur.units else {
                // Choose first currently enabled unit, fallback to seconds
                when {
                    cur.units.days -> UnitToggles(
                        days = true,
                        hours = false,
                        minutes = false,
                        seconds = false
                    )

                    cur.units.hours -> UnitToggles(
                        days = false,
                        hours = true,
                        minutes = false,
                        seconds = false
                    )

                    cur.units.minutes -> UnitToggles(
                        days = false,
                        hours = false,
                        minutes = true,
                        seconds = false
                    )

                    else -> UnitToggles(
                        days = false,
                        hours = false,
                        minutes = false,
                        seconds = true
                    )
                }
            }
            cur.copy(timeStyle = style, units = newUnits)
        }
        viewModelScope.launch {
            WidgetPreferencesManager.saveTimeDisplayStyle(appContext, appWidgetId, style)
            persistUnitsToStore(_uiState.value.units)
            scheduleWidgetRefresh()
        }
    }

    fun onToggleChanged(key: String, value: Boolean) {
        when (key) {
            "show_title" -> _uiState.update { it.copy(showTitle = value) }
            "show_date" -> _uiState.update { it.copy(showDate = value) }
            "show_timer" -> _uiState.update { it.copy(showTimer = value) }
            "show_progress" -> _uiState.update { it.copy(showProgress = value) }
            "show_percentage" -> _uiState.update { it.copy(showPercent = value) }
            "show_icon" -> _uiState.update { it.copy(showIcon = value) }
        }
        viewModelScope.launch {
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, key, value)
            scheduleWidgetRefresh()
        }
    }

    fun onUnitChanged(unit: String, checked: Boolean) {
        val singleUnit = _uiState.value.timeStyle == TimeDisplayStyle.VERBOSE_SINGLE ||
                _uiState.value.timeStyle == TimeDisplayStyle.COUNTDOWN_WORDS
        _uiState.update { cur ->
            var u = cur.units
            u = when (unit) {
                "days" -> u.copy(days = checked)
                "hours" -> u.copy(hours = checked)
                "minutes" -> u.copy(minutes = checked)
                else -> u.copy(seconds = checked)
            }
            if (singleUnit) {
                // enforce radio behavior
                u = when (unit) {
                    "days" -> UnitToggles(
                        days = true,
                        hours = false,
                        minutes = false,
                        seconds = false
                    )

                    "hours" -> UnitToggles(
                        days = false,
                        hours = true,
                        minutes = false,
                        seconds = false
                    )

                    "minutes" -> UnitToggles(
                        days = false,
                        hours = false,
                        minutes = true,
                        seconds = false
                    )

                    else -> UnitToggles(
                        days = false,
                        hours = false,
                        minutes = false,
                        seconds = true
                    )
                }
            }
            cur.copy(units = u.ensureAtLeastOne())
        }
        viewModelScope.launch {
            persistUnitsToStore(_uiState.value.units)
            scheduleWidgetRefresh()
        }
    }

    private suspend fun persistUnitsToStore(u: UnitToggles) {
        WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_days", u.days)
        WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_hours", u.hours)
        WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_minutes", u.minutes)
        WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_seconds", u.seconds)
    }

    fun onTextColorChanged(argb: Int?) {
        _uiState.update { it.copy(textColor = argb) }
        viewModelScope.launch {
            if (argb == null) {
                // Remove keys to fallback to defaults
                WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_title")
                WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_timer")
                WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_date")
            } else {
                WidgetPreferencesManager.saveColor(appContext, appWidgetId, "color_title", argb)
                WidgetPreferencesManager.saveColor(appContext, appWidgetId, "color_timer", argb)
                WidgetPreferencesManager.saveColor(appContext, appWidgetId, "color_date", argb)
            }
            scheduleWidgetRefresh()
        }
    }

    fun onProgressColorChanged(argb: Int?) {
        _uiState.update { it.copy(progressColor = argb) }
        viewModelScope.launch {
            if (argb == null) {
                WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_percentage")
            } else {
                WidgetPreferencesManager.saveColor(
                    appContext,
                    appWidgetId,
                    "color_percentage",
                    argb
                )
            }
            scheduleWidgetRefresh()
        }
    }

    fun onBackgroundColorChanged(argb: Int?) {
        _uiState.update { it.copy(backgroundColor = argb) }
        viewModelScope.launch {
            if (argb == null) {
                WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_background")
            } else {
                WidgetPreferencesManager.saveColor(
                    appContext,
                    appWidgetId,
                    "color_background",
                    argb
                )
            }
            scheduleWidgetRefresh()
        }
    }

    fun onBackgroundAlphaChanged(alpha: Int) {
        _uiState.update { it.copy(backgroundAlpha = alpha.coerceIn(0, 255)) }
        viewModelScope.launch {
            WidgetPreferencesManager.saveColor(
                appContext,
                appWidgetId,
                "background_alpha",
                _uiState.value.backgroundAlpha
            )
            scheduleWidgetRefresh()
        }
    }

    fun resetAll() {
        _uiState.update { UiState(isLoading = false) }
        viewModelScope.launch {
            // Clear per-key overrides and set primitive defaults
            // Time/shape/style
            WidgetPreferencesManager.saveProgressStyle(
                appContext,
                appWidgetId,
                ProgressStyle.Solid.key
            )
            WidgetPreferencesManager.saveShape(appContext, appWidgetId, ShapeStyle.Rounded.key)
            WidgetPreferencesManager.saveTimeDisplayStyle(
                appContext,
                appWidgetId,
                TimeDisplayStyle.COLON
            )
            // Visibility toggles: enable all (safe defaults)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_title", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_date", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_timer", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_progress", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_percentage", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_icon", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_days", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_hours", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_minutes", true)
            WidgetPreferencesManager.saveToggle(appContext, appWidgetId, "show_seconds", true)
            // Colors: remove to fallback, except background alpha to 0xCC
            WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_title")
            WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_date")
            WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_timer")
            WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_percentage")
            WidgetPreferencesManager.removeKey(appContext, appWidgetId, "color_background")
            WidgetPreferencesManager.saveColor(appContext, appWidgetId, "background_alpha", 0xCC)

            // Fonts: default to Sans/Regular (ROBOTO_REGULAR)
            val choice = computeFontChoice(FontFamilyOption.Sans, FontWeightOption.Regular)
            WidgetPreferencesManager.saveTitleFont(appContext, appWidgetId, choice)
            WidgetPreferencesManager.saveDateFont(appContext, appWidgetId, choice)
            WidgetPreferencesManager.saveTimerFont(appContext, appWidgetId, choice)
            WidgetPreferencesManager.savePercentFont(appContext, appWidgetId, choice)

            scheduleWidgetRefresh()
        }
    }

    fun exportStyle(): String {
        val s = _uiState.value
        // Simple JSON (strings/ints only) for optional share/backup
        return "{" + listOf(
            "\"progressStyle\":\"${s.progressStyle.key}\"",
            "\"shape\":\"${s.shape.key}\"",
            "\"timeStyle\":\"${s.timeStyle.name}\"",
            "\"family\":\"${s.family.name}\"",
            "\"weight\":\"${s.weight.name}\"",
            "\"showTitle\":${s.showTitle}",
            "\"showDate\":${s.showDate}",
            "\"showTimer\":${s.showTimer}",
            "\"showProgress\":${s.showProgress}",
            "\"showPercent\":${s.showPercent}",
            "\"showIcon\":${s.showIcon}",
            "\"units\":{\"days\":${s.units.days},\"hours\":${s.units.hours},\"minutes\":${s.units.minutes},\"seconds\":${s.units.seconds}}",
            s.textColor?.let { "\"textColor\":$it" },
            s.progressColor?.let { "\"progressColor\":$it" },
            s.backgroundColor?.let { "\"backgroundColor\":$it" },
            "\"backgroundAlpha\":${s.backgroundAlpha}"
        ).filterNotNull().joinToString(",") + "}"
    }

    fun importStyleOrNull(json: String): Boolean {
        return runCatching {
            val obj = org.json.JSONObject(json)
            obj.optString("progressStyle", null)?.let {
                onProgressStyleSelected(
                    when (it) {
                        "dashed" -> ProgressStyle.Dashed
                        "gradient" -> ProgressStyle.Gradient
                        else -> ProgressStyle.Solid
                    }
                )
            }
            obj.optString("shape", null)?.let {
                onShapeSelected(
                    when (it) {
                        "pill" -> ShapeStyle.Pill
                        "square" -> ShapeStyle.Square
                        else -> ShapeStyle.Rounded
                    }
                )
            }
            obj.optString("timeStyle", null)?.let { name ->
                onTimeStyleSelected(
                    runCatching { TimeDisplayStyle.valueOf(name) }.getOrDefault(
                        TimeDisplayStyle.COLON
                    )
                )
            }
            obj.optString("family", null)?.let { n ->
                onFamilySelected(
                    runCatching { FontFamilyOption.valueOf(n) }.getOrDefault(
                        FontFamilyOption.Sans
                    )
                )
            }
            obj.optString("weight", null)?.let { n ->
                onWeightSelected(
                    runCatching { FontWeightOption.valueOf(n) }.getOrDefault(
                        FontWeightOption.Regular
                    )
                )
            }

            val units = obj.optJSONObject("units")
            if (units != null) {
                val u = UnitToggles(
                    days = units.optBoolean("days", _uiState.value.units.days),
                    hours = units.optBoolean("hours", _uiState.value.units.hours),
                    minutes = units.optBoolean("minutes", _uiState.value.units.minutes),
                    seconds = units.optBoolean("seconds", _uiState.value.units.seconds)
                )
                _uiState.update { it.copy(units = u.ensureAtLeastOne()) }
                viewModelScope.launch { persistUnitsToStore(_uiState.value.units) }
            }

            if (obj.has("textColor") && !obj.isNull("textColor")) onTextColorChanged(obj.optInt("textColor")) else onTextColorChanged(
                null
            )
            if (obj.has("progressColor") && !obj.isNull("progressColor")) onProgressColorChanged(
                obj.optInt(
                    "progressColor"
                )
            ) else onProgressColorChanged(null)
            if (obj.has("backgroundColor") && !obj.isNull("backgroundColor")) onBackgroundColorChanged(
                obj.optInt("backgroundColor")
            ) else onBackgroundColorChanged(null)
            if (obj.has("backgroundAlpha")) onBackgroundAlphaChanged(
                obj.optInt(
                    "backgroundAlpha",
                    _uiState.value.backgroundAlpha
                )
            )
            true
        }.getOrElse { false }
    }

    fun forceRefreshNow() {
        viewModelScope.launch { CountdownWidget.forceUpdateWidget(appContext, appWidgetId) }
    }

    private fun computeFontChoice(f: FontFamilyOption, w: FontWeightOption): FontChoice {
        return when (f) {
            FontFamilyOption.Serif -> FontChoice.SERIF
            FontFamilyOption.Mono -> FontChoice.MONOSPACE
            FontFamilyOption.Sans -> when (w) {
                FontWeightOption.Thin -> FontChoice.ROBOTO_THIN
                FontWeightOption.Light -> FontChoice.ROBOTO_LIGHT
                FontWeightOption.Medium -> FontChoice.ROBOTO_MEDIUM
                FontWeightOption.Black -> FontChoice.ROBOTO_BLACK
                FontWeightOption.Regular -> FontChoice.ROBOTO_REGULAR
            }
        }
    }

    class Factory(
        private val appContext: Context,
        private val appWidgetId: Int
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CustomizeWidgetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return CustomizeWidgetViewModel(appContext, appWidgetId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
