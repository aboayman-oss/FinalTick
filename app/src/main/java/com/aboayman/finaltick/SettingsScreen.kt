package com.aboayman.finaltick

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Modern Compose settings screen wired to SettingsViewModel (DataStore-backed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val context = LocalContext.current

    val theme by vm.theme.collectAsStateWithLifecycle()
    val haptics by vm.hapticsEnabled.collectAsStateWithLifecycle()
    val weight by vm.timerWeight.collectAsStateWithLifecycle()
    val colorInt by vm.timerColor.collectAsStateWithLifecycle()
    val dynamic by vm.timerDynamic.collectAsStateWithLifecycle()
    val confirmOnExit by vm.confirmOnExit.collectAsStateWithLifecycle()
    val resetBusy by vm.resetInProgress.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "Settings") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionCard(title = "Appearance") {
                // Theme selector (System / Light / Dark)
                RowSetting(title = "Theme") {
                    SegmentedOptions(
                        options = listOf(
                            "System" to GlobalSettingsManager.ThemeMode.System,
                            "Light" to GlobalSettingsManager.ThemeMode.Light,
                            "Dark" to GlobalSettingsManager.ThemeMode.Dark
                        ),
                        selected = theme,
                        onSelected = vm::onThemeSelected
                    )
                }

            }

            SectionCard(title = "General") {
                ToggleSetting(
                    title = "Haptic Feedback",
                    checked = haptics,
                    onCheckedChange = vm::onHapticsToggled,
                    subtitle = "Vibrate on taps and actions"
                )

                Spacer(modifier = Modifier.height(8.dp))

                ToggleSetting(
                    title = "Confirm on Exit",
                    checked = confirmOnExit,
                    onCheckedChange = vm::onConfirmOnExitToggled,
                    subtitle = "Ask before closing the app"
                )
            }

            SectionCard(title = "Defaults") {
                // Timer Weight
                RowSetting(title = "Countdown Timer Weight") {
                    SegmentedOptions(
                        options = listOf(
                            "Light" to 300,
                            "Regular" to 400,
                            "Medium" to 500,
                            "SemiBold" to 600,
                            "Bold" to 700
                        ),
                        selected = weight,
                        onSelected = vm::onTimerWeightSelected
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic color toggle
                ToggleSetting(
                    title = "Timer Color: Dynamic by progress",
                    checked = dynamic,
                    onCheckedChange = vm::onTimerDynamicToggled
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Color picker button
                ColorPickerSetting(
                    context = context,
                    title = "Default Timer Color",
                    color = Color(colorInt),
                    onPick = { vm.onTimerColorSelected(it) }
                )
            }

            SectionCard(title = "Danger Zone") {
                Button(onClick = { showResetDialog = true }, enabled = !resetBusy) {
                    Text(text = if (resetBusy) "Resetting..." else "Reset App")
                }
            }
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Confirm Reset") },
                text = { Text("This will clear all settings and widget data. Continue?") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        vm.resetApp()
                    }) { Text("Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("No") }
                }
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun RowSetting(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> SegmentedOptions(
    options: List<Pair<String, T>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Text(
                text = label,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onSelected(value) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ColorPickerSetting(
    context: Context,
    title: String,
    color: Color,
    onPick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Color chip preview
            Spacer(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .height(32.dp)
                    .weight(1f)
            )
            Spacer(modifier = Modifier.height(0.dp))
            Button(onClick = {
                // Use existing AmbilWarna dialog to pick a color
                val dialog = yuku.ambilwarna.AmbilWarnaDialog(
                    context,
                    color.toArgb(),
                    true,
                    object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                            onPick(color)
                        }

                        override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                    }
                )
                dialog.show()
            }) {
                Text("Pick Color")
            }
        }
    }
}
