package dev.asciiwall.app.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlin.math.abs
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.asciiwall.app.R
import dev.asciiwall.app.core.Config
import dev.asciiwall.app.core.Palette
import dev.asciiwall.app.core.SceneCategory
import dev.asciiwall.app.core.SceneType
import dev.asciiwall.app.wall.Renderer
import dev.asciiwall.app.wall.systemAccent

val Mono = FontFamily.Monospace

@Composable
fun sceneLabel(s: SceneType): String = stringResource(sceneLabelRes(s))

@Composable
fun categoryLabel(category: Any): String =
    (category as? SceneCategory)?.let { stringResource(categoryLabelRes(it)) } ?: category.toString()

@Composable
fun presetName(c: Config): String {
    presetLabelRes(c.id)?.let { if (c.builtin) return stringResource(it) }
    return if (c.builtin) sceneLabel(c.scene) else c.name
}

@Composable
fun paletteLabel(p: Palette) = stringResource(
    when (p) {
        Palette.MONO -> R.string.pal_mono
        Palette.GREEN -> R.string.pal_green
        Palette.AMBER -> R.string.pal_amber
        Palette.NEON -> R.string.pal_neon
        Palette.FIRE -> R.string.pal_fire
        Palette.ICE -> R.string.pal_ice
        Palette.DYNAMIC -> R.string.pal_dynamic
    },
)

fun paletteColor(p: Palette, accent: Color): Color = when (p) {
    Palette.MONO -> Color(0xFFEDEDED)
    Palette.GREEN -> Color(0xFF2BFF6B)
    Palette.AMBER -> Color(0xFFFFB000)
    Palette.NEON -> Color(0xFFFF4DDA)
    Palette.FIRE -> Color(0xFFFF6A00)
    Palette.ICE -> Color(0xFF62B8FF)
    Palette.DYNAMIC -> accent
}

/**
 * Живое превью: тот же Renderer, что и у обоев, но в масштабе "экран телефона -> превью".
 * Рендер ограничен ~15 кадр/с и работает только пока composable на экране.
 */
@Composable
fun AsciiPreview(config: Config, modifier: Modifier = Modifier, screenWidthDp: Float = 400f) {
    val ctx = LocalContext.current
    val accent = remember { systemAccent(ctx) }
    val renderer = remember { Renderer() }
    var tick by remember { mutableLongStateOf(0L) }
    val dens = LocalDensity.current.density
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (now - last >= 66_000_000L) { last = now; tick++ }
            }
        }
    }
    var lastNs by remember { mutableLongStateOf(0L) }
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black)) {
        val w = size.width.toInt(); val h = size.height.toInt()
        if (w <= 0 || h <= 0) return@Canvas
        val scale = size.width / (screenWidthDp * dens)
        tick.let { }
        renderer.configure(config.copy(parallax = false), w, h, dens * scale, accent)
        val now = System.nanoTime()
        val dt = if (lastNs == 0L) 0.066f else ((now - lastNs) / 1e9f).coerceAtMost(0.15f)
        lastNs = now
        drawIntoCanvas { renderer.frame(it.nativeCanvas, dt, 0.5f) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueText: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onChange(!checked) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}

@Composable
fun ColorDot(color: Color, size: Dp = 14.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(50)).background(color).border(1.dp, Color(0x33808080), RoundedCornerShape(50)))
}

@Composable
private fun isDarkScheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.22f

/** Карточка секции: скруглённая, с тонкой рамкой и лёгкой тенью. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = if (dark) lerp(c.surface, c.primaryContainer, 0.20f) else lerp(c.surface, c.surfaceVariant, 0.28f),
        border = BorderStroke(1.dp, c.outlineVariant.copy(alpha = if (dark) 0.52f else 0.24f)),
        shadowElevation = 10.dp,
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), verticalArrangement = verticalArrangement, content = content)
    }
}

class NavItem(val label: String, val icon: ImageVector)

/** Нижняя навигация-«пилюля»: плавающая плашка с анимированным индикатором активного пункта. */
@Composable
fun PillNavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val dark = isDarkScheme()
    val shell = if (dark) c.surface.copy(alpha = 0.92f) else lerp(c.surface, c.surfaceVariant, 0.48f).copy(alpha = 0.96f)
    val indicator = if (dark) c.primaryContainer.copy(alpha = 0.85f) else lerp(c.primaryContainer, c.surface, 0.18f)
    val index by animateFloatAsState(selected.toFloat(), tween(720, easing = CubicBezierEasing(0.2f, 0.9f, 0.24f, 1f)), label = "pill")
    val unselected = c.onSurfaceVariant.copy(alpha = 0.6f)

    BoxWithConstraints(
        modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        val pad = 8.dp
        val itemW = (maxWidth - pad * 2) / items.size
        Surface(
            shape = RoundedCornerShape(28.dp), color = shell,
            border = BorderStroke(1.dp, if (dark) c.outlineVariant.copy(alpha = 0.42f) else c.outline.copy(alpha = 0.16f)),
            shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth(),
        ) {
            Box(Modifier.fillMaxWidth().height(72.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp), color = indicator,
                    modifier = Modifier.offset(x = pad + itemW * index).padding(vertical = 6.dp).width(itemW).fillMaxHeight(),
                ) {}
                Row(Modifier.fillMaxSize().padding(horizontal = pad, vertical = 6.dp)) {
                    items.forEachIndexed { i, item ->
                        val emphasis = (1f - abs(i - index)).coerceIn(0f, 1f)
                        val tint = lerp(unselected, c.primary, emphasis)
                        Column(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).clickable { onSelect(i) },
                            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(item.icon, item.label, Modifier.size(22.dp), tint = tint)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1,
                                fontWeight = if (emphasis > 0.55f) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}
