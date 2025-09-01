package com.aboayman.finaltick.widget

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aboayman.finaltick.CountdownWidget
import com.aboayman.finaltick.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class WidgetAppearanceManager(
    private val activity: AppCompatActivity,
    private val appWidgetId: Int,
    private val previewController: FakeWidgetPreviewController
) {
    fun init() {
        val chipGroup = activity.findViewById<ChipGroup>(R.id.chipShapeGroup)
        val chipRounded = activity.findViewById<Chip>(R.id.chipShapeRounded)
        val chipPill = activity.findViewById<Chip>(R.id.chipShapePill)
        val chipSquare = activity.findViewById<Chip>(R.id.chipShapeSquare)

        activity.lifecycleScope.launch {
            val saved = WidgetPreferencesManager.getShape(activity, appWidgetId)
            when (saved) {
                "pill" -> chipGroup.check(chipPill.id)
                "square" -> chipGroup.check(chipSquare.id)
                else -> chipGroup.check(chipRounded.id)
            }
            previewController.applyShape(saved)
        }

        chipGroup.setOnCheckedStateChangeListener { _, ids ->
            val selected = when (ids.firstOrNull()) {
                chipPill.id -> "pill"
                chipSquare.id -> "square"
                chipRounded.id -> "rounded"
                else -> "rounded"
            }
            com.aboayman.finaltick.Haptics.perform(activity, chipGroup)
            activity.lifecycleScope.launch {
                WidgetPreferencesManager.saveShape(activity, appWidgetId, selected)
                previewController.applyShape(selected)
                CountdownWidget.forceUpdateWidget(activity, appWidgetId)
            }
        }
    }
}
