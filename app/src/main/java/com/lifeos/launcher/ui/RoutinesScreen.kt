package com.lifeos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.data.Period
import com.lifeos.launcher.data.RoutineEntity

@Composable
fun RoutinesScreen(vm: MainViewModel) {
    val routines by vm.routines.collectAsStateWithLifecycle()
    val today = vm.today
    val done = routines.count { it.doneDate == today }

    // (period, label, pill tint)
    val periods = listOf(
        Triple(Period.MORNING, "Morning", C.HoneyTint),
        Triple(Period.AFTERNOON, "Afternoon", C.OliveTint),
        Triple(Period.EVENING, "Evening", C.TerraTint),
    )

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_branch, Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = 205.dp).size(70.dp, 116.dp))
        },
        bottom = { Deco(R.drawable.ill_cat_blanket, Modifier.fillMaxWidth().height(134.dp), Alignment.BottomCenter) },
    ) {
        PageHeader("Routines", "Small habits. Big changes.") {
            // a plain pill: GlassCard fills the full width, which squeezed the page title away
            Box(Modifier.glass(16.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("$done / ${routines.size}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            }
        }

        periods.forEach { (key, label, tint) ->
            val items: List<RoutineEntity> = routines.filter { it.period == key }
            val periodDone = items.count { it.doneDate == today }
            GlassCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryPill(label, tint)
                    Spacer(Modifier.weight(1f))
                    Text("$periodDone / ${items.size}", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Ink)
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable { vm.editRoutine = RoutineEntity(title = "", period = key) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, "Add $label routine", tint = C.Cherry, modifier = Modifier.size(22.dp)) }
                }
                if (items.isEmpty()) {
                    Body("Nothing here yet.", Modifier.padding(vertical = 8.dp), size = 13.sp)
                } else {
                    items.forEachIndexed { i, r ->
                        TaskRow(
                            title = r.title,
                            sub = r.subtitle,
                            trailing = null,
                            done = r.doneDate == today,
                            onToggle = { vm.toggleRoutine(r) },
                            onClick = { vm.editRoutine = r },
                            divider = i < items.lastIndex,
                        )
                    }
                }
            }
        }
    }
}
