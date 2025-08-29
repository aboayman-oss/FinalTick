package com.aboayman.finaltick

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object Haptics {
    fun perform(context: Context, anchor: View? = null, feedbackType: Int? = null) {
        val prefs = context.getSharedPreferences("finaltick_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("haptic_feedback", true)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            val effect = when (feedbackType) {
                HapticFeedbackConstants.CLOCK_TICK -> android.os.VibrationEffect.EFFECT_TICK
                else -> android.os.VibrationEffect.EFFECT_HEAVY_CLICK
            }
            vib.vibrate(android.os.VibrationEffect.createPredefined(effect))
        } else {
            if (feedbackType != null) {
                anchor?.performHapticFeedback(feedbackType)
            } else {
                anchor?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }
}