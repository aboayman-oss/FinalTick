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
import com.aboayman.finaltick.widget.WidgetPreferencesManager.FontChoice
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

        // Font spinners: Title, Date, Timer, Percentage
        initFontSpinners()

        // Progress style spinner
        val spinnerProgress = findViewById<android.widget.Spinner>(R.id.spinnerProgressStyle)
        // Initialize selection from saved preference and wire change listener
        lifecycleScope.launch {
            val saved =
                WidgetPreferencesManager.getProgressStyle(this@CustomizeWidgetActivity, appWidgetId)
            val index = when (saved) {
                "dashed" -> 1
                "gradient" -> 2
                else -> 0
            }
            spinnerProgress?.setSelection(index)
            previewController.applyProgressStyle(saved)
        }
        spinnerProgress?.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val style = when (position) {
                        1 -> "dashed"
                        2 -> "gradient"
                        else -> "solid"
                    }
                    lifecycleScope.launch {
                        WidgetPreferencesManager.saveProgressStyle(
                            this@CustomizeWidgetActivity,
                            appWidgetId,
                            style
                        )
                        previewController.applyProgressStyle(style)
                        CountdownWidget.forceUpdateWidget(this@CustomizeWidgetActivity, appWidgetId)
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) { /* no-op */
                }
            }

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

        lifecycleScope.launch {
            val savedStyle = WidgetPreferencesManager.getTimeDisplayStyle(
                this@CustomizeWidgetActivity,
                appWidgetId
            )
            val checkId = when (savedStyle) {
                TimeDisplayStyle.COLON -> R.id.rbColonFormat
                TimeDisplayStyle.LETTER -> R.id.rbLetterFormat
                TimeDisplayStyle.NATURAL_LANGUAGE -> R.id.rbNaturalLanguageFormat
                TimeDisplayStyle.VERBOSE_SINGLE -> R.id.rbVerboseSingleUnitFormat
                TimeDisplayStyle.COUNTDOWN_WORDS -> R.id.rbCountdownWordsFormat
                TimeDisplayStyle.MINIMAL_PROGRESS -> R.id.rbProgressOnlyFormat
            }
            findViewById<android.widget.RadioGroup>(R.id.timeFormatRadioGroup).check(checkId)
        }

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
            // Apply constraints immediately so preview reflects the new style
            toggleManager.applyTimeStyleConstraints(selectedStyle)

            // Save the selected style immediately
            lifecycleScope.launch {
                WidgetPreferencesManager.saveTimeDisplayStyle(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    selectedStyle
                )
                // constraints already applied above to update preview instantly
            }

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

        lifecycleScope.launch {
            val savedStyle = WidgetPreferencesManager.getTimeDisplayStyle(
                this@CustomizeWidgetActivity,
                appWidgetId
            )
            toggleManager.applyTimeStyleConstraints(savedStyle)
            previewController.updateTimerText(
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbDays).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbHours).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbMinutes).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbSeconds).isChecked,
                savedStyle
            )
        }

        findViewById<Button>(R.id.btnSaveCustomize).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnResetAllColors).setOnClickListener {
            colorManager.resetAllColorsToDefault()
            Toast.makeText(this, "All colors reset to defaults", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnResetDefaults).setOnClickListener {
            // Reset toggles/layout
            toggleManager.resetToDefaultConfig()

            // Reset colors (including background + alpha)
            colorManager.resetAllColorsToDefault()

            // Reset time style to default (COLON) and re-apply constraints
            val radio = findViewById<android.widget.RadioGroup>(R.id.timeFormatRadioGroup)
            radio.check(R.id.rbColonFormat)
            lifecycleScope.launch {
                WidgetPreferencesManager.saveTimeDisplayStyle(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    TimeDisplayStyle.COLON
                )
            }
            toggleManager.applyTimeStyleConstraints(TimeDisplayStyle.COLON)
            previewController.updateTimerText(
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbDays).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbHours).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbMinutes).isChecked,
                findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.cbSeconds).isChecked,
                TimeDisplayStyle.COLON
            )

            // Reset shape to rounded and UI chips
            lifecycleScope.launch {
                WidgetPreferencesManager.saveShape(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    "rounded"
                )
            }
            previewController.applyShape("rounded")
            runCatching {
                val chipRounded =
                    findViewById<com.google.android.material.chip.Chip>(R.id.chipShapeRounded)
                val chipGroup =
                    findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipShapeGroup)
                chipGroup.check(chipRounded.id)
            }

            // Reset progress style
            val spinnerProgress = findViewById<android.widget.Spinner>(R.id.spinnerProgressStyle)
            spinnerProgress?.setSelection(0)
            lifecycleScope.launch {
                WidgetPreferencesManager.saveProgressStyle(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    "solid"
                )
            }
            previewController.applyProgressStyle("solid")

            // Reset fonts to defaults (ROBOTO)
            lifecycleScope.launch {
                WidgetPreferencesManager.saveTitleFont(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    FontChoice.ROBOTO
                )
                WidgetPreferencesManager.saveDateFont(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    FontChoice.ROBOTO
                )
                WidgetPreferencesManager.saveTimerFont(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    FontChoice.ROBOTO
                )
                WidgetPreferencesManager.savePercentFont(
                    this@CustomizeWidgetActivity,
                    appWidgetId,
                    FontChoice.ROBOTO
                )
            }
            runCatching { initFontSpinners() }

            Toast.makeText(this, "Widget reset to defaults.", Toast.LENGTH_SHORT).show()
            CountdownWidget.forceUpdateAll(this)
        }
    }

    private fun initFontSpinners() {
        val spinnerTitle = findViewById<android.widget.Spinner>(R.id.spinnerFontTitle)
        val spinnerDate = findViewById<android.widget.Spinner>(R.id.spinnerFontDate)
        val spinnerTimer = findViewById<android.widget.Spinner>(R.id.spinnerFontTimer)
        val spinnerPercent = findViewById<android.widget.Spinner>(R.id.spinnerFontPercentage)

        fun toIndex(choice: FontChoice): Int = when (choice) {
            FontChoice.ROBOTO -> 0
            FontChoice.ROBOTO_REGULAR -> 1
            FontChoice.ROBOTO_MEDIUM -> 2
            FontChoice.ROBOTO_LIGHT -> 3
            FontChoice.ROBOTO_CONDENSED -> 4
            FontChoice.ROBOTO_BLACK -> 5
            FontChoice.ROBOTO_THIN -> 6
            FontChoice.SERIF -> 7
            FontChoice.MONOSPACE -> 8
        }

        fun toChoice(index: Int): FontChoice = when (index) {
            0 -> FontChoice.ROBOTO
            1 -> FontChoice.ROBOTO_REGULAR
            2 -> FontChoice.ROBOTO_MEDIUM
            3 -> FontChoice.ROBOTO_LIGHT
            4 -> FontChoice.ROBOTO_CONDENSED
            5 -> FontChoice.ROBOTO_BLACK
            6 -> FontChoice.ROBOTO_THIN
            7 -> FontChoice.SERIF
            8 -> FontChoice.MONOSPACE
            else -> FontChoice.ROBOTO
        }

        lifecycleScope.launch {
            // Load saved selections
            spinnerTitle?.setSelection(
                toIndex(
                    WidgetPreferencesManager.getTitleFont(
                        this@CustomizeWidgetActivity,
                        appWidgetId
                    )
                )
            )
            spinnerDate?.setSelection(
                toIndex(
                    WidgetPreferencesManager.getDateFont(
                        this@CustomizeWidgetActivity,
                        appWidgetId
                    )
                )
            )
            spinnerTimer?.setSelection(
                toIndex(
                    WidgetPreferencesManager.getTimerFont(
                        this@CustomizeWidgetActivity,
                        appWidgetId
                    )
                )
            )
            spinnerPercent?.setSelection(
                toIndex(
                    WidgetPreferencesManager.getPercentFont(
                        this@CustomizeWidgetActivity,
                        appWidgetId
                    )
                )
            )
        }

        fun applyPreviewFontsFromSpinners() {
            val t = toChoice(spinnerTitle?.selectedItemPosition ?: 0)
            val d = toChoice(spinnerDate?.selectedItemPosition ?: 0)
            val ti = toChoice(spinnerTimer?.selectedItemPosition ?: 0)
            val p = toChoice(spinnerPercent?.selectedItemPosition ?: 0)
            previewController.applyFonts(t, d, ti, p)
        }

        val listener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                lifecycleScope.launch {
                    when (parent.id) {
                        R.id.spinnerFontTitle -> WidgetPreferencesManager.saveTitleFont(
                            this@CustomizeWidgetActivity,
                            appWidgetId,
                            toChoice(position)
                        )

                        R.id.spinnerFontDate -> WidgetPreferencesManager.saveDateFont(
                            this@CustomizeWidgetActivity,
                            appWidgetId,
                            toChoice(position)
                        )

                        R.id.spinnerFontTimer -> WidgetPreferencesManager.saveTimerFont(
                            this@CustomizeWidgetActivity,
                            appWidgetId,
                            toChoice(position)
                        )

                        R.id.spinnerFontPercentage -> WidgetPreferencesManager.savePercentFont(
                            this@CustomizeWidgetActivity,
                            appWidgetId,
                            toChoice(position)
                        )
                    }
                    // Reflect immediately in live preview
                    applyPreviewFontsFromSpinners()
                    // Update any live preview text sizes or content if needed, then refresh widgets
                    CountdownWidget.forceUpdateAll(this@CustomizeWidgetActivity)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        spinnerTitle?.onItemSelectedListener = listener
        spinnerDate?.onItemSelectedListener = listener
        spinnerTimer?.onItemSelectedListener = listener
        spinnerPercent?.onItemSelectedListener = listener

        // Apply initial preview fonts once selections are set
        spinnerPercent?.post { applyPreviewFontsFromSpinners() }
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
        val progressStyleF = com.aboayman.finaltick.SettingsManager.progressStyleFlow(context, id)

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

        // Build a typed base state from 5 flows, then layer progress style
        val baseStateFlow =
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
                    shape = shape,
                    progressStyle = "solid"
                )
            }

        lifecycleScope.launch {
            combine(baseStateFlow, progressStyleF) { base, pstyle ->
                base.copy(progressStyle = pstyle)
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
        previewController.applyProgressStyle(state.progressStyle)
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
        val shape: String,
        val progressStyle: String
    )
}
