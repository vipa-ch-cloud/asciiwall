package dev.asciiwall.app.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Визуальный язык перенесён из tg-ws-proxy-android (GPLv3): ui/Theme.kt.

val AppFontFamily = FontFamily.Default

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = AppFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ---- Палитры оригинала: espresso, indigo, forest ----

private val EspressoLight = lightColorScheme(
    primary = Color(0xFF6D4C41), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7CCC8), onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF8D6E63), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFEBE9), onSecondaryContainer = Color(0xFF4E342E),
    tertiary = Color(0xFF795548), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCAAA4), onTertiaryContainer = Color(0xFF3E2723),
    background = Color(0xFFFFFBF7), onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFF5F0EB), onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFEFEBE9), onSurfaceVariant = Color(0xFF5D4037),
    outline = Color(0xFFBCAAA4), outlineVariant = Color(0xFFD7CCC8),
    inverseSurface = Color(0xFF322F2D), inverseOnSurface = Color(0xFFF5F0EB), inversePrimary = Color(0xFFD7CCC8),
    surfaceTint = Color(0xFF6D4C41),
)

private val EspressoDark = darkColorScheme(
    primary = Color(0xFFD7CCC8), onPrimary = Color(0xFF3E2723),
    primaryContainer = Color(0xFF5D4037), onPrimaryContainer = Color(0xFFEFEBE9),
    secondary = Color(0xFFBCAAA4), onSecondary = Color(0xFF3E2723),
    secondaryContainer = Color(0xFF4E342E), onSecondaryContainer = Color(0xFFEFEBE9),
    tertiary = Color(0xFFA1887F), onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFF5D4037), onTertiaryContainer = Color(0xFFEFEBE9),
    background = Color(0xFF1A1614), onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF211D1B), onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF2C2624), onSurfaceVariant = Color(0xFFD7CCC8),
    outline = Color(0xFF8D6E63), outlineVariant = Color(0xFF4E342E),
    inverseSurface = Color(0xFFEDE0D4), inverseOnSurface = Color(0xFF322F2D), inversePrimary = Color(0xFF6D4C41),
    surfaceTint = Color(0xFFD7CCC8),
)

private val IndigoLight = lightColorScheme(
    primary = Color(0xFF5B588D), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2DFFF), onPrimaryContainer = Color(0xFF1A1744),
    secondary = Color(0xFF5B588D), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2DFFF), onSecondaryContainer = Color(0xFF1A1744),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFF6F3FA), onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE4E1EC), onSurfaceVariant = Color(0xFF47464F),
    outline = Color(0xFF787680), outlineVariant = Color(0xFFC8C5D0),
)

private val IndigoDark = darkColorScheme(
    primary = Color(0xFFC4C0FF), onPrimary = Color(0xFF2D2A5B),
    primaryContainer = Color(0xFF434073), onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFFC4C0FF), onSecondary = Color(0xFF2D2A5B),
    secondaryContainer = Color(0xFF434073), onSecondaryContainer = Color(0xFFE2DFFF),
    background = Color(0xFF131316), onBackground = Color(0xFFE4E1E6),
    surface = Color(0xFF1B1B1F), onSurface = Color(0xFFC8C5D0),
    surfaceVariant = Color(0xFF47464F), onSurfaceVariant = Color(0xFFC8C5D0),
    outline = Color(0xFF918F9A), outlineVariant = Color(0xFF47464F),
)

private val ForestLight = lightColorScheme(
    primary = Color(0xFF5F5D68), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E0F0), onPrimaryContainer = Color(0xFF1C1A23),
    secondary = Color(0xFF5F5D68), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5E0F0), onSecondaryContainer = Color(0xFF1C1A23),
    background = Color(0xFFFCF8FF), onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFF7F2FA), onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE6E0E9), onSurfaceVariant = Color(0xFF48454E),
    outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
)

private val ForestDark = darkColorScheme(
    primary = Color(0xFFC8C4D3), onPrimary = Color(0xFF312F38),
    primaryContainer = Color(0xFF474550), onPrimaryContainer = Color(0xFFE5E0F0),
    secondary = Color(0xFFC8C4D3), onSecondary = Color(0xFF312F38),
    secondaryContainer = Color(0xFF474550), onSecondaryContainer = Color(0xFFE5E0F0),
    background = Color(0xFF141318), onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B20), onSurface = Color(0xFFCAC4D0),
    surfaceVariant = Color(0xFF48454E), onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF48454E),
)

// ---- Дополнительные палитры в том же стиле (по оттенку) ----

private fun hueLight(h: Float) = lightColorScheme(
    primary = Color.hsv(h, 0.46f, 0.55f), onPrimary = Color.White,
    primaryContainer = Color.hsv(h, 0.12f, 0.96f), onPrimaryContainer = Color.hsv(h, 0.65f, 0.2f),
    secondary = Color.hsv(h, 0.40f, 0.52f), onSecondary = Color.White,
    secondaryContainer = Color.hsv(h, 0.10f, 0.94f), onSecondaryContainer = Color.hsv(h, 0.65f, 0.2f),
    background = Color.hsv(h, 0.02f, 0.995f), onBackground = Color(0xFF1B1B1F),
    surface = Color.hsv(h, 0.04f, 0.965f), onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color.hsv(h, 0.08f, 0.92f), onSurfaceVariant = Color.hsv(h, 0.20f, 0.32f),
    outline = Color.hsv(h, 0.10f, 0.50f), outlineVariant = Color.hsv(h, 0.10f, 0.82f),
)

private fun hueDark(h: Float) = darkColorScheme(
    primary = Color.hsv(h, 0.25f, 1f), onPrimary = Color.hsv(h, 0.6f, 0.25f),
    primaryContainer = Color.hsv(h, 0.45f, 0.40f), onPrimaryContainer = Color.hsv(h, 0.10f, 0.95f),
    secondary = Color.hsv(h, 0.22f, 0.92f), onSecondary = Color.hsv(h, 0.6f, 0.25f),
    secondaryContainer = Color.hsv(h, 0.40f, 0.36f), onSecondaryContainer = Color.hsv(h, 0.10f, 0.95f),
    background = Color.hsv(h, 0.08f, 0.09f), onBackground = Color(0xFFE6E1E5),
    surface = Color.hsv(h, 0.08f, 0.12f), onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color.hsv(h, 0.10f, 0.28f), onSurfaceVariant = Color.hsv(h, 0.08f, 0.82f),
    outline = Color.hsv(h, 0.08f, 0.60f), outlineVariant = Color.hsv(h, 0.10f, 0.28f),
)

/** Контейнеры поверхностей считаются от surface/surfaceVariant, чтобы все палитры выглядели одинаково. */
private fun ColorScheme.withContainers(): ColorScheme = copy(
    surfaceContainerLowest = lerp(background, surface, 0.4f),
    surfaceContainerLow = lerp(surface, surfaceVariant, 0.15f),
    surfaceContainer = lerp(surface, surfaceVariant, 0.35f),
    surfaceContainerHigh = lerp(surface, surfaceVariant, 0.55f),
    surfaceContainerHighest = lerp(surface, surfaceVariant, 0.75f),
)

/** AMOLED: чисто чёрный фон для тёмной палитры. */
private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color(0xFF0A0A0A),
    surfaceVariant = lerp(Color.Black, surfaceVariant, 0.55f),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0C0C0C),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
)

/** Палитра приложения: id хранится в настройках, [preview] рисуется кружком в выборе. */
class AppPalette(val id: String, val labelRes: Int, val preview: Color, val light: ColorScheme, val dark: ColorScheme)

val AppPalettes: List<AppPalette> = listOf(
    AppPalette("indigo", dev.asciiwall.app.R.string.ui_pal_indigo, Color(0xFF5B588D), IndigoLight, IndigoDark),
    AppPalette("forest", dev.asciiwall.app.R.string.ui_pal_forest, Color(0xFF5F5D68), ForestLight, ForestDark),
    AppPalette("espresso", dev.asciiwall.app.R.string.ui_pal_espresso, Color(0xFF6D4C41), EspressoLight, EspressoDark),
    AppPalette("ocean", dev.asciiwall.app.R.string.ui_pal_ocean, Color(0xFF3F7CAC), hueLight(205f), hueDark(205f)),
    AppPalette("rose", dev.asciiwall.app.R.string.ui_pal_rose, Color(0xFFA24A6E), hueLight(335f), hueDark(335f)),
    AppPalette("ember", dev.asciiwall.app.R.string.ui_pal_ember, Color(0xFFB0701F), hueLight(35f), hueDark(35f)),
    AppPalette("moss", dev.asciiwall.app.R.string.ui_pal_moss, Color(0xFF3F8A6A), hueLight(155f), hueDark(155f)),
)

object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
    const val AMOLED = "amoled"
}

/** Сохраняемые настройки внешнего вида: изменения сразу перекомпонуют тему. */
object ThemeStore {
    private const val FILE = "ui_prefs"
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    var palette by mutableStateOf("indigo")
        private set
    var dynamic by mutableStateOf(false)
        private set
    private var loaded = false

    val dynamicSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val p = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        mode = p.getString("theme_mode", ThemeMode.SYSTEM) ?: ThemeMode.SYSTEM
        palette = p.getString("theme_palette", "indigo") ?: "indigo"
        // ключ прежней версии сохраняем как есть
        dynamic = p.getBoolean("app_dynamic_colors", false)
    }

    private fun edit(ctx: Context, f: android.content.SharedPreferences.Editor.() -> Unit) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().apply(f).apply()

    fun setMode(ctx: Context, v: String) { mode = v; edit(ctx) { putString("theme_mode", v) } }
    fun setPalette(ctx: Context, v: String) { palette = v; edit(ctx) { putString("theme_palette", v) } }
    fun setDynamic(ctx: Context, v: Boolean) { dynamic = v; edit(ctx) { putBoolean("app_dynamic_colors", v) } }
}

fun resolveColorScheme(palette: String, dark: Boolean, amoled: Boolean): ColorScheme {
    val p = AppPalettes.firstOrNull { it.id == palette } ?: AppPalettes.first()
    val base = (if (dark) p.dark else p.light).withContainers()
    return if (dark && amoled) base.amoled() else base
}

@Composable
fun AsciiTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    ThemeStore.load(ctx)
    val mode = ThemeStore.mode
    val dark = when (mode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    val amoled = mode == ThemeMode.AMOLED
    val scheme: ColorScheme = if (ThemeStore.dynamic && ThemeStore.dynamicSupported) {
        val s = (if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx))
        if (dark && amoled) s.amoled() else s
    } else resolveColorScheme(ThemeStore.palette, dark, amoled)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val navColor = if (dark) Color.Transparent else lerp(scheme.background, scheme.surface, 0.55f)
            @Suppress("DEPRECATION") window.statusBarColor = Color.Transparent.toArgb()
            @Suppress("DEPRECATION") window.navigationBarColor = navColor.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                @Suppress("DEPRECATION") window.isStatusBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
