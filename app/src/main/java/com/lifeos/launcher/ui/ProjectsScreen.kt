package com.lifeos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.data.ProjectEntity
import com.lifeos.launcher.data.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ------------------------------------------------------------------ helpers
/** Progress of a project, derived from the tasks that belong to it. */
internal data class ProjectProgress(val done: Int, val total: Int, val frac: Float)

internal fun projectProgress(p: ProjectEntity, all: List<TaskEntity>): ProjectProgress {
    val mine = all.filter { it.projectId == p.id }
    val d = mine.count { it.done }
    val frac = when {
        p.done -> 1f
        mine.isEmpty() -> 0f
        else -> d / mine.size.toFloat()
    }
    return ProjectProgress(d, mine.size, frac)
}

internal fun prettyDate(iso: String): String = try {
    LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
} catch (_: Exception) {
    iso
}

/** Short deadline text for a project and whether it is overdue; null when there is no deadline. */
internal fun dueInfo(p: ProjectEntity): Pair<String, Boolean>? {
    if (p.dueDate.isBlank()) return null
    val d = try { LocalDate.parse(p.dueDate) } catch (_: Exception) { return null }
    if (p.done) return "Was due ${prettyDate(p.dueDate)}" to false
    val days = ChronoUnit.DAYS.between(LocalDate.now(), d)
    return when {
        days < 0 -> (if (days == -1L) "Overdue by 1 day" else "Overdue by ${-days} days") to true
        days == 0L -> "Due today" to false
        days == 1L -> "Due tomorrow" to false
        days < 14 -> "Due in $days days" to false
        else -> "Due ${d.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))}" to false
    }
}

private fun projectColor(p: ProjectEntity) = C.ProjectColors[p.colorIdx.coerceIn(0, C.ProjectColors.lastIndex)]

// ------------------------------------------------------------------ screen
@Composable
fun ProjectsScreen(vm: MainViewModel) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val open = vm.openProjectId?.let { id -> projects.firstOrNull { it.id == id } }

    if (open != null) ProjectDetail(vm, open, tasks.filter { it.projectId == open.id })
    else ProjectList(vm, projects, tasks)
}

// ------------------------------------------------------------------ list
@Composable
private fun ProjectList(vm: MainViewModel, projects: List<ProjectEntity>, tasks: List<TaskEntity>) {
    var filter by rememberSaveable { mutableStateOf("active") }
    val shown = projects
        .filter { filter == "all" || (filter == "done") == it.done }
        .sortedWith(compareBy<ProjectEntity>({ it.done }, { it.dueDate.isBlank() }, { it.dueDate }, { it.id }))
    val newProject = { vm.editProject = ProjectEntity(title = "") }

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_bl, Modifier.align(Alignment.BottomStart).offset(x = (-36).dp, y = 28.dp).size(190.dp, 170.dp))
        },
        bottom = {
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                Deco(R.drawable.ill_cottage, Modifier.align(Alignment.BottomEnd).offset(x = (-8).dp).size(190.dp, 142.dp))
            }
        },
    ) {
        PageHeader("Projects", "Bigger goals.") {
            RoundIconButton(Icons.Rounded.Add, newProject, desc = "New project")
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("active" to "Active", "done" to "Completed", "all" to "All").forEach { (k, l) ->
                FilterPill(l, filter == k) { filter = k }
            }
        }

        if (shown.isEmpty()) {
            GlassCard(strong = true) {
                Heading(if (projects.isEmpty()) "Start your first project" else "Nothing here yet", size = 24.sp)
                Spacer(Modifier.height(6.dp))
                Body(
                    if (projects.isEmpty()) "Break a big goal into tasks and watch the progress add up."
                    else if (filter == "done") "Completed projects will show up here."
                    else "All your projects are complete. Start something new.",
                )
                Spacer(Modifier.height(14.dp))
                PillButton("New project", newProject)
            }
        }

        shown.forEach { p ->
            ProjectCard(p, projectProgress(p, tasks)) { vm.openProjectId = p.id }
        }

        if (shown.isNotEmpty()) {
            GlassCard(radius = 20.dp, padding = 14.dp, onClick = newProject) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Rounded.Add, null, tint = C.Ink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("New Project", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Ink)
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(p: ProjectEntity, prog: ProjectProgress, onClick: () -> Unit) {
    val color = projectColor(p)
    val due = dueInfo(p)
    GlassCard(Modifier.alpha(if (p.done) 0.75f else 1f), padding = 16.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(p.category, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            if (p.done) {
                CategoryPill("Done", C.OliveTint)
                Spacer(Modifier.width(6.dp))
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = C.Brown, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(p.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
        if (p.subtitle.isNotBlank()) Body(p.subtitle, Modifier.padding(top = 2.dp), size = 14.sp, color = C.Ink)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LoProgress(prog.frac, Modifier.weight(1f), color = color, height = 6.dp)
            Spacer(Modifier.width(12.dp))
            Text("${(prog.frac * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.Ink)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (prog.total == 0) "No tasks yet" else "${prog.done} of ${prog.total} tasks done",
                Modifier.weight(1f), fontSize = 12.sp, color = C.Brown,
            )
            if (due != null) {
                Text(due.first, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (due.second) C.Cherry else C.Brown)
            }
        }
    }
}

// ------------------------------------------------------------------ detail
@Composable
private fun ProjectDetail(vm: MainViewModel, p: ProjectEntity, tasks: List<TaskEntity>) {
    val color = projectColor(p)
    val prog = projectProgress(p, tasks)
    val todo = tasks.filter { !it.done }
    val finished = tasks.filter { it.done }
    val due = dueInfo(p)
    var newTask by remember(p.id) { mutableStateOf("") }
    val submit = {
        if (newTask.isNotBlank()) {
            vm.addProjectTask(p, newTask)
            newTask = ""
        }
    }

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_bl, Modifier.align(Alignment.BottomStart).offset(x = (-36).dp, y = 28.dp).size(190.dp, 170.dp))
        },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.Rounded.ChevronLeft, { vm.openProjectId = null }, desc = "Back to projects")
            Spacer(Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.Edit, { vm.editProject = p }, desc = "Edit project")
        }

        Column(Modifier.padding(horizontal = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(11.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Eyebrow(p.category, color = C.Ink)
            }
            Spacer(Modifier.height(6.dp))
            Heading(p.title, size = 34.sp)
            if (p.subtitle.isNotBlank()) Body(p.subtitle, Modifier.padding(top = 4.dp), size = 15.sp, color = C.Ink)
            if (due != null) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = if (due.second) C.Cherry else C.Brown, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(due.first, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (due.second) C.Cherry else C.Brown)
                }
            }
        }

        GlassCard(strong = true, padding = 18.dp) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${(prog.frac * 100).toInt()}%",
                    fontFamily = Fonts.display, fontSize = 44.sp, fontWeight = FontWeight.Medium, color = color,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        p.done -> "Project complete"
                        prog.total == 0 -> "No tasks yet"
                        else -> "${prog.done} of ${prog.total} tasks done"
                    },
                    Modifier.weight(1f).padding(bottom = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.Ink,
                )
            }
            Spacer(Modifier.height(10.dp))
            LoProgress(prog.frac, color = color, height = 8.dp)
        }

        GlassCard(padding = 16.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LoField(newTask, { newTask = it }, "Add a task to this project…", Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                RoundIconButton(Icons.Rounded.Add, { submit() }, bg = C.Cherry, fg = C.Cream2, desc = "Add task")
            }

            SectionLabel("To do", todo.size.toString())
            if (todo.isEmpty()) {
                Body(
                    if (tasks.isEmpty()) "No tasks yet. Add the first step above." else "Everything is done.",
                    Modifier.padding(vertical = 10.dp), size = 13.sp,
                )
            }
            todo.forEachIndexed { i, t ->
                TaskRow(
                    title = t.title,
                    sub = if (t.estimateMin > 0) "${t.estimateMin} min" else "",
                    trailing = t.dueLabel,
                    done = false,
                    onToggle = { vm.toggleTask(t) },
                    onClick = { vm.editTask = t },
                    divider = i < todo.lastIndex,
                )
            }

            if (finished.isNotEmpty()) {
                SectionLabel("Done", finished.size.toString())
                finished.forEachIndexed { i, t ->
                    TaskRow(
                        title = t.title,
                        sub = "",
                        trailing = t.dueLabel,
                        done = true,
                        onToggle = { vm.toggleTask(t) },
                        onClick = { vm.editTask = t },
                        divider = i < finished.lastIndex,
                    )
                }
            }
        }

        if (!p.done && tasks.isNotEmpty() && todo.isEmpty()) {
            Body("Every task is done. Ready to wrap this one up?", size = 14.sp, color = C.Ink)
        }
        PillButton(
            if (p.done) "Reopen project" else "Mark project complete",
            { vm.setProjectDone(p, !p.done) },
            Modifier.fillMaxWidth(),
            bg = if (p.done) C.Olive else C.Cherry,
        )
    }
}
