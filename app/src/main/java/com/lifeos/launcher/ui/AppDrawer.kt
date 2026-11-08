package com.lifeos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.apps.AppEntry
import com.lifeos.launcher.apps.AppsRepo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(vm: MainViewModel) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val list = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    fun open(a: AppEntry) {
        AppsRepo.launch(ctx, a)
        vm.drawerOpen = false
    }

    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } },
    ) {
        CozyBackground()
        Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
            // header (drag down to close)
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp).pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 60f) vm.drawerOpen = false },
                        onDragCancel = {},
                        onVerticalDrag = { _, dy -> total += dy },
                    )
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Heading("All apps", size = 34.sp)
                    Body("${apps.size} installed", size = 14.sp, color = C.Ink)
                }
                RoundIconButton(Icons.Rounded.KeyboardArrowDown, { vm.drawerOpen = false }, desc = "Close app drawer")
            }

            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps", color = C.Brown) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = C.Brown) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Fonts.sans, fontSize = 16.sp, color = C.Ink),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.CherrySoft, unfocusedBorderColor = C.Hairline,
                    focusedContainerColor = C.GlassTint.copy(alpha = glassAlpha(0.7f)), unfocusedContainerColor = C.GlassTint.copy(alpha = glassAlpha(0.55f)),
                    focusedTextColor = C.Ink, unfocusedTextColor = C.Ink, cursorColor = C.Cherry,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { list.firstOrNull()?.let { open(it) } }),
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(list, key = { it.pkg + "/" + it.cls }) { a ->
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .combinedClickable(onClick = { open(a) }, onLongClick = { menu = true })
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(a.icon, a.label, Modifier.size(52.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                a.label, color = C.Ink, fontSize = 12.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (vm.isPinned(a)) "Unpin from Essentials" else "Pin to Essentials") },
                                onClick = { menu = false; vm.togglePin(a) },
                            )
                            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; AppsRepo.appInfo(ctx, a) })
                            DropdownMenuItem(text = { Text("Uninstall") }, onClick = { menu = false; AppsRepo.uninstall(ctx, a) })
                        }
                    }
                }
            }
        }
    }
}
