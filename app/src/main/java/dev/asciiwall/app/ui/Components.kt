package dev.asciiwall.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.asciiwall.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Компоненты перенесены из tg-ws-proxy-android: AppBackdrop (MainActivity.kt), FloatingToolbar.kt, заголовок секции (SettingsTab.kt).

private fun orbShape(points: Int, innerRatio: Float): Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = min(size.width, size.height) / 2f
    val inner = outer * innerRatio
    for (i in 0 until points * 2) {
        val a = (-PI / 2.0) + (i * PI / points)
        val r = if (i % 2 == 0) outer else inner
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private val OrbLarge: Shape = orbShape(18, 0.90f)
private val OrbMedium: Shape = orbShape(20, 0.92f)
private val OrbSmall: Shape = orbShape(16, 0.88f)

@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val dark = c.background.luminance() < 0.22f
    val base = remember(c.background, c.surface, c.surfaceVariant) {
        Brush.verticalGradient(
            if (dark) listOf(lerp(c.background, c.surface, 0.42f), c.background, lerp(c.surfaceVariant, c.background, 0.35f))
            else listOf(lerp(c.background, c.surface, 0.78f), c.background, lerp(c.surfaceVariant, c.background, 0.30f)),
        )
    }
    val topGlow = if (dark) c.primary.copy(alpha = 0.16f) else lerp(c.primary, c.primaryContainer, 0.72f).copy(alpha = 0.32f)
    val leftGlow = if (dark) c.tertiary.copy(alpha = 0.11f) else lerp(c.tertiary, c.secondaryContainer, 0.74f).copy(alpha = 0.24f)
    val bottomGlow = if (dark) c.primary.copy(alpha = 0.10f) else lerp(c.secondary, c.primaryContainer, 0.70f).copy(alpha = 0.22f)
    val outline = c.outlineVariant.copy(alpha = 0.26f)
    Box(modifier.fillMaxSize().background(base)) {
        Box(
            Modifier.align(Alignment.TopStart).offset(x = (-86).dp, y = (-126).dp).size(258.dp)
                .clip(OrbLarge).background(topGlow)
                .then(if (dark) Modifier else Modifier.border(1.dp, outline, OrbLarge)),
        )
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = (-44).dp, y = 28.dp).size(146.dp)
                .clip(OrbSmall).background(leftGlow)
                .then(if (dark) Modifier else Modifier.border(1.dp, outline.copy(alpha = 0.22f), OrbSmall)),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 62.dp, y = (-208).dp).size(198.dp)
                .clip(OrbMedium).background(bottomGlow)
                .then(if (dark) Modifier else Modifier.border(1.dp, outline.copy(alpha = 0.20f), OrbMedium)),
        )
    }
}

/** Заголовок секции как в настройках оригинала: иконка + titleSmall (primary) в строке 48dp, ниже разделитель. */
@Composable
fun SectionHeader(icon: Int, title: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}

@Composable
fun ThemeOption(icon: Int? = null, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, shape = RoundedCornerShape(24.dp),
        color = if (selected) c.primaryContainer else c.surface,
        contentColor = if (selected) c.primary else c.onSurface,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(18.dp), tint = if (selected) c.primary else c.onSurfaceVariant)
            Text(
                label, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) c.primary else c.onSurface,
            )
        }
    }
}

@Composable
fun PaletteCircle(p: AppPalette, selectedId: String, onClick: (String) -> Unit) {
    val selected = p.id == selectedId
    Box(
        Modifier.size(30.dp).clip(CircleShape).background(p.preview)
            .clickable { onClick(p.id) }
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
    )
}

@Composable
private fun ThemePanelContent(close: () -> Unit) {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    val divider = c.outlineVariant.copy(alpha = 0.5f)
    ThemeOption(R.drawable.ic_auto, stringResource(R.string.ui_theme_system), ThemeStore.mode == ThemeMode.SYSTEM) { ThemeStore.setMode(ctx, ThemeMode.SYSTEM); close() }
    ThemeOption(R.drawable.ic_light_mode, stringResource(R.string.ui_theme_light), ThemeStore.mode == ThemeMode.LIGHT) { ThemeStore.setMode(ctx, ThemeMode.LIGHT); close() }
    ThemeOption(R.drawable.ic_dark_mode, stringResource(R.string.ui_theme_dark), ThemeStore.mode == ThemeMode.DARK) { ThemeStore.setMode(ctx, ThemeMode.DARK); close() }
    ThemeOption(R.drawable.ic_dark_mode, stringResource(R.string.ui_theme_amoled), ThemeStore.mode == ThemeMode.AMOLED) { ThemeStore.setMode(ctx, ThemeMode.AMOLED); close() }
    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = divider)
    val supported = ThemeStore.dynamicSupported
    val dynOn = ThemeStore.dynamic && supported
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.ui_dynamic_colors), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
            color = c.onSurfaceVariant.copy(alpha = if (supported) 1f else 0.5f), modifier = Modifier.weight(1f),
        )
        Switch(dynOn, { ThemeStore.setDynamic(ctx, it) }, enabled = supported, modifier = Modifier.scale(0.8f))
    }
    AnimatedVisibility(!dynOn) {
        Column {
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = divider)
            Text(
                stringResource(R.string.ui_palette_title), style = MaterialTheme.typography.labelSmall,
                color = c.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
            )
            ChipRowCircles()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRowCircles() {
    val ctx = LocalContext.current
    FlowRow(
        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { AppPalettes.forEach { PaletteCircle(it, ThemeStore.palette) { id -> ThemeStore.setPalette(ctx, id) } } }
}

/** Карточка выбора темы в настройках: режим, палитры с кружками-превью, Material You. */
@Composable
fun ThemePickerCard() {
    val ctx = LocalContext.current
    val c = MaterialTheme.colorScheme
    AppCard {
        SectionHeader(R.drawable.ic_palette, stringResource(R.string.ui_theme))
        Text(stringResource(R.string.ui_mode), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ThemeOption(R.drawable.ic_auto, stringResource(R.string.ui_theme_system), ThemeStore.mode == ThemeMode.SYSTEM) { ThemeStore.setMode(ctx, ThemeMode.SYSTEM) }
            ThemeOption(R.drawable.ic_light_mode, stringResource(R.string.ui_theme_light), ThemeStore.mode == ThemeMode.LIGHT) { ThemeStore.setMode(ctx, ThemeMode.LIGHT) }
            ThemeOption(R.drawable.ic_dark_mode, stringResource(R.string.ui_theme_dark), ThemeStore.mode == ThemeMode.DARK) { ThemeStore.setMode(ctx, ThemeMode.DARK) }
            ThemeOption(R.drawable.ic_dark_mode, stringResource(R.string.ui_theme_amoled), ThemeStore.mode == ThemeMode.AMOLED) { ThemeStore.setMode(ctx, ThemeMode.AMOLED) }
        }
        SwitchRow(stringResource(R.string.ui_dynamic_colors), ThemeStore.dynamic && ThemeStore.dynamicSupported) { if (ThemeStore.dynamicSupported) ThemeStore.setDynamic(ctx, it) }
        if (!ThemeStore.dynamicSupported) Text(stringResource(R.string.ui_dynamic_colors_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
        AnimatedVisibility(!(ThemeStore.dynamic && ThemeStore.dynamicSupported)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.ui_palette_title), style = MaterialTheme.typography.labelMedium, color = c.onSurfaceVariant)
                ChipRowCircles()
            }
        }
        Text(stringResource(R.string.ui_theme_hint), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

/** Плавающая вкладка-палитра у края экрана с выезжающей панелью (как в оригинале). */
@Composable
fun FloatingToolbar(modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeightPx = remember(configuration.screenHeightDp, density) { with(density) { configuration.screenHeightDp.dp.toPx() } }
    val screenWidthPx = remember(configuration.screenWidthDp, density) { with(density) { configuration.screenWidthDp.dp.toPx() } }
    var offsetY by rememberSaveable { mutableFloatStateOf(-1f) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var tabHeightPx by remember { mutableFloatStateOf(0f) }
    var panelHeightPx by remember { mutableFloatStateOf(0f) }
    val tabW = 42.dp; val tabH = 52.dp; val panelW = 220.dp
    val tabWidthPx = with(density) { tabW.toPx() }
    val fallbackTabH = with(density) { tabH.toPx() }
    val edge = with(density) { 8.dp.toPx() }
    val safeTop = WindowInsets.safeDrawing.getTop(density).toFloat()
    val safeBottom = WindowInsets.safeDrawing.getBottom(density).toFloat()
    val tabHeight = maxOf(tabHeightPx, fallbackTabH)
    val floating = if (expanded && panelHeightPx > 0f) maxOf(tabHeight, panelHeightPx) else tabHeight
    val minY = safeTop + edge
    val maxY = (screenHeightPx - safeBottom - floating - edge).coerceAtLeast(minY)
    val defaultY = (screenHeightPx * 0.24f).coerceIn(minY, maxY)
    val targetX = screenWidthPx - tabWidthPx
    val tabX by animateFloatAsState(targetX, spring(stiffness = Spring.StiffnessLow), label = "tab_shift")
    LaunchedEffect(minY, maxY) { offsetY = if (offsetY < 0f) defaultY else offsetY.coerceIn(minY, maxY) }

    Box(modifier.fillMaxSize()) {
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier
                .offset { IntOffset(tabX.roundToInt(), offsetY.roundToInt()) }
                .onGloballyPositioned { tabHeightPx = it.size.height.toFloat() }
                .pointerInput(minY, maxY) {
                    detectDragGestures(onDrag = { change, drag -> change.consume(); offsetY = (offsetY + drag.y).coerceIn(minY, maxY) })
                },
            shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            shadowElevation = 6.dp, tonalElevation = 4.dp,
        ) {
            Box(Modifier.size(tabW, tabH), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_palette), stringResource(R.string.ui_theme), Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        AnimatedVisibility(
            visible = expanded, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.offset {
                val pw = with(density) { panelW.toPx() }
                val gap = with(density) { 8.dp.toPx() }
                IntOffset((targetX - pw - gap).roundToInt(), offsetY.roundToInt())
            },
        ) {
            Surface(
                Modifier.onGloballyPositioned { panelHeightPx = it.size.height.toFloat() },
                shape = RoundedCornerShape(32.dp), color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface, shadowElevation = 8.dp, tonalElevation = 4.dp,
            ) {
                Column(Modifier.padding(12.dp).width(panelW - 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.ui_theme), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                    ThemePanelContent { expanded = false }
                }
            }
        }
    }
}
