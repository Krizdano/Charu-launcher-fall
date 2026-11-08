package com.lifeos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.launcher.R
import kotlin.math.roundToInt

/** Full-screen settings, opened by tapping the profile initials. */
@Composable
fun SettingsScreen(vm: MainViewModel) {
    var initials by remember { mutableStateOf(vm.initials) }
    var saved by remember { mutableStateOf(false) }
    val transparency = ((1f - vm.glassOpacity) / 0.85f).coerceIn(0f, 1f)

    Box(
        Modifier.fillMaxSize()
            // swallow touches so nothing underneath the overlay reacts
            .pointerInput(Unit) { detectTapGestures { } }
            .pointerInput(Unit) { detectHorizontalDragGestures { _, _ -> } },
    ) {
        CozyBackground()
        Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))

        PageScroll(
            Modifier.statusBarsPadding().navigationBarsPadding(),
            bottom = { Deco(R.drawable.deco_scatter, Modifier.fillMaxWidth().height(44.dp), Alignment.BottomCenter) },
        ) {
            PageHeader("Settings", "Make it yours.") {
                RoundIconButton(Icons.Rounded.Close, { vm.settingsOpen = false }, desc = "Close settings")
            }

            // ---------------- profile ----------------
            GlassCard {
                Eyebrow("Profile", color = C.Ink)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(C.Cherry),
                        contentAlignment = Alignment.Center,
                    ) { Text(initials.ifBlank { "ME" }.uppercase(), color = C.Cream2, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    LoField(initials, { initials = it.take(2); saved = false }, "Initials (2 letters)", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    if (saved) "Saved" else "Save initials",
                    { vm.updateInitials(initials); saved = true },
                )
            }

            // ---------------- appearance ----------------
            GlassCard {
                Eyebrow("Appearance", color = C.Ink)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Glass transparency", Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = C.Ink)
                    Text("${(transparency * 100).roundToInt()}%", fontSize = 14.sp, color = C.Brown)
                }
                Body("Controls how see-through cards, chips, fields and the bottom bar are.", size = 13.sp)
                Slider(
                    value = transparency,
                    onValueChange = { vm.setGlassOpacity(1f - 0.85f * it) },
                    colors = SliderDefaults.colors(
                        thumbColor = C.Cherry, activeTrackColor = C.Cherry,
                        inactiveTrackColor = Color(0x33876B5B),
                    ),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Solid", fontSize = 12.sp, color = C.Brown)
                    Text("Clear", fontSize = 12.sp, color = C.Brown)
                }
                Spacer(Modifier.height(10.dp))
                // live preview over some leaves so the effect is easy to judge
                Box(Modifier.fillMaxWidth().height(96.dp)) {
                    Deco(R.drawable.leaf_orange, Modifier.align(Alignment.CenterStart).offset(x = 18.dp).size(84.dp))
                    Deco(R.drawable.leaf_rust, Modifier.align(Alignment.CenterEnd).offset(x = (-26).dp, y = 8.dp).size(72.dp))
                    GlassCard(Modifier.align(Alignment.Center).fillMaxWidth(0.78f), strong = true, radius = 18.dp, padding = 14.dp) {
                        Text("Preview", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
                        Text("This is how glass looks.", fontSize = 13.sp, color = C.Brown)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Reset to default", color = C.Cherry, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.heightIn(min = 44.dp).clip(CircleShape)
                        .clickable { vm.setGlassOpacity(DEFAULT_GLASS_OPACITY) }.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }

            // ---------------- page order ----------------
            GlassCard {
                Eyebrow("Page order", color = C.Ink)
                Spacer(Modifier.height(4.dp))
                Body("Sets the order of the bottom bar and of swiping left / right.", size = 13.sp)
                Spacer(Modifier.height(6.dp))
                val order = vm.tabOrder
                order.forEachIndexed { i, t ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(C.Cherry.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(t.icon, null, tint = C.Cherry, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text(t.label, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = C.Ink)
                        RoundIconButton(
                            Icons.Rounded.KeyboardArrowUp, { vm.moveTab(t, -1) },
                            modifier = Modifier.alpha(if (i == 0) 0.35f else 1f), size = 40.dp, desc = "Move ${t.label} up",
                        )
                        Spacer(Modifier.width(8.dp))
                        RoundIconButton(
                            Icons.Rounded.KeyboardArrowDown, { vm.moveTab(t, 1) },
                            modifier = Modifier.alpha(if (i == order.lastIndex) 0.35f else 1f), size = 40.dp, desc = "Move ${t.label} down",
                        )
                    }
                    if (i < order.lastIndex) GlassDivider()
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Reset to default order", color = C.Cherry, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.heightIn(min = 44.dp).clip(CircleShape)
                        .clickable { vm.resetTabOrder() }.padding(horizontal = 4.dp, vertical = 12.dp),
                )
            }
        }
    }
}
