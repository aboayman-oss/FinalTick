package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.R

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

        findViewById<Button>(R.id.btnSaveCustomize).setOnClickListener {
            toggleManager.saveAll()
            CountdownWidget.forceUpdateAll(this)
            Toast.makeText(this, "Customization saved!", Toast.LENGTH_SHORT).show()
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
