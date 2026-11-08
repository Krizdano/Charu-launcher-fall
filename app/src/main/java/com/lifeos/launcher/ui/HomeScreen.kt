package com.lifeos.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.data.CalEvent
import com.lifeos.launcher.data.Sec
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun eventWhen(e: CalEvent): String {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val z = if (e.allDay) ZoneOffset.UTC else zone
    val d = Instant.ofEpochMilli(e.begin).atZone(z).toLocalDate()
    val day = when {
        d.isBefore(today) || d == today -> "Today"
        d == today.plusDays(1) -> "Tomorrow"
        else -> d.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
    val time = if (e.allDay) "All day"
    else Instant.ofEpochMilli(e.begin).atZone(zone).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return "$day · $time"
}

@Composable
fun HomeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val np by vm.media.state.collectAsStateWithLifecycle()
    val battery = rememberBattery()

    val dayTasks = tasks.filter { it.section == Sec.NOW || it.section == Sec.TODAY }
    val total = dayTasks.size
    val doneCount = dayTasks.count { it.done }
    val frac = if (total == 0) 0f else doneCount / total.toFloat()
    val pending = dayTasks.filter { !it.done }
    val focus = pending.firstOrNull()
    val next = pending.getOrNull(1)
    val up = vm.upcoming

    val hour = vm.now.hour
    val greeting = when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
    val clockPattern = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "hh:mm"

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-18).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_edge_l, Modifier.align(Alignment.CenterStart).offset(x = (-40).dp, y = 130.dp).size(90.dp, 215.dp))
            Deco(R.drawable.deco_edge_r, Modifier.align(Alignment.CenterEnd).offset(x = 36.dp, y = (-30).dp).size(90.dp, 215.dp))
        },
        bottom = { Deco(R.drawable.ill_books_mug, Modifier.fillMaxWidth().height(116.dp), Alignment.BottomCenter) },
    ) {
        // ---- clock + greeting ----
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    vm.now.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                    fontSize = 16.sp, fontWeight = FontWeight.Medium, color = C.Ink,
                )
                Text(
                    vm.now.format(DateTimeFormatter.ofPattern(clockPattern)),
                    fontFamily = Fonts.display, fontSize = 68.sp, fontWeight = FontWeight.Medium,
                    color = C.Cherry, letterSpacing = (-1.5).sp, lineHeight = 72.sp,
                )
            }
            if (battery >= 0) {
                GlassCard(Modifier.padding(top = 18.dp).width(112.dp), radius = 18.dp, padding = 10.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BatteryFull, null, tint = C.Olive, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text("$battery%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                            Text("Battery", fontSize = 11.sp, color = C.Brown)
                        }
                    }
                }
            }
        }
        Column(Modifier.padding(top = 4.dp)) {
            Heading(greeting, size = 29.sp, weight = FontWeight.Medium)
            Body("Let's make today count.", size = 16.sp, color = C.Ink)
        }

        // ---- progress ----
        GlassCard(padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (total == 0) "No tasks planned yet" else "$doneCount / $total tasks done",
                    Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.Ink,
                )
                Text("${(frac * 100).toInt()}%", fontSize = 13.sp, color = C.Brown)
            }
            Spacer(Modifier.height(10.dp))
            LoProgress(frac)
        }

        // ---- focus ----
        GlassCard(strong = true, padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(if (focus != null) C.Cherry else C.Olive))
                Spacer(Modifier.width(8.dp))
                Eyebrow(if (focus != null) "Now" else "All clear", color = C.Ink)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(40.dp).clip(CircleShape).clickable { vm.tab = Tab.Tasks }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronRight, "Open tasks", tint = C.Brown, modifier = Modifier.size(22.dp))
                }
            }
            if (focus != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CheckCircle(false, { vm.completeFocus(focus) })
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(focus.title, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = C.Ink)
                        val meta = listOf(focus.tag, if (focus.estimateMin > 0) "${focus.estimateMin} min" else "")
                            .filter { it.isNotBlank() }.joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Folder, null, tint = C.Brown, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(meta, fontSize = 13.sp, color = C.Brown)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val active = vm.focusFor == focus.id
                val label = when {
                    active && vm.focusRunning -> "Pause · ${mmss(vm.focusRemaining)}"
                    active -> "Resume · ${mmss(vm.focusRemaining)}"
                    else -> "Start"
                }
                PillButton(label, { vm.toggleFocus(focus) }, Modifier.fillMaxWidth())
                if (next != null) {
                    Spacer(Modifier.height(12.dp))
                    GlassDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Next:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                    Row(Modifier.clickable { vm.editTask = next }, verticalAlignment = Alignment.CenterVertically) {
                        CheckCircle(false, { vm.toggleTask(next) }, small = true)
                        Text(next.title, Modifier.weight(1f), fontSize = 14.sp, color = C.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (next.dueLabel.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text("Today · ${next.dueLabel}", fontSize = 12.sp, color = C.Brown)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Heading("Nice work. Take a breath.", size = 24.sp)
                Spacer(Modifier.height(4.dp))
                Body("Nothing left for today. Add something with the + button.")
            }
        }

        // ---- next event + now playing ----
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.weight(1f).fillMaxHeight(), radius = 20.dp, padding = 14.dp, onClick = { vm.tab = Tab.Calendar }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Next Event", fontSize = 12.sp, color = C.Brown)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    when {
                        !vm.calGranted -> "Connect calendar"
                        up != null -> up.title
                        else -> "Nothing scheduled"
                    },
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        !vm.calGranted -> "Tap to allow access"
                        up != null -> eventWhen(up)
                        else -> "A clear stretch ahead"
                    },
                    fontSize = 13.sp, color = C.Brown,
                )
            }
            GlassCard(Modifier.weight(1f).fillMaxHeight(), radius = 20.dp, padding = 14.dp, onClick = { if (np?.playing == true) vm.tab = Tab.Music else vm.openMusicApp(ctx) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, null, tint = C.Ink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Now Playing", Modifier.weight(1f), fontSize = 12.sp, color = C.Brown)
                    if (np?.playing == true) Icon(Icons.Rounded.GraphicEq, null, tint = C.Cherry, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    np?.title ?: "Nothing playing",
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(np?.artist?.ifBlank { null } ?: "Tap to open music", fontSize = 13.sp, color = C.Brown, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // ---- inbox (notes captured with the + button) ----
        if (notes.isNotEmpty()) {
            GlassCard(padding = 16.dp) {
                Eyebrow("Inbox", color = C.Ink)
                Spacer(Modifier.height(4.dp))
                notes.forEachIndexed { i, n ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(n.text, Modifier.weight(1f).padding(vertical = 6.dp), fontSize = 14.sp, color = C.Ink)
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable { vm.deleteNote(n) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Delete note", tint = C.Brown, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (i < notes.lastIndex) GlassDivider()
                }
            }
        }
    }
}
