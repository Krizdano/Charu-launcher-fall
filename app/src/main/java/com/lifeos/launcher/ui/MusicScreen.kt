package com.lifeos.launcher.ui

import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import kotlinx.coroutines.delay

@Composable
fun MusicScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val np by vm.media.state.collectAsStateWithLifecycle()
    val hasAccess by vm.media.hasAccess.collectAsStateWithLifecycle()
    val tick by produceState(SystemClock.elapsedRealtime(), np?.playing) {
        while (true) {
            value = SystemClock.elapsedRealtime()
            delay(500)
        }
    }
    val track = np

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
        },
        bottom = { Deco(R.drawable.deco_scatter, Modifier.fillMaxWidth().height(44.dp), Alignment.BottomCenter) },
    ) {
        PageHeader("Music", "Play. Relax. Repeat.") {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.GraphicEq, null, tint = if (track?.playing == true) C.Cherry else C.Brown, modifier = Modifier.size(26.dp))
            }
        }

        if (!hasAccess) {
            GlassCard(strong = true) {
                Eyebrow("Media access", color = C.Cherry)
                Spacer(Modifier.height(6.dp))
                Heading("Connect your music app.", size = 22.sp)
                Spacer(Modifier.height(6.dp))
                Body(
                    "Turn on notification access for life.os so it can see and control the song playing in " +
                        "Spotify, YouTube Music and other players. Nothing leaves your phone.",
                )
                Spacer(Modifier.height(14.dp))
                PillButton("Enable media access", { ctx.safeStart(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
                Spacer(Modifier.height(10.dp))
                Body(
                    "Sideloaded on Android 13+? If the switch is greyed out, open App info → ⋮ → Allow restricted settings first.",
                    size = 12.sp,
                )
            }
        }

        // illustrated autumn scene (the artwork is the star of this page)
        Box(
            Modifier.fillMaxWidth().aspectRatio(320f / 220f)
                .softShadow(22.dp).clip(RoundedCornerShape(22.dp))
                .border(1.dp, C.Hairline, RoundedCornerShape(22.dp)),
        ) {
            Deco(R.drawable.ill_record_scene, Modifier.matchParentSize())
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Heading(
                track?.title ?: "Nothing playing", size = 27.sp, align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            Body(
                track?.artist?.ifBlank { null } ?: track?.appLabel ?: if (hasAccess) "Start something in your music app" else "—",
                size = 15.sp, color = C.Ink, align = TextAlign.Center,
            )
        }

        Column {
            val dur = track?.durationMs ?: 0L
            val pos = track?.position(tick) ?: 0L
            LoProgress(if (dur > 0) pos / dur.toFloat() else 0f, height = 5.dp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtMs(pos), color = C.Brown, fontSize = 12.sp)
                Text(if (dur > 0) fmtMs(dur) else "--:--", color = C.Brown, fontSize = 12.sp)
            }
        }

        Row(
            Modifier.fillMaxWidth().alpha(if (track != null) 1f else 0.45f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(Icons.Rounded.Replay10, { vm.media.seekBy(-10_000) }, size = 48.dp, bg = androidx.compose.ui.graphics.Color.Transparent, desc = "Back 10 seconds")
            RoundIconButton(Icons.Rounded.SkipPrevious, { vm.media.previous() }, size = 52.dp, bg = androidx.compose.ui.graphics.Color.Transparent, desc = "Previous track")
            RoundIconButton(
                if (track?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                { vm.media.playPause() }, size = 68.dp, bg = C.Cherry, fg = C.Cream2,
                desc = if (track?.playing == true) "Pause" else "Play",
            )
            RoundIconButton(Icons.Rounded.SkipNext, { vm.media.next() }, size = 52.dp, bg = androidx.compose.ui.graphics.Color.Transparent, desc = "Next track")
            RoundIconButton(Icons.Rounded.Forward10, { vm.media.seekBy(10_000) }, size = 48.dp, bg = androidx.compose.ui.graphics.Color.Transparent, desc = "Forward 10 seconds")
        }

        if (track != null) {
            GlassCard(padding = 12.dp, radius = 20.dp) {
                Eyebrow("Playing from", color = C.Ink)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(C.Peach.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val art = track.art
                        if (art != null) {
                            val img = remember(art) { art.asImageBitmap() }
                            Image(img, null, Modifier.size(46.dp), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Rounded.MusicNote, null, tint = C.Cherry, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.appLabel, fontSize = 13.sp, color = C.Brown, maxLines = 1)
                    }
                    PillButton("Open", {
                        ctx.packageManager.getLaunchIntentForPackage(track.pkg)?.let { ctx.safeStart(it) }
                    }, bg = C.Cherry)
                }
            }
        }
    }
}
