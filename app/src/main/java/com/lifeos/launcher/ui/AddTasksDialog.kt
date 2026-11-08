package com.lifeos.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifeos.launcher.data.ProjectEntity
import com.lifeos.launcher.data.TaskEntity

/** Pick open tasks from your list and move them into [project]. */
@Composable
fun AddTasksToProjectDialog(
    vm: MainViewModel,
    project: ProjectEntity,
    tasks: List<TaskEntity>,
    projects: List<ProjectEntity>,
) {
    val names = projects.associate { it.id to it.title }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<Long>()) }
    val candidates = tasks.filter { !it.done && it.projectId != project.id }
    val list = candidates.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
    val close = { vm.addTasksProjectId = null }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SoftDim()
        Box(Modifier.fillMaxSize().tap { close() }.imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = 620.dp)
                    .glass(26.dp, strong = true).tap {}.padding(20.dp),
            ) {
                DialogHeader("Add to ${project.title}", "Choose tasks.") { close() }
                if (candidates.isEmpty()) {
                    Body("You have no other open tasks to add. Create some with the + button first.", Modifier.padding(vertical = 12.dp))
                } else {
                    LoField(query, { query = it }, "Search tasks…")
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(list, key = { it.id }) { t ->
                            val toggle = {
                                selected = if (t.id in selected) selected - t.id else selected + t.id
                            }
                            val where = names[t.projectId]?.let { "Currently in $it" } ?: t.tag
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { toggle() }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CheckCircle(t.id in selected, { toggle() }, small = true)
                                Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                                    Text(t.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Ink)
                                    if (where.isNotBlank()) Text(where, fontSize = 12.sp, color = C.Brown)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    when {
                        candidates.isEmpty() -> "Close"
                        selected.isEmpty() -> "Select tasks"
                        selected.size == 1 -> "Add 1 task"
                        else -> "Add ${selected.size} tasks"
                    },
                    {
                        if (selected.isNotEmpty()) {
                            vm.assignTasks(project, selected.toList())
                            close()
                        } else if (candidates.isEmpty()) {
                            close()
                        } else {
                            vm.toast("Pick at least one task")
                        }
                    },
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
