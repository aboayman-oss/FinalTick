package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.Haptics
import com.aboayman.finaltick.R
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CustomizeWidgetActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var toggleManager: WidgetToggleManager
    private lateinit var colorManager: WidgetColorManager
    private lateinit var appearanceManager: WidgetAppearanceManager
    private lateinit var previewController: FakeWidgetPreviewController

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customize_widget)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        previewController = FakeWidgetPreviewController(this)
        toggleManager = WidgetToggleManager(this, appWidgetId, previewController)
        colorManager = WidgetColorManager(this, appWidgetId, previewController)
        appearanceManager = WidgetAppearanceManager(this, appWidgetId, previewController)

        toggleManager.init()
        colorManager.init()
        appearanceManager.init()

        observePreview()

        // Info tooltips
        findViewById<android.widget.ImageView>(R.id.ivInfoShape)?.setOnClickListener {
            Haptics.perform(this, it)
            MaterialAlertDialogBuilder(this)
                .setTitle("Shape")
                .setMessage("Choose how rounded the widget background is: Rounded (28dp corners), Pill (fully rounded), or Square.")
                .setPositiveButton("Got it") { d, _ -> d.dismiss() }
                .show()
        }
        findViewById<android.widget.ImageView>(R.id.ivInfoOpacity)?.setOnClickListener {
            Haptics.perform(this, it)
            MaterialAlertDialogBuilder(this)
                .setTitle("Background Opacity")
                .setMessage("Adjust the transparency of the widget background. 0% is fully transparent, 100% is fully opaque.")
                .setPositiveButton("Got it") { d, _ -> d.dismiss() }
                .show()
        }

        val savedStyle = WidgetPreferencesManager.getTimeDisplayStyle(this, appWidgetId)

        val radioButton = when (savedStyle) {
            TimeDisplayStyle.COLON -> findViewById<androidx.appcompat.widget.AppCompatRadioButton>(R.id.rbColonFormat)
            TimeDisplayStyle.LETTER -> findViewById<androidx.appcompat.widget.AppCompatRadioButton>(
                R.id.rbLetterFormat
            )

            TimeDisplayStyle.NATURAL_LANGUAGE -> findViewById<androidx.appcompat.widget.AppCompatRadioButton>(
                R.id.rbNaturalLanguageFormat
            )

            TimeDisplayStyle.VERBOSE_SINGLE -> findViewById<androidx.appcompat.widget.AppCompatRadioButton>(
                R.id.rbVerboseSingleUnitFormat
            )

            TimeDisplayStyle.COUNTDOWN_WORDS -> findViewById<androidx.appcompat.widget.AppCompatRadioButton>(
                R.id.rbCountdownWordsFormat
            )

            TimeDisplayStyle.MINIMAL_PROGRESS -> findViewById<androidx.appcompat.widget.AppCompatRadioButton>(
                R.id.rbProgressOnlyFormat
            )
        }
        radioButton.isChecked = true

        val radioToStyleMap = mapOf(
            R.id.rbColonFormat to TimeDisplayStyle.COLON,
            R.id.rbLetterFormat to TimeDisplayStyle.LETTER,
            R.id.rbNaturalLanguageFormat to TimeDisplayStyle.NATURAL_LANGUAGE,
            R.id.rbVerboseSingleUnitFormat to TimeDisplayStyle.VERBOSE_SINGLE,
            R.id.rbCountdownWordsFormat to TimeDisplayStyle.COUNTDOWN_WORDS,
            R.id.rbProgressOnlyFormat to TimeDisplayStyle.MINIMAL_PROGRESS
        )

        val radioGroup = findViewById<android.widget.RadioGroup>(R.id.timeFormatRadioGroup)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedStyle = radioToStyleMap[checkedId] ?: return@setOnCheckedChangeListener

            // Save the selected style immediately
            WidgetPreferencesManager.saveTimeDisplayStyle(this, appWidgetId, selectedStyle)
            toggleManager.applyTimeStyleConstraints(selectedStyle)

            previewController.updateTimerText(
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbDays).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbHours).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbMinutes).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbSeconds).isChecked,
                selectedStyle
            )
            // 🆕 Force the actual widget to update on screen
            CountdownWidget.forceUpdateAll(this)
        }

        toggleManager.applyTimeStyleConstraints(savedStyle)
        previewController.updateTimerText(
            findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbDays).isChecked,
            findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbHours).isChecked,
            findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbMinutes).isChecked,
            findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbSeconds).isChecked,
            savedStyle
        )

        findViewById<Button>(R.id.btnSaveCustomize).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnResetAllColors).setOnClickListener {
            colorManager.resetAllColorsToDefault()
            Toast.makeText(this, "All colors reset to defaults", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnResetDefaults).setOnClickListener {
            toggleManager.resetToDefaultConfig()
            colorManager.reloadColors()
            Toast.makeText(this, "Widget reset to defaults.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observePreview() {
        val context = this
        val id = appWidgetId

        val titleFallback = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            context.getColor(R.color.onSurface)
        )
        val dateFallback = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            context.getColor(R.color.onSurface)
        )
        val iconFallback = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            context.getColor(R.color.onSurface)
        )
        val timerFallback = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            context.getColor(R.color.onSurface)
        )
        val percentFallback = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOnSurface,
            context.getColor(R.color.onSurface)
        )
        val surfaceFallback = MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSurface,
            context.getColor(R.color.colorWidgetBackground)
        )

        val showTitleF =
            com.aboayman.finaltick.SettingsManager.toggleFlow(context, id, "show_title", true)
        val showDateF =
            com.aboayman.finaltick.SettingsManager.toggleFlow(context, id, "show_date", true)
        val showTimerF =
            com.aboayman.finaltick.SettingsManager.toggleFlow(context, id, "show_timer", true)
        val showProgressF =
            com.aboayman.finaltick.SettingsManager.toggleFlow(context, id, "show_progress", true)
        val showPercentF =
            com.aboayman.finaltick.SettingsManager.toggleFlow(context, id, "show_percentage", true)
        val showIconF =
            com.aboayman.finaltick.SettingsManager.toggleFlow(context, id, "show_icon", true)

        val titleColorF = com.aboayman.finaltick.SettingsManager.colorFlow(
            context,
            id,
            "color_title",
            titleFallback
        )
        val dateColorF = com.aboayman.finaltick.SettingsManager.colorFlow(
            context,
            id,
            "color_date",
            dateFallback
        )
        val timerColorF = com.aboayman.finaltick.SettingsManager.colorFlow(
            context,
            id,
            "color_timer",
            timerFallback
        )
        val percentColorF = com.aboayman.finaltick.SettingsManager.colorFlow(
            context,
            id,
            "color_percentage",
            percentFallback
        )
        val iconColorF = com.aboayman.finaltick.SettingsManager.colorFlow(
            context,
            id,
            "color_icon",
            iconFallback
        )

        val bgColorF = com.aboayman.finaltick.SettingsManager.colorFlow(
            context,
            id,
            "color_background",
            surfaceFallback
        )
        val bgAlphaF =
            com.aboayman.finaltick.SettingsManager.colorFlow(context, id, "background_alpha", 0xCC)

        val styleF = com.aboayman.finaltick.SettingsManager.timeStyleFlow(context, id)
        val shapeF = com.aboayman.finaltick.SettingsManager.shapeFlow(context, id)

        val togglesPart1 =
            combine(showTitleF, showDateF, showTimerF) { a: Boolean, b: Boolean, c: Boolean ->
                Triple(a, b, c)
            }
        val togglesPart2 =
            combine(showProgressF, showPercentF, showIconF) { d: Boolean, e: Boolean, f: Boolean ->
                Triple(d, e, f)
            }
        val togglesFlow = combine(togglesPart1, togglesPart2) { p1, p2 ->
            arrayOf(p1.first, p1.second, p1.third, p2.first, p2.second, p2.third)
        }
        val colorsFlow = combine(
            titleColorF,
            dateColorF,
            timerColorF,
            percentColorF,
            iconColorF
        ) { a, b, c, d, e -> arrayOf(a, b, c, d, e) }
        val bgFlow = combine(bgColorF, bgAlphaF) { c, a -> c to a }

        lifecycleScope.launch {
            combine(togglesFlow, colorsFlow, bgFlow, styleF, shapeF) { t, cols, bg, style, shape ->
                PreviewState(
                    showTitle = t[0] as Boolean,
                    showDate = t[1] as Boolean,
                    showTimer = t[2] as Boolean,
                    showProgress = t[3] as Boolean,
                    showPercent = t[4] as Boolean,
                    showIcon = t[5] as Boolean,
                    titleColor = cols[0] as Int,
                    dateColor = cols[1] as Int,
                    timerColor = cols[2] as Int,
                    percentColor = cols[3] as Int,
                    iconColor = cols[4] as Int,
                    bgColor = bg.first,
                    bgAlpha = bg.second,
                    style = style,
                    shape = shape
                )
            }.collect { state ->
                applyPreview(state)
            }
        }
    }

    private fun applyPreview(state: PreviewState) {
        previewController.updateVisibility(
            state.showTitle, state.showDate, state.showTimer,
            state.showProgress, state.showPercent, state.showIcon
        )
        previewController.applyColors(
            state.titleColor,
            state.dateColor,
            state.timerColor,
            state.percentColor,
            state.iconColor
        )
        previewController.applyShape(state.shape)
        previewController.applyBackground(state.bgColor, state.bgAlpha)
        previewController.updateTimerText(
            showDays = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbDays).isChecked,
            showHours = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbHours).isChecked,
            showMinutes = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbMinutes).isChecked,
            showSeconds = findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbSeconds).isChecked,
            style = state.style
        )
    }

    private data class PreviewState(
        val showTitle: Boolean,
        val showDate: Boolean,
        val showTimer: Boolean,
        val showProgress: Boolean,
        val showPercent: Boolean,
        val showIcon: Boolean,
        val titleColor: Int,
        val dateColor: Int,
        val timerColor: Int,
        val percentColor: Int,
        val iconColor: Int,
        val bgColor: Int,
        val bgAlpha: Int,
        val style: WidgetPreferencesManager.TimeDisplayStyle,
        val shape: String
    )
}
