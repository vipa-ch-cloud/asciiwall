package dev.asciiwall.app.ui

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.asciiwall.app.R
import dev.asciiwall.app.core.Config
import dev.asciiwall.app.core.Palette
import dev.asciiwall.app.core.Presets
import dev.asciiwall.app.core.SceneType
import dev.asciiwall.app.wall.*
import kotlin.math.roundToInt

private val NAV_RESERVE = 104.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AsciiTheme {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f)) {
                    Box(Modifier.fillMaxSize()) {
                        AppBackdrop(Modifier.matchParentSize())
                        Surface(Modifier.fillMaxSize(), color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) {
                            Box {
                                Root()
                                FloatingToolbar()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Все сцены как карточки: пресет, если он есть, иначе конфиг по умолчанию для сцены. */
private fun sceneConfigs(): List<Config> {
    val presets = Presets.all
    val extra = SceneType.entries.filter { s -> presets.none { it.scene == s } }
        .map { s -> Config(id = s.name.lowercase(), name = s.name.lowercase(), builtin = true, scene = s) }
    return presets + extra
}

@Composable
private fun Root() {
    val ctx = LocalContext.current
    val sp = remember { prefs(ctx) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var active by remember { mutableStateOf(loadActive(sp)) }
    var editing by remember { mutableStateOf(active) }
    val customs = remember { mutableStateListOf<Config>().apply { addAll(loadCustom(sp)) } }

    fun persist() = saveCustom(sp, customs.toList())
    fun apply(c: Config) {
        saveActive(sp, c); active = c
        Toast.makeText(ctx, ctx.getString(R.string.applied), Toast.LENGTH_SHORT).show()
    }

    var pendingExport by remember { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = pendingExport
        if (uri != null && text != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            Toast.makeText(ctx, R.string.exported, Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(ctx, R.string.io_error, Toast.LENGTH_LONG).show() }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runCatching {
            val text = ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes()) } ?: error("empty")
            val list = Config.listFromJson(text).map { it.copy(id = Config.newId(), builtin = false) }
            if (list.isEmpty()) error("none")
            customs.addAll(list); persist()
            Toast.makeText(ctx, ctx.getString(R.string.imported, list.size), Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(ctx, R.string.import_error, Toast.LENGTH_LONG).show() }
    }

    var wpStart by remember { mutableLongStateOf(0L) }
    var wpFallback by remember { mutableStateOf(false) }
    val wpLauncherRef = remember { mutableStateOf<androidx.activity.result.ActivityResultLauncher<Intent>?>(null) }
    val wpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val quick = android.os.SystemClock.elapsedRealtime() - wpStart < 1500
        if (wpStart != 0L && quick && !wpFallback && !isOurWallpaper(ctx)) { wpFallback = true; openChooser(ctx, wpLauncherRef.value!!) }
        else wpFallback = false
        wpStart = 0L
    }
    wpLauncherRef.value = wpLauncher

    BackHandler(enabled = tab != 0) { tab = 0 }

    val items = listOf(
        NavItem(stringResource(R.string.ui_tab_wallpapers), Icons.Filled.Home),
        NavItem(stringResource(R.string.ui_tab_settings), Icons.Filled.Settings),
        NavItem(stringResource(R.string.ui_tab_about), Icons.Filled.Info),
    )
    var dragX by remember { mutableFloatStateOf(0f) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .pointerInput(tab) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onDragEnd = {
                            if (dragX < -120f && tab < items.lastIndex) tab++
                            else if (dragX > 120f && tab > 0) tab--
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f },
                    ) { _, d -> dragX += d }
                },
        ) {
            Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
                Text(items[tab].label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = tab, modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(225)) }, label = "tabs",
            ) { page ->
                Box(Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> WallpapersTab(
                            active = active, customs = customs,
                            onOpen = { editing = it; tab = 1 },
                            onApply = ::apply,
                            onSetWallpaper = { apply(active); setWallpaper(ctx, wpLauncher) { wpStart = it } },
                            onImport = { importer.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        )
                        1 -> key(editing.id) {
                            SettingsTab(
                                initial = editing,
                                onApply = ::apply,
                                onSave = { c, asCopy ->
                                    val saved = if (c.builtin || asCopy) c.copy(id = Config.newId(), builtin = false) else c
                                    val i = customs.indexOfFirst { it.id == saved.id }
                                    if (i >= 0) customs[i] = saved else customs.add(saved)
                                    persist()
                                    editing = saved
                                    Toast.makeText(ctx, R.string.saved, Toast.LENGTH_SHORT).show()
                                    saved
                                },
                                onDelete = { c ->
                                    customs.removeAll { it.id == c.id }; persist()
                                    Toast.makeText(ctx, R.string.deleted, Toast.LENGTH_SHORT).show()
                                    editing = Presets.all.first(); tab = 0
                                },
                                onExport = { c ->
                                    pendingExport = Config.listToJson(listOf(c))
                                    exporter.launch("asciiwall-${c.name.filter { it.isLetterOrDigit() }.ifEmpty { "config" }}.json")
                                },
                                isCustom = { c -> customs.any { it.id == c.id } },
                            )
                        }
                        else -> AboutTab()
                    }
                }
            }
        }
        PillNavigationBar(items, tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

private fun isOurWallpaper(ctx: Context): Boolean = runCatching {
    WallpaperManager.getInstance(ctx).wallpaperInfo?.serviceName == AsciiWallpaperService::class.java.name
}.getOrDefault(false)

/** Запускает системное окно установки живых обоев; если система молча его закрыла — открывает общий выбор. */
private fun setWallpaper(ctx: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>, startedAt: (Long) -> Unit) {
    if (isOurWallpaper(ctx)) {
        Toast.makeText(ctx, R.string.wallpaper_already, Toast.LENGTH_SHORT).show()
        return
    }
    val cn = ComponentName(ctx, AsciiWallpaperService::class.java)
    startedAt(android.os.SystemClock.elapsedRealtime())
    try {
        launcher.launch(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, cn),
        )
    } catch (e: Exception) {
        openChooser(ctx, launcher)
    }
}

private fun openChooser(ctx: Context, launcher: androidx.activity.result.ActivityResultLauncher<Intent>) {
    val variants = listOf(
        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
        Intent(Intent.ACTION_SET_WALLPAPER),
        Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS),
    )
    for (i in variants) {
        try {
            launcher.launch(i)
            Toast.makeText(ctx, R.string.wallpaper_pick_hint, Toast.LENGTH_LONG).show()
            return
        } catch (_: Exception) {}
    }
    Toast.makeText(ctx, R.string.no_chooser, Toast.LENGTH_LONG).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WallpapersTab(
    active: Config, customs: List<Config>,
    onOpen: (Config) -> Unit, onApply: (Config) -> Unit,
    onSetWallpaper: () -> Unit, onImport: () -> Unit,
) {
    val all = remember { sceneConfigs() }
    // 0 = все, 1 = статичные, 2 = анимированные
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var catName by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = remember { SceneType.entries.map { it.category }.distinct() }
    val selectedCat = categories.firstOrNull { it.toString() == catName }

    fun matches(c: Config): Boolean {
        val s = c.scene
        if (mode == 1 && !s.isStatic) return false
        if (mode == 2 && s.isStatic) return false
        if (selectedCat != null && s.category != selectedCat) return false
        return true
    }
    val builtinShown = all.filter(::matches)
    val customShown = customs.filter(::matches)

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = NAV_RESERVE + 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf(R.string.ui_filter_all, R.string.ui_filter_static, R.string.ui_filter_animated)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        labels.forEachIndexed { i, r ->
                            SegmentedButton(
                                selected = mode == i, onClick = { mode = i },
                                shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                            ) { Text(stringResource(r), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                    ChipRow {
                        FilterChip(selected = selectedCat == null, onClick = { catName = null }, label = { Text(stringResource(R.string.ui_cat_all)) })
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCat == cat, onClick = { catName = cat.toString() },
                                label = { Text(categoryLabel(cat)) },
                            )
                        }
                    }
                    SectionTitle(stringResource(R.string.section_builtin))
                }
            }
            items(builtinShown, key = { it.id }) { c -> SceneCard(c, c.id == active.id, onOpen, onApply) }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(R.string.section_custom), Modifier.weight(1f))
                    OutlinedButton(onClick = onImport) { Text(stringResource(R.string.import_cfg)) }
                }
            }
            if (customs.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                AppCard { Text(stringResource(R.string.custom_empty), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            }
            items(customShown, key = { it.id }) { c -> SceneCard(c, c.id == active.id, onOpen, onApply) }
            if (builtinShown.isEmpty() && customShown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.ui_nothing_found), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        ExtendedFloatingActionButton(
            onClick = onSetWallpaper,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = NAV_RESERVE + 4.dp),
        ) { Text(stringResource(R.string.set_wallpaper), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun SceneCard(c: Config, isActive: Boolean, onOpen: (Config) -> Unit, onApply: (Config) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    Surface(
        shape = shape,
        color = cs.surfaceContainer,
        border = BorderStroke(if (isActive) 2.dp else 1.dp, if (isActive) cs.primary else cs.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clip(shape).clickable { onOpen(c) },
    ) {
        Column(Modifier.padding(10.dp)) {
            AsciiPreview(c, Modifier.fillMaxWidth().aspectRatio(0.8f))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(paletteColor(c.palette, cs.primary))
                Spacer(Modifier.width(8.dp))
                Text(presetName(c), style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Text(
                if (isActive) stringResource(R.string.active)
                else sceneLabel(c.scene) + " · " + stringResource(if (c.scene.isStatic) R.string.ui_tag_static else R.string.ui_tag_animated),
                color = if (isActive) cs.primary else cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = { onApply(c) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(stringResource(R.string.apply))
            }
        }
    }
}

@Composable
private fun SettingsTab(
    initial: Config,
    onApply: (Config) -> Unit,
    onSave: (Config, Boolean) -> Config,
    onDelete: (Config) -> Unit,
    onExport: (Config) -> Unit,
    isCustom: (Config) -> Boolean,
) {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(initial) }
    val builtinName = presetName(initial)
    var name by remember { mutableStateOf(if (initial.builtin) builtinName else initial.name) }
    var confirmDelete by remember { mutableStateOf(false) }
    val custom = !cfg.builtin && isCustom(cfg)
    val full = { cfg.copy(name = name.ifBlank { builtinName }) }

    val wide = LocalConfiguration.current.screenWidthDp >= 720

    val previewBlock: @Composable (Modifier) -> Unit = { m ->
        AsciiPreview(cfg, m)
    }
    val controls: @Composable ColumnScope.() -> Unit = {
        OutlinedTextField(
            value = name, onValueChange = { name = it.take(40) }, singleLine = true,
            label = { Text(stringResource(R.string.name)) }, modifier = Modifier.fillMaxWidth(),
                    )
        SectionTitle(stringResource(R.string.scene))
        ChipRow {
            SceneType.entries.forEach { s ->
                FilterChip(selected = cfg.scene == s, onClick = { cfg = cfg.copy(scene = s) }, label = { Text(sceneLabel(s)) })
            }
        }
        SectionTitle(stringResource(R.string.palette))
        ChipRow {
            Palette.entries.forEach { p ->
                FilterChip(
                    selected = cfg.palette == p, onClick = { cfg = cfg.copy(palette = p) },
                    leadingIcon = { ColorDot(paletteColor(p, MaterialTheme.colorScheme.primary), 12.dp) },
                    label = { Text(paletteLabel(p)) },
                )
            }
        }
        SectionTitle(stringResource(R.string.ramp))
        ChipRow {
            listOf(" .:-=+*#%@", " .oO0@", " .,-~:;=!*#\$@", " 01", " .'`+*@", " ./|!").forEach { r ->
                FilterChip(selected = cfg.ramp == r, onClick = { cfg = cfg.copy(ramp = r) }, label = { Text(r.trim(), fontFamily = Mono) })
            }
        }
        OutlinedTextField(
            value = cfg.ramp, onValueChange = { cfg = cfg.copy(ramp = it.take(32)) },
            label = { Text(stringResource(R.string.ramp_custom)) }, modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontFamily = Mono),
        )
        SectionTitle(stringResource(R.string.params))
        LabeledSlider(stringResource(R.string.speed), cfg.speed, 0.1f..3f, "%.1fx".format(cfg.speed)) { cfg = cfg.copy(speed = it) }
        LabeledSlider(stringResource(R.string.density), cfg.density, 0.2f..2f, "%.1f".format(cfg.density)) { cfg = cfg.copy(density = it) }
        LabeledSlider(stringResource(R.string.font_size), cfg.fontDp, 5f..28f, "${cfg.fontDp.roundToInt()} dp") { cfg = cfg.copy(fontDp = it.roundToInt().toFloat()) }
        LabeledSlider(stringResource(R.string.brightness), cfg.brightness, 0.1f..1f, "${(cfg.brightness * 100).roundToInt()}%") { cfg = cfg.copy(brightness = it) }
        LabeledSlider(stringResource(R.string.fps), cfg.fps.toFloat(), 5f..60f, "${cfg.fps} FPS") { cfg = cfg.copy(fps = it.roundToInt()) }
        SwitchRow(stringResource(R.string.glow), cfg.glow) { cfg = cfg.copy(glow = it) }
        SwitchRow(stringResource(R.string.shuffle), cfg.shuffle) { cfg = cfg.copy(shuffle = it) }
        SwitchRow(stringResource(R.string.parallax), cfg.parallax) { cfg = cfg.copy(parallax = it) }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onApply(full()) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.apply_live)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { cfg = onSave(full(), false) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.save)) }
            OutlinedButton(onClick = { onSave(full().copy(name = name.ifBlank { builtinName } + " " + ctx.getString(R.string.copy_suffix)), true) }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.duplicate))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onExport(full()) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.export)) }
            if (custom) OutlinedButton(
                onClick = { confirmDelete = true }, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        }
        ThemePickerCard()
        Spacer(Modifier.height(NAV_RESERVE + 16.dp))
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            previewBlock(Modifier.weight(1f).fillMaxHeight().padding(bottom = NAV_RESERVE))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = controls)
        }
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            previewBlock(Modifier.fillMaxWidth().height(280.dp))
            controls()
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.delete)) },
        text = { Text(stringResource(R.string.delete_confirm, cfg.name)) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(cfg) }) { Text(stringResource(R.string.delete)) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
}


@Composable
private fun LinkButton(text: String, url: String, filled: Boolean = false) {
    val ctx = LocalContext.current
    if (filled) Button(onClick = { openUrl(ctx, url) }, modifier = Modifier.fillMaxWidth()) { Text(text) }
    else OutlinedButton(onClick = { openUrl(ctx, url) }, modifier = Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
private fun PersonRow(p: Person) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled = p.url.isNotBlank()) { openUrl(ctx, p.url) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleSmall)
            p.roleRes?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun AboutTab() {
    val ctx = LocalContext.current
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppCard {
            Text("AsciiWall", style = MaterialTheme.typography.headlineSmall, fontFamily = Mono)
            Text(stringResource(R.string.about_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (version.isNotBlank()) Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppCard {
            SectionTitle(stringResource(R.string.about_author))
            Text(AppLinks.AUTHOR_NAME, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.about_author_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkButton(stringResource(R.string.about_open_github), AppLinks.AUTHOR)
        }
        AppCard {
            SectionTitle(stringResource(R.string.about_support))
            Text(stringResource(R.string.about_support_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkButton(stringResource(R.string.about_support_btn), AppLinks.DONATE_URL, filled = true)
            if (AppLinks.DONATE_URL.isBlank()) Text(stringResource(R.string.about_support_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (AppLinks.BOOSTY_URL.isNotBlank()) LinkButton(stringResource(R.string.about_boosty), AppLinks.BOOSTY_URL)
        }
        AppCard {
            SectionTitle(stringResource(R.string.about_sources))
            Text(stringResource(R.string.about_sources_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinkButton(stringResource(R.string.about_sources_btn), AppLinks.REPO)
            Text(stringResource(R.string.about_style_credit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppCard {
            SectionTitle(stringResource(R.string.about_contrib))
            Text(stringResource(R.string.about_developers), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            People.developers.forEach { PersonRow(it) }
            Text(stringResource(R.string.about_supporters), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (People.supporters.isEmpty()) Text(stringResource(R.string.about_supporters_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            People.supporters.forEach { PersonRow(it) }
        }
        SectionTitle(stringResource(R.string.about_other), Modifier.padding(start = 8.dp))
        OtherProjects.list.forEach { p ->
            AppCard(Modifier.clickable { openUrl(ctx, p.url) }) {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(p.descRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.url.removePrefix("https://"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(NAV_RESERVE + 16.dp))
    }
}
