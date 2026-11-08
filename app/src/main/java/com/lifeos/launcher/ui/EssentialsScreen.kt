package com.lifeos.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.apps.AppEntry
import com.lifeos.launcher.apps.AppsRepo

@Composable
fun EssentialsScreen(vm: MainViewModel) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val pinnedApps = vm.pinned.mapNotNull { k -> apps.firstOrNull { it.key == k } }

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_bl, Modifier.align(Alignment.BottomStart).offset(x = (-36).dp, y = 28.dp).size(190.dp, 170.dp))
        },
        bottom = {
            Box(Modifier.fillMaxWidth().height(110.dp)) {
                Deco(R.drawable.ill_acorns, Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp).size(126.dp, 90.dp))
            }
        },
    ) {
        PageHeader("Essentials", "Your go-to apps.") {
            RoundIconButton(Icons.Rounded.Add, { vm.pickerOpen = true }, desc = "Add apps")
        }

        if (pinnedApps.isEmpty()) {
            GlassCard(strong = true) {
                Heading("Pin your essentials", size = 24.sp)
                Spacer(Modifier.height(6.dp))
                Body("Keep the apps you reach for every day one tap away. You can also long-press any app in the app drawer to pin it.")
                Spacer(Modifier.height(14.dp))
                PillButton("Choose apps", { vm.pickerOpen = true })
            }
        } else {
            GlassCard(padding = 14.dp) {
                pinnedApps.chunked(4).forEachIndexed { rowIdx, row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { a -> PinnedCell(vm, a, Modifier.weight(1f)) }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    if (rowIdx < (pinnedApps.size - 1) / 4) Spacer(Modifier.height(8.dp))
                }
            }
            Body("Tap to open · long-press to unpin", size = 12.sp, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedCell(vm: MainViewModel, a: AppEntry, modifier: Modifier) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = { AppsRepo.launch(ctx, a) }, onLongClick = { menu = true })
                .padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(a.icon, a.label, Modifier.size(56.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                a.label, color = C.Ink, fontSize = 12.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("Unpin from Essentials") }, onClick = { menu = false; vm.togglePin(a) })
            DropdownMenuItem(text = { Text("App info") }, onClick = { menu = false; AppsRepo.appInfo(ctx, a) })
        }
    }
}

/** Searchable list of every installed app; tap a row to pin / unpin it. */
@Composable
fun AppPickerDialog(vm: MainViewModel) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val list = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val close = { vm.pickerOpen = false }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SoftDim()
        Box(Modifier.fillMaxSize().tap { close() }.imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = 620.dp)
                    .glass(26.dp, strong = true).tap {}.padding(20.dp),
            ) {
                DialogHeader("Essentials", "Choose your apps.") { close() }
                LoField(query, { query = it }, "Search apps…")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    items(list, key = { it.key }) { a ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { vm.togglePin(a) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(a.icon, null, Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                a.label, Modifier.weight(1f), color = C.Ink, fontSize = 15.sp,
                                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            CheckCircle(vm.isPinned(a), { vm.togglePin(a) }, small = true)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton("Done", { close() }, Modifier.fillMaxWidth())
            }
        }
    }
}
