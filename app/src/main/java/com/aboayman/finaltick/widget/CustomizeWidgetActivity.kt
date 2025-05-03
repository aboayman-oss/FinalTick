package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.R
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle

class CustomizeWidgetActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var toggleManager: WidgetToggleManager
    private lateinit var colorManager: WidgetColorManager
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

        toggleManager.init()
        colorManager.init()

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
}
