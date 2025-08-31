package com.aboayman.finaltick.widget

import android.content.Context
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.Haptics
import com.aboayman.finaltick.R
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

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
    private val defaultBackgroundColor: Int by lazy {
        MaterialColors.getColor(
            activity,
            com.google.android.material.R.attr.colorSurface,
            context.getColor(R.color.colorSurface)
        )
    }

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

        // Background controls
        val bgPreview = activity.findViewById<View>(R.id.colorPreviewBackground)
        val bgReset = activity.findViewById<Button>(R.id.btnResetBackgroundColor)
        val slider = activity.findViewById<Slider>(R.id.sliderBackgroundAlpha)
        val alphaLabel =
            activity.findViewById<android.widget.TextView>(R.id.tvBackgroundOpacityValue)

        val bgColor = WidgetPreferencesManager.getColor(
            context,
            appWidgetId,
            "color_background",
            defaultBackgroundColor
        )
        val bgAlpha =
            WidgetPreferencesManager.getColor(context, appWidgetId, "background_alpha", 0xCC)

        bgPreview.background.setTint(bgColor)
        slider.value = bgAlpha.toFloat()
        runCatching {
            val pct = ((bgAlpha / 255f) * 100).roundToInt()
            alphaLabel?.text = "$pct%"
        }
        previewController.applyBackground(bgColor, bgAlpha)

        bgPreview.setOnClickListener {
            Haptics.perform(context, bgPreview)
            openColorPicker("color_background", bgPreview) {
                val c = WidgetPreferencesManager.getColor(
                    context,
                    appWidgetId,
                    "color_background",
                    defaultBackgroundColor
                )
                previewController.applyBackground(
                    c,
                    WidgetPreferencesManager.getColor(
                        context,
                        appWidgetId,
                        "background_alpha",
                        0xCC
                    )
                )
            }
        }
        bgReset.setOnClickListener {
            Haptics.perform(context, bgReset)
            WidgetPreferencesManager.removeKey(context, appWidgetId, "color_background")
            bgPreview.background.setTint(defaultBackgroundColor)
            previewController.applyBackground(
                defaultBackgroundColor,
                WidgetPreferencesManager.getColor(context, appWidgetId, "background_alpha", 0xCC)
            )
            CountdownWidget.forceUpdateWidget(context, appWidgetId)
        }
        slider.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val alpha = value.toInt().coerceIn(0, 255)
            WidgetPreferencesManager.saveColor(context, appWidgetId, "background_alpha", alpha)
            val currentBg = WidgetPreferencesManager.getColor(
                context,
                appWidgetId,
                "color_background",
                defaultBackgroundColor
            )
            previewController.applyBackground(currentBg, alpha)
            runCatching {
                val pct = ((alpha / 255f) * 100).roundToInt()
                alphaLabel?.text = "$pct%"
            }
            Haptics.perform(context, slider)
            CountdownWidget.forceUpdateWidget(context, appWidgetId)
        }
    }

    fun resetAllColorsToDefault() {
        bindings.forEach {
            WidgetPreferencesManager.removeKey(context, appWidgetId, it.key)
        }
        WidgetPreferencesManager.removeKey(context, appWidgetId, "color_background")
        WidgetPreferencesManager.removeKey(context, appWidgetId, "background_alpha")
        reloadColors()
        CountdownWidget.forceUpdateAll(context)
    }

    fun reloadColors() {
        bindings.forEach {
            val view = activity.findViewById<View>(it.previewViewId)
            view.background.setTint(getColor(it.key))
            it.applyToPreview()
        }
        val bgPreview = activity.findViewById<View>(R.id.colorPreviewBackground)
        val bgColor = WidgetPreferencesManager.getColor(
            context,
            appWidgetId,
            "color_background",
            defaultBackgroundColor
        )
        bgPreview.background.setTint(bgColor)
        val alpha =
            WidgetPreferencesManager.getColor(context, appWidgetId, "background_alpha", 0xCC)
        activity.findViewById<Slider>(R.id.sliderBackgroundAlpha)?.value = alpha.toFloat()
        activity.findViewById<android.widget.TextView>(R.id.tvBackgroundOpacityValue)?.let { tv ->
            val pct = ((alpha / 255f) * 100).roundToInt()
            tv.text = "$pct%"
        }
        previewController.applyBackground(bgColor, alpha)
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
                Haptics.perform(context, previewView)
                WidgetPreferencesManager.saveColor(context, appWidgetId, key, color)
                previewView.background.setTint(color)
                apply()
                CountdownWidget.forceUpdateWidget(context, appWidgetId)
            }
            .setNegativeButton("Cancel")
            .show()
    }
}
