package com.aboayman.finaltick

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.snackbar.Snackbar

// Track current snackbar globally
private var currentSnackbar: Snackbar? = null

fun dismissCurrentSnackbar() {
    currentSnackbar?.dismiss()
    currentSnackbar = null
}

private fun showCustomSnackbar(
    context: Context,
    rootView: View,
    message: String,
    layoutRes: Int,
    actionIconRes: Int? = null,
    actionDesc: String? = null,
    onAction: (() -> Unit)? = null
) {
    // Create the base snackbar
    val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG)
    val snackbarView = snackbar.view

    // Make the Snackbar background fully transparent
    snackbarView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    snackbarView.alpha = 1f
    snackbarView.elevation = 8f

    // Hide the default text inside Snackbar
    snackbarView.findViewById<View>(com.google.android.material.R.id.snackbar_text)?.visibility =
        View.INVISIBLE

    // Inflate your custom layout
    val customView =
        LayoutInflater.from(context).inflate(layoutRes, snackbarView as ViewGroup, false)

    val icon = customView.findViewById<ImageView>(R.id.snackbar_icon)
    val text = customView.findViewById<TextView>(R.id.snackbar_text)
    val actionBtn = customView.findViewById<ImageButton>(R.id.snackbar_action_icon)

    text.text = message

    if (actionIconRes != null && onAction != null) {
        actionBtn.setImageResource(actionIconRes)
        actionBtn.contentDescription = actionDesc ?: "Action"
        actionBtn.visibility = View.VISIBLE
        actionBtn.setOnClickListener {
            onAction()
            snackbar.dismiss()
        }
    } else {
        actionBtn.visibility = View.GONE
    }

    // Add the custom view to the Snackbar layout
    (snackbarView as ViewGroup).addView(customView)

    // Set bottom margin to float above navigation bar
    val density = context.resources.displayMetrics.density
    val marginPx = (16 * density).toInt()
    val marginLayoutParams = snackbarView.layoutParams as? ViewGroup.MarginLayoutParams
    marginLayoutParams?.bottomMargin = marginPx
    snackbarView.layoutParams = marginLayoutParams

    snackbar.show()
    currentSnackbar = snackbar

    // Animate custom view IN after show
    customView.alpha = 0f
    customView.translationY = 40f
    customView.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(250)
        .start()

    // Let SnackbarBaseLayout handle dismiss and animate out automatically
}

// --- Helper functions for different snackbar types ---

fun Context.showSuccessSnackbar(root: View, message: String, onClose: (() -> Unit)? = null) {
    showCustomSnackbar(
        context = this,
        rootView = root,
        message = message,
        layoutRes = R.layout.custom_snackbar_success,
        actionIconRes = R.drawable.close,
        actionDesc = "Close",
        onAction = onClose
    )
}

fun Context.showErrorSnackbar(root: View, message: String, onUndo: (() -> Unit)? = null) {
    showCustomSnackbar(
        context = this,
        rootView = root,
        message = message,
        layoutRes = R.layout.custom_snackbar_error,
        actionIconRes = R.drawable.history,
        actionDesc = "Undo",
        onAction = onUndo
    )
}

fun Context.showInfoSnackbar(root: View, message: String, onClose: (() -> Unit)? = null) {
    showCustomSnackbar(
        context = this,
        rootView = root,
        message = message,
        layoutRes = R.layout.custom_snackbar_info,
        actionIconRes = R.drawable.close,
        actionDesc = "Close",
        onAction = onClose
    )
}

fun Context.showWarningSnackbar(root: View, message: String, onClose: (() -> Unit)? = null) {
    showCustomSnackbar(
        context = this,
        rootView = root,
        message = message,
        layoutRes = R.layout.custom_snackbar_warning,
        actionIconRes = R.drawable.close,
        actionDesc = "Close",
        onAction = onClose
    )
}