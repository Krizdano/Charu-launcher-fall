package com.lifeos.launcher.ui

import android.app.Application
import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lifeos.launcher.apps.AppEntry
import com.lifeos.launcher.apps.AppsRepo
import com.lifeos.launcher.data.AppDb
import com.lifeos.launcher.data.CalEvent
import com.lifeos.launcher.data.CalendarRepo
import com.lifeos.launcher.data.NoteEntity
import com.lifeos.launcher.data.Period
import com.lifeos.launcher.data.ProjectEntity
import com.lifeos.launcher.data.RoutineEntity
import com.lifeos.launcher.data.Sec
import com.lifeos.launcher.data.TaskEntity
import com.lifeos.launcher.media.MediaRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

enum class Tab(val label: String, val crumb: String, val icon: ImageVector) {
    Home("Home", "OVERVIEW", Icons.Rounded.Home),
    Tasks("Tasks", "TASKS", Icons.Rounded.TaskAlt),
    Routines("Routines", "ROUTINES", Icons.Rounded.Autorenew),
    Calendar("Calendar", "CALENDAR", Icons.Rounded.CalendarMonth),
    Music("Music", "MUSIC", Icons.Rounded.MusicNote),
    Projects("Projects", "PROJECTS", Icons.Rounded.Layers),
    Essentials("Essentials", "ESSENTIALS", Icons.Rounded.Star),
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDb.get(app)
    private val prefs = app.getSharedPreferences("lifeos", Context.MODE_PRIVATE)
    val media: MediaRepo = MediaRepo.get(app)

    // ---- persisted data ----
    val tasks = db.tasks().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val notes = db.notes().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val routines = db.routines().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val projects = db.projects().observe().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- UI state ----
    var tab by mutableStateOf(Tab.Home)
    var drawerOpen by mutableStateOf(false)
    var searchOpen by mutableStateOf(false)
    var taskQuery by mutableStateOf("")
    var captureOpen by mutableStateOf(false)
    var editTask by mutableStateOf<TaskEntity?>(null)
    var editRoutine by mutableStateOf<RoutineEntity?>(null)
    var editProject by mutableStateOf<ProjectEntity?>(null)
    var initials by mutableStateOf(prefs.getString("initials", "AK") ?: "AK")

    // ---- settings ----
    var settingsOpen by mutableStateOf(false)
    var glassOpacity by mutableFloatStateOf(prefs.getFloat("glass_opacity", 0.66f))
    var tabOrder by mutableStateOf(loadTabOrder())

    // ---- essentials (pinned apps) ----
    var pinned by mutableStateOf(loadPinned())
    var pickerOpen by mutableStateOf(false)

    var toastMsg by mutableStateOf<String?>(null)
    private var toastJob: Job? = null

    var now by mutableStateOf(LocalDateTime.now())
    val today: String get() = now.toLocalDate().toString()

    // ---- apps ----
    val apps: StateFlow<List<AppEntry>> get() = _apps
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())

    // ---- calendar ----
    var visibleMonth by mutableStateOf(YearMonth.now())
    var selectedDate by mutableStateOf(LocalDate.now())
    var calGranted by mutableStateOf(CalendarRepo.hasPermission(app))
    var monthEvents by mutableStateOf<Map<LocalDate, List<CalEvent>>>(emptyMap())
    var upcoming by mutableStateOf<CalEvent?>(null)

    // ---- focus timer ----
    var focusFor by mutableLongStateOf(-1L)
    var focusRemaining by mutableIntStateOf(0)
    var focusRunning by mutableStateOf(false)
    private var focusJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                now = LocalDateTime.now()
                delay(10_000)
            }
        }
        seedIfNeeded()
        refreshCalendar()
    }

    fun onResume() {
        now = LocalDateTime.now()
        loadApps()
        media.refresh()
        refreshCalendar()
    }

    fun onHomePressed() {
        settingsOpen = false
        drawerOpen = false
        captureOpen = false
        tab = Tab.Home
    }

    fun toast(msg: String) {
        toastMsg = msg
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2100)
            toastMsg = null
        }
    }

    fun updateInitials(v: String) {
        initials = v.trim().take(2).uppercase().ifBlank { "ME" }
        prefs.edit().putString("initials", initials).apply()
    }

    // ---------------- settings: transparency, page order ----------------
    fun setGlassOpacity(v: Float) {
        glassOpacity = v.coerceIn(0.15f, 1f)
        prefs.edit().putFloat("glass_opacity", glassOpacity).apply()
    }

    private fun loadTabOrder(): List<Tab> {
        val saved = prefs.getString("tab_order", null)
            ?.split(",")
            ?.mapNotNull { n -> Tab.entries.firstOrNull { it.name == n } }
            ?.distinct()
            .orEmpty()
        // any page added in a later version is appended so it is never lost
        return saved + Tab.entries.filter { it !in saved }
    }

    private fun saveTabOrder() {
        prefs.edit().putString("tab_order", tabOrder.joinToString(",") { it.name }).apply()
    }

    fun moveTab(tab: Tab, delta: Int) {
        val list = tabOrder.toMutableList()
        val i = list.indexOf(tab)
        val j = i + delta
        if (i < 0 || j !in list.indices) return
        val other = list[j]
        list[j] = tab
        list[i] = other
        tabOrder = list
        saveTabOrder()
    }

    fun resetTabOrder() {
        tabOrder = Tab.entries.toList()
        saveTabOrder()
    }

    // ---------------- essentials: pinned apps ----------------
    private fun loadPinned(): List<String> =
        prefs.getString("pinned_apps", "")?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

    fun isPinned(a: AppEntry): Boolean = a.key in pinned

    fun togglePin(a: AppEntry) {
        val nowPinned = a.key !in pinned
        pinned = if (nowPinned) pinned + a.key else pinned - a.key
        prefs.edit().putString("pinned_apps", pinned.joinToString("\n")).apply()
        toast(if (nowPinned) "Pinned to Essentials" else "Removed from Essentials")
    }

    // ---------------- seed ----------------
    private fun seedIfNeeded() {
        if (prefs.getBoolean("seeded", false)) return
        viewModelScope.launch(Dispatchers.IO) {
            val t = db.tasks()
            t.insert(TaskEntity(title = "Finish project report", tag = "Work", estimateMin = 25, section = Sec.NOW))
            t.insert(TaskEntity(title = "Plan tomorrow's tasks", tag = "Personal", estimateMin = 10, section = Sec.TODAY, dueLabel = "10:30 AM"))
            t.insert(TaskEntity(title = "Buy groceries", tag = "Errands", estimateMin = 20, section = Sec.TODAY, dueLabel = "12:00 PM"))
            t.insert(TaskEntity(title = "Reply to emails", tag = "Work", estimateMin = 15, section = Sec.TODAY, dueLabel = "2:00 PM"))
            t.insert(TaskEntity(title = "Book dentist appointment", tag = "Personal", section = Sec.UPCOMING, dueLabel = "Soon"))
            t.insert(TaskEntity(title = "Prepare presentation", tag = "Work", section = Sec.UPCOMING, dueLabel = "This week"))
            t.insert(TaskEntity(title = "Review design notes", tag = "Launcher v3", section = Sec.WORK))
            t.insert(TaskEntity(title = "Send weekly update", tag = "Admin", section = Sec.WORK))

            val d = LocalDate.now().toString()
            val r = db.routines()
            r.insert(RoutineEntity(title = "Drink water", subtitle = "Start the day", period = Period.MORNING, doneDate = d))
            r.insert(RoutineEntity(title = "Brush teeth", subtitle = "Morning", period = Period.MORNING, doneDate = d))
            r.insert(RoutineEntity(title = "Make bed", subtitle = "Reset the room", period = Period.MORNING))
            r.insert(RoutineEntity(title = "Take a proper break", subtitle = "Step away from the screen", period = Period.AFTERNOON, doneDate = d))
            r.insert(RoutineEntity(title = "Go for a walk", subtitle = "Fresh air", period = Period.AFTERNOON))
            r.insert(RoutineEntity(title = "Prepare tomorrow", subtitle = "Clothes, bag, plan", period = Period.EVENING))
            r.insert(RoutineEntity(title = "Wind down", subtitle = "Screens off", period = Period.EVENING))

            val p = db.projects()
            p.insert(ProjectEntity(title = "Launcher v3", subtitle = "Complete the native port", category = "WORK", colorIdx = 0, progress = 78))
            p.insert(ProjectEntity(title = "Learn Rust", subtitle = "Build a small practical project", category = "PERSONAL", colorIdx = 1, progress = 40))
            p.insert(ProjectEntity(title = "Reading list", subtitle = "Make room for curiosity", category = "PERSONAL", colorIdx = 2, progress = 60))

            prefs.edit().putBoolean("seeded", true).apply()
        }
    }

    // ---------------- tasks ----------------
    fun addTask(title: String, section: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.tasks().insert(TaskEntity(title = title.trim(), section = section))
        }
        toast("Task added")
    }

    fun saveTask(t: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (t.id == 0L) db.tasks().insert(t) else db.tasks().update(t)
        }
    }

    fun deleteTask(t: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.tasks().delete(t) }
        if (focusFor == t.id) stopFocus()
    }

    fun toggleTask(t: TaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.tasks().update(t.copy(done = !t.done)) }
        toast(if (!t.done) "Nice. One less thing to carry." else "Task reopened")
        if (!t.done && focusFor == t.id) stopFocus()
    }

    fun clearDoneTasks() {
        viewModelScope.launch(Dispatchers.IO) { db.tasks().clearDone() }
        toast("Cleared completed tasks")
    }

    fun completeFocus(t: TaskEntity) {
        stopFocus()
        viewModelScope.launch(Dispatchers.IO) { db.tasks().update(t.copy(done = true)) }
        toast("Nice work. Take a breath.")
    }

    // ---------------- notes ----------------
    fun addNote(text: String) {
        viewModelScope.launch(Dispatchers.IO) { db.notes().insert(NoteEntity(text = text.trim())) }
        toast("Saved to inbox")
    }

    fun deleteNote(n: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.notes().delete(n) }
    }

    // ---------------- routines ----------------
    fun toggleRoutine(r: RoutineEntity) {
        val done = r.doneDate == today
        viewModelScope.launch(Dispatchers.IO) {
            db.routines().update(r.copy(doneDate = if (done) "" else today))
        }
        toast(if (!done) "Routine checked off" else "Routine reopened")
    }

    fun saveRoutine(r: RoutineEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (r.id == 0L) db.routines().insert(r) else db.routines().update(r)
        }
    }

    fun deleteRoutine(r: RoutineEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.routines().delete(r) }
    }

    // ---------------- projects ----------------
    fun saveProject(p: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (p.id == 0L) db.projects().insert(p) else db.projects().update(p)
        }
    }

    fun deleteProject(p: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) { db.projects().delete(p) }
    }

    // ---------------- focus ----------------
    fun toggleFocus(t: TaskEntity) {
        if (focusRunning) {
            focusJob?.cancel()
            focusRunning = false
            return
        }
        if (focusFor != t.id || focusRemaining <= 0) {
            focusFor = t.id
            focusRemaining = (if (t.estimateMin > 0) t.estimateMin else 25) * 60
        }
        focusRunning = true
        toast("Focus session started")
        focusJob = viewModelScope.launch {
            while (focusRemaining > 0) {
                delay(1000)
                focusRemaining -= 1
            }
            focusRunning = false
            focusFor = -1L
            toast("Focus session finished")
        }
    }

    private fun stopFocus() {
        focusJob?.cancel()
        focusRunning = false
        focusFor = -1L
        focusRemaining = 0
    }

    // ---------------- apps ----------------
    fun loadApps() {
        viewModelScope.launch(Dispatchers.Default) {
            _apps.value = AppsRepo.load(getApplication())
        }
    }

    // ---------------- calendar ----------------
    fun gridStart(m: YearMonth): LocalDate {
        val first = m.atDay(1)
        return first.minusDays((first.dayOfWeek.value % 7).toLong())
    }

    fun refreshCalendar() {
        val ctx = getApplication<Application>()
        calGranted = CalendarRepo.hasPermission(ctx)
        if (!calGranted) {
            monthEvents = emptyMap()
            upcoming = null
            return
        }
        val month = visibleMonth
        viewModelScope.launch(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val start = gridStart(month)
            val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = start.plusDays(42).atStartOfDay(zone).toInstant().toEpochMilli()
            monthEvents = CalendarRepo.groupByDay(CalendarRepo.query(ctx, from, to), zone)
            val nowMs = System.currentTimeMillis()
            upcoming = CalendarRepo.query(ctx, nowMs, nowMs + 30L * 86_400_000L)
                .firstOrNull { it.end > nowMs }
        }
    }

    fun shiftMonth(delta: Long) {
        visibleMonth = visibleMonth.plusMonths(delta)
        selectedDate = if (visibleMonth == YearMonth.now()) LocalDate.now() else visibleMonth.atDay(1)
        refreshCalendar()
    }

    fun selectDate(d: LocalDate) {
        selectedDate = d
        val ym = YearMonth.from(d)
        if (ym != visibleMonth) {
            visibleMonth = ym
            refreshCalendar()
        }
    }
}
