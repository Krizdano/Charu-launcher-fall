package com.lifeos.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LifeOsApp(vm: MainViewModel) {
    CompositionLocalProvider(LocalGlassOpacity provides vm.glassOpacity) {
        LifeOsAppContent(vm)
    }
}

@Composable
private fun LifeOsAppContent(vm: MainViewModel) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val dialogOpen = vm.captureOpen || vm.editTask != null || vm.editRoutine != null ||
        vm.editProject != null || vm.pickerOpen || vm.musicPickerOpen || vm.addTasksProjectId != null

    // ---- paging: swipe left / right between pages, in the user's chosen order ----
    val order = vm.tabOrder
    val latestOrder by rememberUpdatedState(order)
    val pagerState = rememberPagerState(
        initialPage = order.indexOf(vm.tab).coerceAtLeast(0),
        pageCount = { order.size },
    )
    // tab chosen elsewhere (bottom bar, Home button, Back) -> scroll the pager there
    LaunchedEffect(vm.tab, order) {
        val idx = order.indexOf(vm.tab)
        if (idx >= 0 && idx != pagerState.targetPage) pagerState.animateScrollToPage(idx)
    }
    // user swiped -> remember which page we landed on
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { p ->
            latestOrder.getOrNull(p)?.let { if (vm.tab != it) vm.tab = it }
        }
    }

    // As the home screen, back should never close us.
    BackHandler(enabled = true) {
        when {
            vm.settingsOpen -> vm.settingsOpen = false
            vm.drawerOpen -> vm.drawerOpen = false
            vm.tab == Tab.Projects && vm.openProjectId != null -> vm.openProjectId = null
            vm.tab != Tab.Home -> vm.tab = Tab.Home
        }
    }

    Box(Modifier.fillMaxSize()) {
        CozyBackground()

        // Page + nav. Blurred (Android 12+) while a dialog is open; no-op on older versions.
        Box(Modifier.fillMaxSize().then(if (dialogOpen) Modifier.blur(14.dp) else Modifier)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(vm)
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    key = { order[it].name },
                ) { page ->
                    when (order[page]) {
                        Tab.Home -> HomeScreen(vm)
                        Tab.Tasks -> TasksScreen(vm)
                        Tab.Routines -> RoutinesScreen(vm)
                        Tab.Calendar -> CalendarScreen(vm)
                        Tab.Music -> MusicScreen(vm)
                        Tab.Projects -> ProjectsScreen(vm)
                        Tab.Essentials -> EssentialsScreen(vm)
                    }
                }
                BottomNav(vm, order)
            }

            // Floating quick-capture button (hidden on Music so it never covers the controls)
            if (vm.tab != Tab.Music) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = navInset + 86.dp)
                        .size(56.dp).softShadow(28.dp, dy = 5.dp, layers = 5)
                        .clip(CircleShape).background(C.Cherry)
                        .clickable(role = Role.Button) { vm.captureOpen = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "Quick capture", tint = C.Cream2, modifier = Modifier.size(28.dp)) }
            }
        }

        AnimatedVisibility(
            visible = vm.drawerOpen,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) { AppDrawer(vm) }

        AnimatedVisibility(
            visible = vm.settingsOpen,
            enter = slideInVertically { it / 3 } + fadeIn(),
            exit = slideOutVertically { it / 3 } + fadeOut(),
        ) { SettingsScreen(vm) }

        // Toast sits above every overlay
        AnimatedVisibility(
            visible = vm.toastMsg != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navInset + 152.dp),
            enter = fadeIn(), exit = fadeOut(),
        ) {
            Box(Modifier.clip(CircleShape).background(C.Ink).padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text(vm.toastMsg ?: "", color = C.Cream2, fontSize = 14.sp)
            }
        }
    }

    if (vm.captureOpen) CaptureDialog(vm)
    if (vm.pickerOpen) AppPickerDialog(vm)
    if (vm.musicPickerOpen) MusicAppPickerDialog(vm)
    vm.addTasksProjectId?.let { id ->
        projects.firstOrNull { it.id == id }?.let { AddTasksToProjectDialog(vm, it, allTasks, projects) }
    }

    vm.editTask?.let { t ->
        val del: (() -> Unit)? = if (t.id != 0L) ({ vm.deleteTask(t); vm.editTask = null }) else null
        TaskDialog(t, { vm.saveTask(it); vm.editTask = null }, del, { vm.editTask = null }, projects)
    }
    vm.editRoutine?.let { r ->
        val del: (() -> Unit)? = if (r.id != 0L) ({ vm.deleteRoutine(r); vm.editRoutine = null }) else null
        RoutineDialog(r, { vm.saveRoutine(it); vm.editRoutine = null }, del, { vm.editRoutine = null })
    }
    vm.editProject?.let { p ->
        val del: (() -> Unit)? = if (p.id != 0L) ({ vm.deleteProject(p); vm.editProject = null }) else null
        ProjectDialog(p, { vm.saveProject(it); vm.editProject = null }, del, { vm.editProject = null })
    }
}

@Composable
private fun TopBar(vm: MainViewModel) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "LIFE.OS", Modifier.weight(1f), color = C.Brown, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
        )
        RoundIconButton(Icons.Rounded.Apps, { vm.drawerOpen = true }, size = 40.dp, desc = "All apps")
        Spacer(Modifier.width(8.dp))
        // profile initials -> opens Settings
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(C.Cherry)
                .clickable(role = Role.Button) { vm.settingsOpen = true }
                .semantics { contentDescription = "Open settings" },
            contentAlignment = Alignment.Center,
        ) { Text(vm.initials, color = C.Cream2, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun BottomNav(vm: MainViewModel, order: List<Tab>) {
    val shape = RoundedCornerShape(30.dp)
    // bar transparency follows the Settings slider, but never so clear that the icons lose contrast
    val k = ((LocalGlassOpacity.current - 0.15f) / 0.85f).coerceIn(0f, 1f)
    val aTop = (0.60f + 0.54f * k).coerceAtMost(0.98f)
    val aBottom = (aTop + 0.02f).coerceAtMost(0.98f)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 10.dp).navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(60.dp)
                .softShadow(30.dp, dy = 5.dp, layers = 5)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(C.NavTop.copy(alpha = aTop), C.NavBottom.copy(alpha = aBottom))))
                .border(1.dp, Color.White.copy(alpha = 0.18f), shape)
                // swipe up anywhere on the bar to open the app drawer
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -40f) vm.drawerOpen = true },
                        onDragCancel = {},
                        onVerticalDrag = { _, dy -> total += dy },
                    )
                }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            order.forEach { t ->
                val active = vm.tab == t
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.widthIn(max = 46.dp).fillMaxWidth().height(42.dp).clip(CircleShape)
                            .background(if (active) C.NavActive else Color.Transparent)
                            .clickable(role = Role.Tab) { vm.tab = t },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            t.icon, t.label,
                            tint = if (active) Color.White else C.NavIcon.copy(alpha = 0.78f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}
