package com.aboayman.finaltick.widget

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.R
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape

data class ColorBinding(
    val key: String,
    val previewViewId: Int,
    val resetButtonId: Int,
    val applyToPreview: () -> Unit
)

class WidgetColorManager(
    private val activity: AppCompatActivity,
    private val appWidgetId: Int,
    private val previewController: FakeWidgetPreviewController
) {
    private val context: Context get() = activity
    private val defaultColor = context.getColor(R.color.onSurface)

    private val bindings = listOf(
        ColorBinding("color_title", R.id.colorPreviewTitle, R.id.btnResetTitleColor) {
            previewController.applyColors(
                titleColor = getColor("color_title"),
                null, null, null, null
            )
        },
        ColorBinding("color_date", R.id.colorPreviewDate, R.id.btnResetDateColor) {
            previewController.applyColors(
                null,
                dateColor = getColor("color_date"),
                null, null, null
            )
        },
        ColorBinding("color_icon", R.id.colorPreviewIcon, R.id.btnResetIconColor) {
            previewController.applyColors(
                null, null, null, null,
                iconTint = getColor("color_icon")
            )
        },
        ColorBinding("color_timer", R.id.colorPreviewTimer, R.id.btnResetTimerColor) {
            previewController.applyColors(
                null, null,
                timerColor = getColor("color_timer"),
                null, null
            )
        },
        ColorBinding(
            "color_percentage",
            R.id.colorPreviewPercentage,
            R.id.btnResetPercentageColor
        ) {
            previewController.applyColors(
                null, null, null,
                percentColor = getColor("color_percentage"),
                null
            )
        }
    )

    fun init() {
        bindings.forEach { binding ->
            val previewView = activity.findViewById<View>(binding.previewViewId)
            val resetButton = activity.findViewById<Button>(binding.resetButtonId)
            val savedColor = getColor(binding.key)

            previewView.background.setTint(savedColor)

            previewView.setOnClickListener {
                openColorPicker(binding.key, previewView) {
                    binding.applyToPreview()
                }
            }

            resetButton.setOnClickListener {
                WidgetPreferencesManager.removeKey(context, appWidgetId, binding.key)
                previewView.background.setTint(defaultColor)
                binding.applyToPreview()
                CountdownWidget.forceUpdateAll(context)
            }
        }
    }

    fun resetAllColorsToDefault() {
        bindings.forEach {
            WidgetPreferencesManager.removeKey(context, appWidgetId, it.key)
        }
        reloadColors()
        CountdownWidget.forceUpdateAll(context)
    }

    fun reloadColors() {
        bindings.forEach {
            val view = activity.findViewById<View>(it.previewViewId)
            view.background.setTint(getColor(it.key))
            it.applyToPreview()
        }
    }

    private fun getColor(key: String): Int {
        return WidgetPreferencesManager.getColor(context, appWidgetId, key, defaultColor)
    }

    private fun openColorPicker(key: String, previewView: View, apply: () -> Unit) {
        val currentColor = getColor(key)
        ColorPickerDialog.Builder(context)
            .setTitle("Pick a color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(currentColor)
            .setColorListener { color, _ ->
                WidgetPreferencesManager.saveColor(context, appWidgetId, key, color)
                previewView.background.setTint(color)
                apply()
                CountdownWidget.forceUpdateWidget(context, appWidgetId)
            }
            .setNegativeButton("Cancel")
            .show()
    }
}