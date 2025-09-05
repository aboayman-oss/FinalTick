@file:OptIn(ExperimentalLayoutApi::class)

package com.aboayman.finaltick.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aboayman.finaltick.widget.CustomizeWidgetViewModel.FontFamilyOption
import com.aboayman.finaltick.widget.CustomizeWidgetViewModel.FontWeightOption
import com.aboayman.finaltick.widget.CustomizeWidgetViewModel.ProgressStyle
import com.aboayman.finaltick.widget.CustomizeWidgetViewModel.ShapeStyle
import com.aboayman.finaltick.widget.CustomizeWidgetViewModel.SizePreset
import com.aboayman.finaltick.widget.WidgetPreferencesManager.TimeDisplayStyle
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun CustomizeWidgetScreen(
    appWidgetId: Int,
    onBack: () -> Unit = {},
    onSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm: CustomizeWidgetViewModel = viewModel(
        factory = CustomizeWidgetViewModel.Factory(context.applicationContext, appWidgetId)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var menuOpen by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Reset all to defaults") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Undo,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    showResetConfirm = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export style") },
                                onClick = {
                                    menuOpen = false
                                    val json = vm.exportStyle()
                                    val cm =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("FinalTickStyle", json))
                                    scope.launch { snackbarHost.showSnackbar("Style copied to clipboard") }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import style") },
                                onClick = {
                                    menuOpen = false
                                    importText = ""
                                    showImportDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    // Live preview defined later
                    WidgetLivePreview(state)
                }
            }
            item {
                SectionCard(title = "Style") {
                    LabeledRow(title = "Progress style") {
                        WrapChips(
                            options = listOf(
                                "Solid" to ProgressStyle.Solid,
                                "Dashed" to ProgressStyle.Dashed,
                                "Gradient" to ProgressStyle.Gradient
                            ),
                            selected = state.progressStyle,
                            onSelect = vm::onProgressStyleSelected
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LabeledRow(title = "Shape") {
                        WrapChips(
                            options = listOf(
                                "Rounded" to ShapeStyle.Rounded,
                                "Pill" to ShapeStyle.Pill,
                                "Square" to ShapeStyle.Square
                            ),
                            selected = state.shape,
                            onSelect = vm::onShapeSelected
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Typography") {
                    LabeledRow(title = "Family") {
                        WrapChips(
                            options = listOf(
                                "Sans" to FontFamilyOption.Sans,
                                "Serif" to FontFamilyOption.Serif,
                                "Mono" to FontFamilyOption.Mono
                            ),
                            selected = state.family,
                            onSelect = vm::onFamilySelected
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LabeledRow(title = "Weight") {
                        WrapChips(
                            options = listOf(
                                "Thin" to FontWeightOption.Thin,
                                "Light" to FontWeightOption.Light,
                                "Regular" to FontWeightOption.Regular,
                                "Medium" to FontWeightOption.Medium,
                                "Black" to FontWeightOption.Black
                            ),
                            selected = state.weight,
                            onSelect = vm::onWeightSelected
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Content & Visibility") {
                    LabeledRow(title = "Time style") {
                        WrapChips(
                            options = listOf(
                                "Compact (00:00)" to TimeDisplayStyle.COLON,
                                "Letters (1d 2h)" to TimeDisplayStyle.LETTER,
                                "Natural (2 parts)" to TimeDisplayStyle.NATURAL_LANGUAGE,
                                "Verbose (single)" to TimeDisplayStyle.VERBOSE_SINGLE,
                                "Countdown (single)" to TimeDisplayStyle.COUNTDOWN_WORDS
                            ),
                            selected = state.timeStyle,
                            onSelect = vm::onTimeStyleSelected
                        )
                    }
                    Text(
                        text = when (state.timeStyle) {
                            TimeDisplayStyle.VERBOSE_SINGLE, TimeDisplayStyle.COUNTDOWN_WORDS ->
                                "Uses exactly one unit; choose which unit below"

                            else -> "Choose units shown; order is Days, Hours, Minutes, Seconds"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.timeStyle == TimeDisplayStyle.VERBOSE_SINGLE || state.timeStyle == TimeDisplayStyle.COUNTDOWN_WORDS) {
                        RadioLikeChips(
                            options = listOf(
                                "Days" to "days",
                                "Hours" to "hours",
                                "Minutes" to "minutes",
                                "Seconds" to "seconds"
                            ),
                            selectedKey = when {
                                state.units.days -> "days"
                                state.units.hours -> "hours"
                                state.units.minutes -> "minutes"
                                else -> "seconds"
                            },
                            onSelect = { key -> vm.onUnitChanged(key, true) }
                        )
                    } else {
                        ToggleChips(
                            pairs = listOf(
                                "Days" to state.units.days,
                                "Hours" to state.units.hours,
                                "Minutes" to state.units.minutes,
                                "Seconds" to state.units.seconds
                            ),
                            onToggle = { label, checked ->
                                val key = when (label) {
                                    "Days" -> "days"
                                    "Hours" -> "hours"
                                    "Minutes" -> "minutes"
                                    else -> "seconds"
                                }
                                vm.onUnitChanged(key, checked)
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    VisibilityToggles(
                        showTitle = state.showTitle,
                        showDate = state.showDate,
                        showTimer = state.showTimer,
                        showProgress = state.showProgress,
                        showPercent = state.showPercent,
                        showIcon = state.showIcon,
                        onToggle = { key, v -> vm.onToggleChanged(key, v) }
                    )
                }
            }

            item {
                SectionCard(title = "Colors") {
                    ColorPickerRow(
                        title = "Text",
                        argb = state.textColor,
                        supportsAlpha = false,
                        onPick = { vm.onTextColorChanged(it) },
                        onReset = { vm.onTextColorChanged(null) }
                    )
                    Spacer(Modifier.height(12.dp))
                    ColorPickerRow(
                        title = "Progress %",
                        argb = state.progressColor,
                        supportsAlpha = false,
                        onPick = { vm.onProgressColorChanged(it) },
                        onReset = { vm.onProgressColorChanged(null) }
                    )
                    Spacer(Modifier.height(12.dp))
                    ColorPickerRow(
                        title = "Background",
                        argb = state.backgroundColor,
                        supportsAlpha = true,
                        onPick = { vm.onBackgroundColorChanged(it) },
                        onReset = { vm.onBackgroundColorChanged(null) },
                        extra = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Alpha", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.size(12.dp))
                                Slider(
                                    value = state.backgroundAlpha / 255f,
                                    onValueChange = { vm.onBackgroundAlphaChanged((it * 255).toInt()) },
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "${state.backgroundAlpha}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            vm.forceRefreshNow()
                            onSaved()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun VisibilityToggles(
    showTitle: Boolean,
    showDate: Boolean,
    showTimer: Boolean,
    showProgress: Boolean,
    showPercent: Boolean,
    showIcon: Boolean,
    onToggle: (String, Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Title", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = showTitle,
                onCheckedChange = { onToggle("show_title", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Date", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = showDate,
                onCheckedChange = { onToggle("show_date", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Timer", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = showTimer,
                onCheckedChange = { onToggle("show_timer", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Progress bar", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = showProgress,
                onCheckedChange = { onToggle("show_progress", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Percent", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = showPercent,
                onCheckedChange = { onToggle("show_percentage", it) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh icon", modifier = Modifier.weight(1f))
            androidx.compose.material3.Switch(
                checked = showIcon,
                onCheckedChange = { onToggle("show_icon", it) })
        }
    }
}

// Building blocks below (smaller pieces to avoid long patch)

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun WidgetLivePreview(state: CustomizeWidgetViewModel.UiState) {
    // Simulate grid cell size: 80x100dp per cell
    val width = when (state.sizePreset) {
        SizePreset.C1x1 -> 80.dp
        SizePreset.C2x1 -> 160.dp
        SizePreset.C4x2 -> 320.dp
    }
    val height = when (state.sizePreset) {
        SizePreset.C1x1 -> 100.dp
        SizePreset.C2x1 -> 100.dp
        SizePreset.C4x2 -> 200.dp
    }

    val bgColor = state.backgroundColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
    val bgTint = bgColor.copy(alpha = state.backgroundAlpha / 255f)
    val textColor = state.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
    val percentColor = state.progressColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(width, height)
                    .clip(
                        when (state.shape) {
                            ShapeStyle.Rounded -> RoundedCornerShape(16.dp)
                            ShapeStyle.Pill -> RoundedCornerShape(999.dp)
                            ShapeStyle.Square -> RoundedCornerShape(4.dp)
                        }
                    )
                    .background(bgTint)
                    .semantics { contentDescription = "Widget preview ${state.sizePreset.name}" }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (state.showTitle) {
                        Text(
                            text = "My Deadline",
                            style = previewTextStyle(state, 16.sp, textColor)
                        )
                    }
                    if (state.showDate) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Fri, Nov 15 • 6:00 PM",
                            style = previewTextStyle(state, 12.sp, textColor.copy(alpha = 0.85f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.showProgress) {
                        when (state.progressStyle) {
                            ProgressStyle.Solid -> SolidProgressBarPreview(percentColor)
                            ProgressStyle.Dashed -> DashedProgressBarPreview(percentColor)
                            ProgressStyle.Gradient -> GradientProgressBarPreview()
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (state.showTimer || state.showPercent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.showTimer) {
                                Text(
                                    text = previewTimerText(state),
                                    style = previewTextStyle(state, 18.sp, textColor),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else Spacer(Modifier.weight(1f))
                            if (state.showPercent) {
                                Text("42%", style = previewTextStyle(state, 14.sp, percentColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun previewTextStyle(
    state: CustomizeWidgetViewModel.UiState,
    size: androidx.compose.ui.unit.TextUnit,
    color: Color
): TextStyle {
    val family = when (state.family) {
        FontFamilyOption.Sans -> FontFamily.SansSerif
        FontFamilyOption.Serif -> FontFamily.Serif
        FontFamilyOption.Mono -> FontFamily.Monospace
    }
    val weight = when (state.weight) {
        FontWeightOption.Thin -> FontWeight.Thin
        FontWeightOption.Light -> FontWeight.Light
        FontWeightOption.Regular -> FontWeight.Normal
        FontWeightOption.Medium -> FontWeight.Medium
        FontWeightOption.Black -> FontWeight.Black
    }
    return TextStyle(fontFamily = family, fontWeight = weight, fontSize = size, color = color)
}

@Composable
private fun SolidProgressBarPreview(color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(track)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(10.dp)
                .background(color)
        )
    }
}

@Composable
private fun ColorPickerRow(
    title: String,
    argb: Int?,
    supportsAlpha: Boolean,
    onPick: (Int?) -> Unit,
    onReset: () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val color = argb?.let { Color(it) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color ?: MaterialTheme.colorScheme.surfaceVariant)
                    .semantics {
                        contentDescription = "$title color ${argb?.let { toHex(it) } ?: "Default"}"
                    }
            )
            Spacer(Modifier.size(8.dp))
            Text(argb?.let { toHex(it) } ?: "Default", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = {
                val dialog = yuku.ambilwarna.AmbilWarnaDialog(
                    context,
                    (argb ?: 0xFF2196F3.toInt()),
                    supportsAlpha,
                    object : yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onOk(dialog: yuku.ambilwarna.AmbilWarnaDialog?, color: Int) {
                            onPick(color)
                        }

                        override fun onCancel(dialog: yuku.ambilwarna.AmbilWarnaDialog?) {}
                    }
                )
                dialog.show()
            }) { Icon(Icons.Default.Refresh, contentDescription = "Edit color") }
            Spacer(Modifier.size(4.dp))
            TextButton(onClick = onReset) { Text("Reset") }
        }
        if (extra != null) {
            Spacer(Modifier.height(8.dp)); extra()
        }
    }
}

private fun toHex(argb: Int): String = "#" + (argb ushr 24 and 0xFF).toString(16).padStart(2, '0') +
        (argb ushr 16 and 0xFF).toString(16).padStart(2, '0') +
        (argb ushr 8 and 0xFF).toString(16).padStart(2, '0') +
        (argb and 0xFF).toString(16).padStart(2, '0')

@Composable
private fun previewTimerText(state: CustomizeWidgetViewModel.UiState): String {
    val d = 12L;
    val h = 8L;
    val m = 30L;
    val s = 25L
    return when (state.timeStyle) {
        TimeDisplayStyle.COLON -> buildList {
            if (state.units.days) add(d.toString())
            if (state.units.hours) add(h.toString().padStart(2, '0'))
            if (state.units.minutes) add(m.toString().padStart(2, '0'))
            if (state.units.seconds) add(s.toString().padStart(2, '0'))
        }.joinToString(":")

        TimeDisplayStyle.LETTER -> buildString {
            if (state.units.days) append("${'$'}d d ")
            if (state.units.hours) append("${'$'}h h ")
            if (state.units.minutes) append("${'$'}m m ")
            if (state.units.seconds) append("${'$'}s s")
        }.trim()

        TimeDisplayStyle.NATURAL_LANGUAGE -> buildList {
            if (state.units.days) add("12 days")
            if (state.units.hours) add("8 hours")
            if (state.units.minutes) add("30 minutes")
            if (state.units.seconds) add("25 seconds")
        }.take(2).joinToString(", ") + " remaining"

        TimeDisplayStyle.VERBOSE_SINGLE -> when {
            state.units.days -> "12 days remaining"
            state.units.hours -> "8 hours remaining"
            state.units.minutes -> "30 minutes remaining"
            else -> "25 seconds remaining"
        }

        TimeDisplayStyle.COUNTDOWN_WORDS -> when {
            state.units.days -> "Only 12 days left!"
            state.units.hours -> "Only 8 hours left!"
            state.units.minutes -> "Only 30 minutes left!"
            else -> "Only 25 seconds left!"
        }

        TimeDisplayStyle.MINIMAL_PROGRESS -> "Progress: 42%"
    }
}

@Composable
private fun DashedProgressBarPreview(color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val total = 20
    val filled = (total * 0.42f).toInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(track)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(total) { idx ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (idx < filled) color else track)
            )
        }
    }
}

@Composable
private fun GradientProgressBarPreview() {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val grad = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.secondary
        )
    )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(track)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(10.dp)
                .background(grad)
        )
    }
}

@Composable
private fun LabeledRow(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun <T> WrapChips(
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, value) ->
            val chosen = value == selected
            FilterChip(
                selected = chosen,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun RadioLikeChips(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, key) ->
            val chosen = key == selectedKey
            FilterChip(
                selected = chosen,
                onClick = { onSelect(key) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun ToggleChips(
    pairs: List<Pair<String, Boolean>>,
    onToggle: (String, Boolean) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pairs.forEach { (label, checked) ->
            FilterChip(
                selected = checked,
                onClick = { onToggle(label, !checked) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SizePresetRow(selected: SizePreset, onSelected: (SizePreset) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        WrapChips(
            options = listOf(
                "1×1" to SizePreset.C1x1,
                "2×1" to SizePreset.C2x1,
                "4×2" to SizePreset.C4x2
            ),
            selected = selected,
            onSelect = onSelected
        )
    }
}
