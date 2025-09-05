package com.aboayman.finaltick.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.FinalTickTheme
import com.aboayman.finaltick.R

/**
 * Host activity for the Compose-based Customize Widget screen.
 * Replaces the legacy XML layout and Spinner-based UI.
 */
class CustomizeWidgetActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FinalTick)
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            FinalTickTheme {
                CustomizeWidgetScreen(
                    appWidgetId = appWidgetId,
                    onBack = { finish() },
                    onSaved = { finish() }
                )
            }
        }
    }
}

