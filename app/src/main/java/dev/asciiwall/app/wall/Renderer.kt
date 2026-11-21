package dev.asciiwall.app.wall

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import dev.asciiwall.app.core.Config
import dev.asciiwall.app.core.Palette
import dev.asciiwall.app.core.Scene
import dev.asciiwall.app.core.SceneType
import kotlin.math.max
import kotlin.math.min

const val DEFAULT_ACCENT = 0xFFB9A6FF.toInt()

/** Рисует сетку ASCII. Один Paint, один буфер, никаких аллокаций в кадре. */
class Renderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.MONOSPACE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE; style = Paint.Style.FILL_AND_STROKE
    }
    private val one = CharArray(1)
    private var cfg: Config = Config("x", "x", false, SceneType.PLASMA)
    private var scene: Scene? = null
    private var sceneType: SceneType? = null
    private var buf = FloatArray(0)
    private var lut = IntArray(0)
    private var ramp = CharArray(2)
    private var levels = 2
    private var cw = 1f
    private var ch = 1f
    private var baseline = 0f
    private var cols = 0
    private var rows = 0
    private var width = 0
    private var height = 0
    private var originX = 0f
    private var originY = 0f
    private var margin = 0f
    private var t = 0f
    private var density = 1f
    private var accent = DEFAULT_ACCENT

    val fps get() = cfg.fps

    fun configure(config: Config, w: Int, h: Int, density: Float, accent: Int) {
        val c = config.sanitized()
        if (scene != null && c == cfg && w == width && h == height && density == this.density && accent == this.accent) return
        val sizeChanged = w != width || h != height || density != this.density
        val fontChanged = sizeChanged || c.fontDp != cfg.fontDp
        val parChanged = c.parallax != cfg.parallax
        cfg = c; width = w; height = h; this.density = density; this.accent = accent
        ramp = c.ramp.toCharArray(); levels = ramp.size
        buildLut()
        if (width <= 0 || height <= 0) return
        if (fontChanged || parChanged || scene == null) {
            val px = max(4f, c.fontDp * density)
            paint.textSize = px; glowPaint.textSize = px
            glowPaint.strokeWidth = px * 0.22f
            cw = paint.measureText("M")
            val fm = paint.fontMetrics
            ch = (fm.descent - fm.ascent) * 0.92f
            baseline = -fm.ascent * 0.92f
            margin = if (c.parallax) width * 0.2f else 0f
            cols = max(1, ((width + margin) / cw).toInt())
            rows = max(1, (height / ch).toInt())
            originY = (height - rows * ch) * 0.5f
            val s = scene
            if (s == null || sceneType != c.scene) {
                scene = Scene.create(c.scene); sceneType = c.scene
            }
            buf = FloatArray(cols * rows)
            scene!!.resize(cols, rows)
        } else if (sceneType != c.scene) {
            scene = Scene.create(c.scene); sceneType = c.scene
            scene!!.resize(cols, rows)
        }
    }

    fun gridCols() = cols
    fun gridRows() = rows

    /** xOffset 0..1 — страница рабочего стола (для параллакса). */
    fun frame(canvas: Canvas, dt: Float, xOffset: Float) {
        val s = scene ?: return
        t += dt * cfg.speed
        canvas.drawColor(Color.BLACK)
        s.update(t, dt * cfg.speed, cfg.density, buf)
        originX = if (cfg.parallax) -margin * xOffset.coerceIn(0f, 1f) else (width - cols * cw) * 0.5f
        val n1 = levels - 1
        val glow = cfg.glow
        val shuffle = cfg.shuffle
        val neon = cfg.palette == Palette.NEON
        val slot = (t * 5f).toInt()
        val tHue = (t * 6f).toInt()
        for (r in 0 until rows) {
            val y = originY + r * ch + baseline
            val rowBase = r * cols
            for (c in 0 until cols) {
                val v = buf[rowBase + c]
                var lvl = (v * n1 + 0.5f).toInt()
                if (lvl <= 0) continue
                if (lvl > n1) lvl = n1
                val hue = if (neon) ((c * 64 / cols + r + tHue) and 63) else 0
                val color = lut[hue * levels + lvl]
                var chr = ramp[lvl]
                if (shuffle) {
                    var h = c * 73856093 xor r * 19349663 xor slot * 83492791
                    h = h xor (h ushr 15)
                    chr = ramp[1 + ((h and 0x7fffffff) % n1)]
                }
                one[0] = chr
                val x = originX + c * cw
                if (glow && lvl >= n1 - 1) {
                    glowPaint.color = (color and 0x00FFFFFF) or 0x44000000
                    canvas.drawText(one, 0, 1, x, y, glowPaint)
                }
                paint.color = color
                canvas.drawText(one, 0, 1, x, y, paint)
            }
        }
    }

    private fun buildLut() {
        val hues = if (cfg.palette == Palette.NEON) 64 else 1
        if (lut.size != hues * levels) lut = IntArray(hues * levels)
        val br = cfg.brightness
        for (h in 0 until hues) for (l in 0 until levels) {
            val i = l.toFloat() / (levels - 1)
            lut[h * levels + l] = scale(colorAt(cfg.palette, i, h / 64f), br)
        }
    }

    private fun scale(c: Int, f: Float): Int =
        Color.rgb((Color.red(c) * f).toInt(), (Color.green(c) * f).toInt(), (Color.blue(c) * f).toInt())

    private fun mix(a: Int, b: Int, f: Float): Int {
        val k = min(1f, max(0f, f))
        return Color.rgb(
            (Color.red(a) + (Color.red(b) - Color.red(a)) * k).toInt(),
            (Color.green(a) + (Color.green(b) - Color.green(a)) * k).toInt(),
            (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).toInt(),
        )
    }

    private fun ramp3(dark: Int, mid: Int, top: Int, i: Float) =
        if (i < 0.7f) mix(dark, mid, i / 0.7f) else mix(mid, top, (i - 0.7f) / 0.3f)

    private fun colorAt(p: Palette, i: Float, hue: Float): Int = when (p) {
        Palette.MONO -> mix(0x303030, 0xFFFFFF, i)
        Palette.GREEN -> ramp3(0x053D12, 0x2BFF6B, 0xE0FFE8, i)
        Palette.AMBER -> ramp3(0x4A2E00, 0xFFB000, 0xFFF0C8, i)
        Palette.ICE -> ramp3(0x0B2A44, 0x62B8FF, 0xEAF6FF, i)
        Palette.FIRE -> when {
            i < 0.3f -> mix(0x300000, 0xB01000, i / 0.3f)
            i < 0.65f -> mix(0xB01000, 0xFF7A00, (i - 0.3f) / 0.35f)
            else -> mix(0xFF7A00, 0xFFF3B0, (i - 0.65f) / 0.35f)
        }
        Palette.NEON -> {
            val hsv = floatArrayOf(((hue * 360f) + 200f) % 360f, 0.75f - 0.35f * i * i, 0.35f + 0.65f * i)
            Color.HSVToColor(hsv)
        }
        Palette.DYNAMIC -> ramp3(mix(Color.BLACK, accent, 0.3f), accent, mix(accent, Color.WHITE, 0.8f), i)
    }
}
