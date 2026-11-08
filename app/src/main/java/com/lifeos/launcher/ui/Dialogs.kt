package com.lifeos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.lifeos.launcher.R
import com.lifeos.launcher.data.CalendarRepo
import com.lifeos.launcher.data.Period
import com.lifeos.launcher.data.ProjectEntity
import com.lifeos.launcher.data.RoutineEntity
import com.lifeos.launcher.data.Sec
import com.lifeos.launcher.data.TaskEntity
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Click with no ripple (used to catch taps on dialog scrims and cards). */
@Composable
internal fun Modifier.tap(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)

/** Keeps the page behind a dialog visible (softly dimmed) instead of the default heavy black scrim. */
@Composable
internal fun SoftDim(amount: Float = 0.18f) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect { window?.setDimAmount(amount) }
}

@Composable
fun LoDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SoftDim()
        Box(Modifier.fillMaxSize().tap(onDismiss).imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth()
                    .glass(26.dp, strong = true).tap {}
                    .padding(22.dp).verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

@Composable
fun DialogHeader(eyebrow: String, title: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Eyebrow(eyebrow)
            Spacer(Modifier.height(6.dp))
            Heading(title, size = 28.sp)
        }
        RoundIconButton(Icons.Rounded.Close, onClose, size = 40.dp, desc = "Close")
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
fun LoField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(hint, color = C.Brown, fontSize = 15.sp) },
        singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Fonts.sans, fontSize = 16.sp, color = C.Ink),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = C.CherrySoft, unfocusedBorderColor = C.Hairline,
            focusedContainerColor = C.Peach.copy(alpha = glassAlpha(0.38f)), unfocusedContainerColor = C.Peach.copy(alpha = glassAlpha(0.30f)),
            cursorColor = C.Cherry, focusedTextColor = C.Ink, unfocusedTextColor = C.Ink,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
        ),
    )
}

@Composable
fun LoChip(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val shape = CircleShape
    Row(
        Modifier.heightIn(min = 40.dp).clip(shape)
            .background(if (selected) C.Cherry.copy(alpha = 0.14f) else C.GlassTint.copy(alpha = glassAlpha(0.62f)))
            .border(1.dp, if (selected) C.Cherry else C.Hairline, shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) C.Cherry else C.Ink, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, fontSize = 14.sp, color = if (selected) C.Cherry else C.Ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) -> LoChip(label, key == selected, { onSelect(key) }) }
    }
}

@Composable
fun DeleteLink(onDelete: () -> Unit) {
    Text(
        "Delete", color = C.Cherry, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.heightIn(min = 48.dp).clip(CircleShape).clickable(onClick = onDelete)
            .padding(horizontal = 16.dp, vertical = 13.dp),
    )
}

private val sectionOptions = listOf(
    Sec.NOW to "Now", Sec.TODAY to "Today", Sec.UPCOMING to "Upcoming", Sec.WORK to "Projects",
)

/** Four-point sparkle used on the capture panel. */
@Composable
private fun Sparkle(modifier: Modifier = Modifier) {
    Box(
        modifier.size(40.dp).drawBehind {
            val w = size.width; val h = size.height; val c = Offset(w / 2, h / 2)
            val p = Path().apply {
                moveTo(c.x, h * 0.06f)
                cubicTo(c.x + w * 0.04f, h * 0.38f, c.x + w * 0.12f, c.y - h * 0.04f, w * 0.94f, c.y)
                cubicTo(c.x + w * 0.12f, c.y + h * 0.04f, c.x + w * 0.04f, h * 0.62f, c.x, h * 0.94f)
                cubicTo(c.x - w * 0.04f, h * 0.62f, c.x - w * 0.12f, c.y + h * 0.04f, w * 0.06f, c.y)
                cubicTo(c.x - w * 0.12f, c.y - h * 0.04f, c.x - w * 0.04f, h * 0.38f, c.x, h * 0.06f)
                close()
            }
            drawPath(p, C.Cherry, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        },
    )
}

// ---------------------------------------------------------------- capture
@Composable
fun CaptureDialog(vm: MainViewModel) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("task") }
    var section by remember { mutableStateOf(Sec.TODAY) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        runCatching { focus.requestFocus() }
    }
    val close = { vm.captureOpen = false }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SoftDim(0.12f)
        Box(Modifier.fillMaxSize().tap { close() }.imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 20.dp).widthIn(max = 460.dp).fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.fillMaxWidth().glass(26.dp, strong = true).tap {}.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Sparkle()
                    Spacer(Modifier.height(10.dp))
                    Heading("Capture anything", size = 28.sp, align = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Body("Add a task, note, event or idea…", align = TextAlign.Center, color = C.Ink)
                    Spacer(Modifier.height(16.dp))
                    LoField(
                        text, { text = it }, "Type here…",
                        modifier = Modifier.focusRequester(focus).heightIn(min = 112.dp),
                        singleLine = false, minLines = 3,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LoChip("Task", type == "task", { type = "task" }, Icons.Rounded.TaskAlt)
                        LoChip("Note", type == "note", { type = "note" }, Icons.Rounded.Description)
                        LoChip("Event", type == "event", { type = "event" }, Icons.Rounded.CalendarMonth)
                    }
                    if (type == "task") {
                        Spacer(Modifier.height(10.dp))
                        ChipRow(sectionOptions, section) { section = it }
                    }
                    Spacer(Modifier.height(16.dp))
                    PillButton(
                        if (type == "event") "Open in calendar" else "Save",
                        onClick = {
                            if (text.isBlank()) {
                                vm.toast("Write something first")
                            } else {
                                when (type) {
                                    "task" -> vm.addTask(text, section)
                                    "note" -> vm.addNote(text)
                                    else -> ctx.safeStart(CalendarRepo.addEventIntent(vm.selectedDate, text.trim()))
                                }
                                close()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Deco(R.drawable.ill_mug_books, Modifier.height(120.dp).offset(y = (-6).dp))
            }
        }
    }
}

// ---------------------------------------------------------------- task
@Composable
fun TaskDialog(
    initial: TaskEntity,
    onSave: (TaskEntity) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
    projects: List<ProjectEntity> = emptyList(),
) {
    var title by remember { mutableStateOf(initial.title) }
    var tag by remember { mutableStateOf(initial.tag) }
    var est by remember { mutableStateOf(if (initial.estimateMin > 0) initial.estimateMin.toString() else "") }
    var due by remember { mutableStateOf(initial.dueLabel) }
    var sec by remember { mutableStateOf(initial.section) }
    var projectId by remember { mutableStateOf(initial.projectId) }
    LoDialog(onDismiss) {
        DialogHeader(if (initial.id == 0L) "New task" else "Edit task", "Shape the step.", onDismiss)
        LoField(title, { title = it }, "What needs doing?")
        Spacer(Modifier.height(10.dp))
        LoField(tag, { tag = it }, "Tag (Work, Errands…)")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LoField(est, { est = it.filter { c -> c.isDigit() }.take(3) }, "Minutes", Modifier.weight(1f), keyboardType = KeyboardType.Number)
            LoField(due, { due = it }, "Time / date", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        ChipRow(sectionOptions, sec) { sec = it }
        if (projects.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Eyebrow("Project")
            Spacer(Modifier.height(6.dp))
            ChipRow(
                listOf("0" to "None") + projects.filter { !it.done || it.id == projectId }.map { it.id.toString() to it.title },
                projectId.toString(),
            ) { projectId = it.toLongOrNull() ?: 0L }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillButton("Save", {
                if (title.isNotBlank()) {
                    onSave(initial.copy(title = title.trim(), tag = tag.trim(), estimateMin = est.toIntOrNull() ?: 0, dueLabel = due.trim(), section = sec, projectId = projectId))
                }
            }, Modifier.weight(1f))
            if (onDelete != null) DeleteLink(onDelete)
        }
    }
}

// ---------------------------------------------------------------- routine
@Composable
fun RoutineDialog(initial: RoutineEntity, onSave: (RoutineEntity) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var sub by remember { mutableStateOf(initial.subtitle) }
    var period by remember { mutableStateOf(initial.period) }
    LoDialog(onDismiss) {
        DialogHeader(if (initial.id == 0L) "New routine" else "Edit routine", "Small habits, steady days.", onDismiss)
        LoField(title, { title = it }, "Routine")
        Spacer(Modifier.height(10.dp))
        LoField(sub, { sub = it }, "A short note (optional)")
        Spacer(Modifier.height(12.dp))
        ChipRow(
            listOf(Period.MORNING to "Morning", Period.AFTERNOON to "Afternoon", Period.EVENING to "Evening"),
            period,
        ) { period = it }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillButton("Save", {
                if (title.isNotBlank()) onSave(initial.copy(title = title.trim(), subtitle = sub.trim(), period = period))
            }, Modifier.weight(1f))
            if (onDelete != null) DeleteLink(onDelete)
        }
    }
}

// ---------------------------------------------------------------- project
@Composable
fun ProjectDialog(initial: ProjectEntity, onSave: (ProjectEntity) -> Unit, onDelete: (() -> Unit)?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var sub by remember { mutableStateOf(initial.subtitle) }
    var cat by remember { mutableStateOf(initial.category) }
    var colorIdx by remember { mutableIntStateOf(initial.colorIdx) }
    var due by remember { mutableStateOf(initial.dueDate) }
    var pickDate by remember { mutableStateOf(false) }
    LoDialog(onDismiss) {
        DialogHeader(if (initial.id == 0L) "New project" else "Edit project", "Make it a clear next step.", onDismiss)
        LoField(title, { title = it }, "Project name")
        Spacer(Modifier.height(10.dp))
        LoField(sub, { sub = it }, "What does done look like?")
        Spacer(Modifier.height(10.dp))
        LoField(cat, { cat = it }, "Category (Work, Personal…)")
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            C.ProjectColors.forEachIndexed { i, col ->
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(col)
                        .border(if (i == colorIdx) 3.dp else 0.dp, C.Ink, CircleShape)
                        .clickable { colorIdx = i },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Eyebrow("Deadline")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LoChip(
                if (due.isBlank()) "Set a date" else prettyDate(due),
                selected = due.isNotBlank(),
                onClick = { pickDate = true },
                icon = Icons.Rounded.CalendarMonth,
            )
            if (due.isNotBlank()) LoChip("Clear", false, { due = "" })
        }
        Spacer(Modifier.height(8.dp))
        Body("Progress is worked out from the project's tasks. Add them on the project page.", size = 12.sp)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillButton("Save", {
                if (title.isNotBlank()) {
                    onSave(initial.copy(title = title.trim(), subtitle = sub.trim(), category = cat.trim().ifBlank { "Personal" }, colorIdx = colorIdx, dueDate = due))
                }
            }, Modifier.weight(1f))
            if (onDelete != null) DeleteLink(onDelete)
        }
    }
    if (pickDate) DuePicker(due, { due = it; pickDate = false }, { pickDate = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuePicker(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val start = try { LocalDate.parse(current) } catch (_: Exception) { LocalDate.now() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms == null) onDismiss()
                else onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state = state) }
}

// ---------------------------------------------------------------- initials
@Composable
fun InitialsDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var v by remember { mutableStateOf(current) }
    LoDialog(onDismiss) {
        DialogHeader("Profile", "Your initials.", onDismiss)
        LoField(v, { v = it.take(2) }, "Two letters")
        Spacer(Modifier.height(16.dp))
        PillButton("Save", { onSave(v) }, Modifier.fillMaxWidth())
    }
}
