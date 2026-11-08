package com.lifeos.launcher.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifeos.launcher.R

// ------------------------------------------------------------------ typography
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = C.Brown) {
    Text(
        text.uppercase(), modifier = modifier, color = color, fontFamily = Fonts.sans,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp,
    )
}

/** Playfair Display heading. */
@Composable
fun Heading(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 24.sp,
    color: Color = C.Ink,
    weight: FontWeight = FontWeight.Medium,
    align: TextAlign? = null,
) {
    Text(
        text, modifier = modifier, color = color, fontFamily = Fonts.display, fontSize = size,
        fontWeight = weight, lineHeight = size * 1.15f, textAlign = align,
    )
}

@Composable
fun Body(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 14.sp,
    color: Color = C.Brown,
    align: TextAlign? = null,
) {
    Text(text, modifier = modifier, color = color, fontSize = size, lineHeight = size * 1.45f, textAlign = align)
}

// ------------------------------------------------------------------ surfaces
/** User-controlled opacity of translucent surfaces (Settings → Glass transparency). */
val LocalGlassOpacity = compositionLocalOf { DEFAULT_GLASS_OPACITY }
const val DEFAULT_GLASS_OPACITY = 0.66f

/** Scales a design alpha by the user's glass-opacity setting (default setting leaves it unchanged). */
@Composable
fun glassAlpha(base: Float): Float =
    (base * LocalGlassOpacity.current / DEFAULT_GLASS_OPACITY).coerceIn(0.03f, 0.98f)

/** Soft warm shadow drawn only OUTSIDE the shape, so translucent cards stay clean inside. */
fun Modifier.softShadow(
    radius: Dp,
    dy: Dp = 6.dp,
    color: Color = C.ShadowBase,
    layers: Int = 7,
): Modifier = this.drawBehind {
    val r = radius.toPx()
    val hole = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r))) }
    clipPath(hole, ClipOp.Difference) {
        for (i in 1..layers) {
            val g = i * 2.4.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = 0.055f * (1f - (i - 1f) / layers)),
                topLeft = Offset(-g, -g + dy.toPx()),
                size = Size(size.width + 2 * g, size.height + 2 * g),
                cornerRadius = CornerRadius(r + g, r + g),
            )
        }
    }
}

/**
 * Frosted-glass surface: warm translucent gradient, light top border, soft shadow.
 * (True backdrop blur isn't available in Compose for individual cards; the layered translucency
 * lets the illustrated background bleed through. Dialogs additionally blur the page on Android 12+.)
 */
fun Modifier.glass(radius: Dp = 22.dp, strong: Boolean = false, tint: Color = C.GlassTint): Modifier = composed {
    val shape = RoundedCornerShape(radius)
    val a1 = glassAlpha(if (strong) 0.82f else 0.66f)
    val a2 = glassAlpha(if (strong) 0.66f else 0.50f)
    this
        .softShadow(radius)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = a1), tint.copy(alpha = a2))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), C.Hairline)), shape)
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    strong: Boolean = false,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var m = modifier.fillMaxWidth().glass(radius, strong)
    if (onClick != null) m = m.clickable(role = Role.Button, onClick = onClick)
    Column(m.padding(padding), content = content)
}

@Composable
fun LoProgress(frac: Float, modifier: Modifier = Modifier, color: Color = C.Cherry, height: Dp = 6.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(Color(0x33876B5B))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).clip(CircleShape).background(color))
    }
}

@Composable
fun GlassDivider() = HorizontalDivider(color = C.Hairline, thickness = 1.dp)

// ------------------------------------------------------------------ buttons / chips
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = C.Cherry,
    fg: Color = C.Cream2,
    icon: ImageVector? = null,
) {
    Row(
        modifier.heightIn(min = 48.dp).softShadow(24.dp, dy = 4.dp, layers = 4)
            .clip(CircleShape).background(bg).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (icon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    bg: Color? = null,
    fg: Color = C.Ink,
    desc: String? = null,
) {
    val base = if (bg == null) Modifier.size(size).glass(size / 2) else Modifier.size(size).clip(CircleShape).background(bg)
    Box(
        modifier.then(base).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = fg, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun FilterPill(label: String, active: Boolean, onClick: () -> Unit) {
    val m = if (active) Modifier.clip(CircleShape).background(C.Cherry)
    else Modifier.glass(18.dp)
    Box(
        m.heightIn(min = 36.dp).clickable(role = Role.Tab, onClick = onClick).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (active) C.Cream2 else C.Ink)
    }
}

@Composable
fun CategoryPill(text: String, bg: Color, fg: Color = C.Ink) {
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text.uppercase(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun CheckCircle(done: Boolean, onClick: () -> Unit, small: Boolean = false) {
    val d = if (small) 18.dp else 22.dp
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(d).clip(CircleShape)
                .background(if (done) C.Cherry else Color.Transparent)
                .border(1.5.dp, if (done) C.Cherry else C.Brown.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, null, tint = C.Cream2, modifier = Modifier.size(d * 0.7f))
        }
    }
}

@Composable
fun TaskRow(
    title: String,
    sub: String,
    trailing: String?,
    done: Boolean,
    onToggle: () -> Unit,
    onClick: (() -> Unit)?,
    divider: Boolean = true,
    accent: Boolean = false,
) {
    Column {
        var m = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        if (onClick != null) m = m.clickable(onClick = onClick)
        Box {
            Row(m, verticalAlignment = Alignment.CenterVertically) {
                CheckCircle(done, onToggle)
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(
                        title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                        color = if (done) C.Brown else C.Ink,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                    )
                    if (sub.isNotBlank()) Text(sub, fontSize = 12.sp, color = C.Brown)
                }
                if (!trailing.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(trailing, fontSize = 11.sp, color = C.Brown)
                }
            }
            if (accent) {
                Box(
                    Modifier.align(Alignment.CenterStart).padding(start = 0.dp).width(3.dp).fillMaxHeight(0.62f)
                        .clip(RoundedCornerShape(2.dp)).background(C.Cherry),
                )
            }
        }
        if (divider) GlassDivider()
    }
}

@Composable
fun SectionLabel(title: String, count: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(title, Modifier.weight(1f), color = C.Ink)
        Text(count, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = C.Ink)
    }
    GlassDivider()
}

@Composable
fun PageHeader(title: String, sub: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Heading(title, size = 40.sp, weight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Body(sub, size = 15.sp, color = C.Ink)
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { trailing() }
        }
    }
}

// ------------------------------------------------------------------ backgrounds & illustrations
@Composable
fun CozyBackground(modifier: Modifier = Modifier) {
    val grain: ImageBitmap = ImageBitmap.imageResource(R.drawable.paper_grain)
    val grainBrush = remember(grain) { ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated)) }
    Box(
        modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(C.Cream2, C.Cream, Color(0xFFEEC9A6))))
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(C.Honey.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.04f), radius = size.width * 0.9f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(C.Peach.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.98f), radius = size.width * 0.9f,
                    ),
                )
                drawRect(grainBrush)
            },
    )
}

/** Decorative illustration: hidden from accessibility services. */
@Composable
fun Deco(@DrawableRes id: Int, modifier: Modifier = Modifier, alignment: Alignment = Alignment.Center) {
    Image(painterResource(id), contentDescription = null, modifier = modifier, alignment = alignment, contentScale = ContentScale.Fit)
}

/**
 * Standard scrolling page. [decor] is drawn behind the content (never scrolls);
 * [bottom] is pinned to the bottom of the page when content is short, and follows the content when long.
 */
@Composable
fun PageScroll(
    modifier: Modifier = Modifier,
    decor: @Composable BoxScope.() -> Unit = {},
    bottom: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        decor()
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = h),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        Modifier.padding(horizontal = 18.dp).padding(top = 2.dp, bottom = 76.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        content = content,
                    )
                    Box(Modifier.fillMaxWidth()) { bottom() }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ helpers
fun mmss(totalSeconds: Int): String = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

fun fmtMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
fun rememberBattery(): Int {
    val ctx = LocalContext.current
    var pct by remember {
        mutableIntStateOf(batteryPct(ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))))
    }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { pct = batteryPct(i) }
        }
        ctx.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { ctx.unregisterReceiver(r) }
    }
    return pct
}

private fun batteryPct(i: Intent?): Int {
    val l = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val s = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    return if (l < 0 || s <= 0) -1 else l * 100 / s
}

fun Context.safeStart(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

/** Like [safeStart] but reports whether an activity was actually started. */
fun Context.tryStart(intent: Intent): Boolean = try {
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    true
} catch (_: Exception) {
    false
}
