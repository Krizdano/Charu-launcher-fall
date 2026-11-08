package com.lifeos.launcher.ui

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.apps.AppEntry

/** Popular players, so they are listed first even if they don't declare the music category. */
private val knownMusicPackages = setOf(
    "com.spotify.music",
    "com.google.android.apps.youtube.music",
    "com.apple.android.music",
    "com.amazon.mp3",
    "deezer.android.app",
    "com.soundcloud.android",
    "com.aspiro.tidal",
    "com.jio.media.jiobeats",
    "com.bsbportal.music",
    "com.gaana",
    "com.google.android.music",
    "com.sec.android.app.music",
    "com.miui.player",
    "org.videolan.vlc",
    "com.maxmpz.audioplayer",
    "in.krosbits.musicolet",
)

/** Choose which app opens when you tap the Now Playing card with nothing playing. */
@Composable
fun MusicAppPickerDialog(vm: MainViewModel) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    // apps that declare themselves as music players
    val declared = remember(ctx) {
        try {
            ctx.packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC), 0)
                .map { it.activityInfo.packageName }
                .toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val suggested = filtered.filter { it.pkg in knownMusicPackages || it.pkg in declared }
    val others = filtered.filter { it.pkg !in knownMusicPackages && it.pkg !in declared }

    val close = { vm.musicPickerOpen = false }
    val choose = { pkg: String? ->
        vm.setDefaultMusic(pkg)
        vm.musicPickerOpen = false
    }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SoftDim()
        Box(Modifier.fillMaxSize().tap { close() }.imePadding(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.padding(20.dp).widthIn(max = 480.dp).fillMaxWidth().heightIn(max = 620.dp)
                    .glass(26.dp, strong = true).tap {}.padding(20.dp),
            ) {
                DialogHeader("Music", "Choose your player.") { close() }
                LoField(query, { query = it }, "Search apps…")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f, fill = false)) {
                    if (query.isBlank()) {
                        item(key = "system-default") {
                            PickRow(null, "System default", vm.defaultMusicPkg == null) { choose(null) }
                        }
                    }
                    if (suggested.isNotEmpty()) {
                        item(key = "hdr-suggested") { Eyebrow("Music apps", Modifier.padding(top = 10.dp, bottom = 4.dp)) }
                        items(suggested, key = { "s-" + it.key }) { a ->
                            PickRow(a, a.label, vm.defaultMusicPkg == a.pkg) { choose(a.pkg) }
                        }
                    }
                    if (others.isNotEmpty()) {
                        item(key = "hdr-all") { Eyebrow("All apps", Modifier.padding(top = 10.dp, bottom = 4.dp)) }
                        items(others, key = { "a-" + it.key }) { a ->
                            PickRow(a, a.label, vm.defaultMusicPkg == a.pkg) { choose(a.pkg) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PillButton("Done", { close() }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PickRow(app: AppEntry?, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (app != null) {
            Image(app.icon, null, Modifier.size(40.dp))
        } else {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(C.Cherry.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MusicNote, null, tint = C.Cherry, modifier = Modifier.size(22.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label, Modifier.weight(1f), color = C.Ink, fontSize = 15.sp,
            fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        CheckCircle(selected, onClick, small = true)
    }
}
