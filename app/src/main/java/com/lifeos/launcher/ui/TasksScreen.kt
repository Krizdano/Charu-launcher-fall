package com.lifeos.launcher.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.data.Sec
import com.lifeos.launcher.data.TaskEntity

private data class TaskGroup(val title: String, val section: String, val kind: String)

private val groups = listOf(
    TaskGroup("Now", Sec.NOW, "today"),
    TaskGroup("Today", Sec.TODAY, "today"),
    TaskGroup("Upcoming", Sec.UPCOMING, "upcoming"),
    TaskGroup("Projects", Sec.WORK, "work"),
)

@Composable
fun TasksScreen(vm: MainViewModel) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("all") }
    var menu by remember { mutableStateOf(false) }
    val q = vm.taskQuery.trim()
    val searching = q.isNotBlank()
    val visible = tasks.filter { searching.not() || it.title.contains(q, true) || it.tag.contains(q, true) }
    val doneCount = tasks.count { it.done }
    val shown = groups.filter { filter == "all" || it.kind == filter }
        .map { g -> g to visible.filter { it.section == g.section } }
        .filter { (_, items) -> !searching || items.isNotEmpty() }

    PageScroll(
        decor = {
            Deco(R.drawable.leaf_rust, Modifier.align(Alignment.TopStart).offset(x = 168.dp, y = 6.dp).size(42.dp).rotate(-24f))
            Deco(R.drawable.deco_edge_r, Modifier.align(Alignment.CenterEnd).offset(x = 40.dp, y = 90.dp).size(90.dp, 215.dp))
            Deco(R.drawable.deco_bl, Modifier.align(Alignment.BottomStart).offset(x = (-34).dp, y = 22.dp).size(190.dp, 170.dp))
            Deco(R.drawable.deco_br, Modifier.align(Alignment.BottomEnd).offset(x = 30.dp, y = 26.dp).size(190.dp, 170.dp))
        },
    ) {
        PageHeader("Tasks", "Get things done.") {
            RoundIconButton(Icons.Rounded.Search, {
                vm.searchOpen = !vm.searchOpen
                if (!vm.searchOpen) vm.taskQuery = ""
            }, desc = "Search tasks")
            Box {
                RoundIconButton(Icons.Rounded.MoreVert, { menu = true }, desc = "More options")
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (doneCount > 0) "Clear completed ($doneCount)" else "No completed tasks") },
                        enabled = doneCount > 0,
                        onClick = { menu = false; vm.clearDoneTasks() },
                    )
                }
            }
        }

        if (vm.searchOpen) LoField(vm.taskQuery, { vm.taskQuery = it }, "Search tasks…")

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All", "today" to "Today", "upcoming" to "Upcoming", "work" to "Projects").forEach { (k, l) ->
                FilterPill(l, filter == k) { filter = k }
            }
        }

        GlassCard(padding = 16.dp) {
            if (shown.isEmpty()) Body("No matching tasks.", Modifier.padding(vertical = 8.dp))
            shown.forEach { (g, items) ->
                SectionLabel(g.title, items.size.toString())
                if (items.isEmpty()) {
                    Body("Nothing here.", Modifier.padding(vertical = 10.dp), size = 13.sp)
                } else {
                    items.forEachIndexed { i, t: TaskEntity ->
                        TaskRow(
                            title = t.title,
                            sub = t.tag,
                            trailing = t.dueLabel,
                            done = t.done,
                            onToggle = { vm.toggleTask(t) },
                            onClick = { vm.editTask = t },
                            divider = i < items.lastIndex,
                            accent = g.section == Sec.NOW && i == 0 && !t.done,
                        )
                    }
                }
            }
        }
    }
}
