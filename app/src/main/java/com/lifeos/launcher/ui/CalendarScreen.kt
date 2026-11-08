package com.lifeos.launcher.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.launcher.R
import com.lifeos.launcher.data.CalEvent
import com.lifeos.launcher.data.CalendarRepo
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private fun timeRange(e: CalEvent): String {
    if (e.allDay) return "All day"
    val zone = ZoneId.systemDefault()
    val f = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val s = Instant.ofEpochMilli(e.begin).atZone(zone).format(f)
    val en = Instant.ofEpochMilli(e.end).atZone(zone).format(f)
    return "$s – $en"
}

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshCalendar()
    }
    val month: YearMonth = vm.visibleMonth
    val selected: LocalDate = vm.selectedDate
    val today = LocalDate.now()
    val start = vm.gridStart(month)
    val events = vm.monthEvents[selected].orEmpty()
    val monthLabel = month.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    val markerColors = listOf(C.Cherry, C.Honey, C.Olive)
    val agendaTitle = if (selected == today) {
        "Today · " + selected.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    } else selected.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_bl, Modifier.align(Alignment.BottomStart).offset(x = (-36).dp, y = 26.dp).size(190.dp, 170.dp))
        },
        bottom = {
            Box(Modifier.fillMaxWidth().height(150.dp)) {
                Deco(R.drawable.ill_lantern, Modifier.align(Alignment.BottomEnd).offset(x = (-6).dp).size(60.dp, 150.dp))
                Deco(R.drawable.deco_scatter, Modifier.align(Alignment.BottomCenter).offset(x = (-30).dp).size(200.dp, 40.dp))
            }
        },
    ) {
        PageHeader("Calendar", "Plan ahead.") {
            RoundIconButton(Icons.Rounded.CalendarToday, { vm.selectDate(LocalDate.now()) }, desc = "Jump to today")
        }

        if (!vm.calGranted) {
            GlassCard(strong = true) {
                Eyebrow("Calendar access", color = C.Cherry)
                Spacer(Modifier.height(6.dp))
                Heading("Show your real events.", size = 22.sp)
                Spacer(Modifier.height(6.dp))
                Body("Life OS reads events from the calendars already on your phone. It never changes or uploads them.")
                Spacer(Modifier.height(14.dp))
                PillButton("Allow calendar access", { permission.launch(Manifest.permission.READ_CALENDAR) })
            }
        }

        GlassCard(padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { vm.shiftMonth(-1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = C.Ink, modifier = Modifier.size(24.dp))
                }
                Text(
                    "$monthLabel ${month.year}", Modifier.weight(1f), textAlign = TextAlign.Center,
                    color = C.Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                )
                Box(Modifier.size(44.dp).clip(CircleShape).clickable { vm.shiftMonth(1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ChevronRight, "Next month", tint = C.Ink, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = C.Ink, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            for (w in 0 until 6) {
                Row(Modifier.fillMaxWidth()) {
                    for (d in 0 until 7) {
                        val date = start.plusDays((w * 7 + d).toLong())
                        val isSel = date == selected
                        val inMonth = YearMonth.from(date) == month
                        val dayEvents = vm.monthEvents[date].orEmpty()
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape)
                                .background(if (isSel) C.Cherry else Color.Transparent)
                                .then(if (date == today && !isSel) Modifier.border(1.dp, C.Cherry, CircleShape) else Modifier)
                                .clickable { vm.selectDate(date) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                date.dayOfMonth.toString(), fontSize = 15.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = when {
                                    isSel -> C.Cream2
                                    !inMonth -> C.Brown.copy(alpha = 0.45f)
                                    else -> C.Ink
                                },
                            )
                            if (dayEvents.isNotEmpty()) {
                                Box(
                                    Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSel) C.Cream2
                                            else if (dayEvents.first().allDay) C.Olive else C.Honey,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        GlassCard(padding = 18.dp) {
            Text(agendaTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Spacer(Modifier.height(6.dp))
            if (events.isEmpty()) {
                Body(
                    if (vm.calGranted) "A little breathing room is part of the plan." else "Allow calendar access to see events here.",
                    Modifier.padding(vertical = 6.dp),
                )
            } else {
                events.forEachIndexed { i, e ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ctx.safeStart(CalendarRepo.viewEventIntent(e.id)) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(markerColors[i % markerColors.size]))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, color = C.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                            Text(timeRange(e), color = C.Brown, fontSize = 13.sp)
                            if (!e.location.isNullOrBlank()) Text(e.location, color = C.Brown, fontSize = 12.sp, maxLines = 1)
                        }
                    }
                    if (i < events.lastIndex) GlassDivider()
                }
            }
            Spacer(Modifier.height(12.dp))
            PillButton("+ Add event", { ctx.safeStart(CalendarRepo.addEventIntent(selected)) })
        }
    }
}
