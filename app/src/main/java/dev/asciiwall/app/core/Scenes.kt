package dev.asciiwall.app.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Генератор кадра: заполняет out[rows*cols] яркостью 0..1.
 * Без аллокаций в update(); состояние создаётся в onResize().
 */
abstract class Scene {
    var cols = 0
        private set
    var rows = 0
        private set

    fun resize(c: Int, r: Int) {
        cols = max(1, c); rows = max(1, r)
        onResize()
    }

    protected abstract fun onResize()

    /** t — анимационное время (с, уже умножено на скорость), dt — приращение. */
    abstract fun update(t: Float, dt: Float, density: Float, out: FloatArray)

    protected fun wipe(out: FloatArray) { java.util.Arrays.fill(out, 0, cols * rows, 0f) }

    /** Точка с max-смешиванием; за границей — ничего. */
    protected fun dot(out: FloatArray, x: Int, y: Int, v: Float) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return
        val i = y * cols + x
        if (v > out[i]) out[i] = v
    }

    protected fun dotf(out: FloatArray, x: Float, y: Float, v: Float) {
        if (x < 0f || y < 0f) return
        dot(out, x.toInt(), y.toInt(), v)
    }

    protected fun seg(out: FloatArray, x0: Float, y0: Float, x1: Float, y1: Float, v: Float) {
        val dx = x1 - x0; val dy = y1 - y0
        val n = min(2000, max(abs(dx), abs(dy)).toInt() + 1)
        for (i in 0..n) { val f = i.toFloat() / n; dotf(out, x0 + dx * f, y0 + dy * f, v) }
    }

    /** Прямоугольник с заменой значения (не max). */
    protected fun rect(out: FloatArray, x0: Int, y0: Int, w: Int, h: Int, v: Float) {
        for (yy in max(0, y0) until min(rows, y0 + h)) for (xx in max(0, x0) until min(cols, x0 + w)) out[yy * cols + xx] = v
    }

    companion object {
        fun create(type: SceneType): Scene = when (type) {
            SceneType.MATRIX -> MatrixScene()
            SceneType.FIRE -> FireScene()
            SceneType.PLASMA -> PlasmaScene()
            SceneType.WAVES -> WavesScene()
            SceneType.STARS -> StarsScene()
            SceneType.CLOCK -> ClockScene()
            SceneType.DONUT -> DonutScene()
            SceneType.LIFE -> LifeScene()
            SceneType.SNOW -> ParticleScene(0)
            SceneType.RAIN -> ParticleScene(1)
            SceneType.BREATH -> BreathScene()
            SceneType.MATRIX_UP -> MatrixScene(up = true)
            SceneType.MATRIX_STORM -> MatrixScene(speedMul = 2.2f, lenMul = 0.45f)
            SceneType.MATRIX_SLOW -> MatrixScene(speedMul = 0.35f, lenMul = 1.5f)
            SceneType.LAVA -> LavaScene()
            SceneType.OCEAN -> OceanScene()
            SceneType.VORTEX -> VortexScene(3, 1f, false)
            SceneType.HYPNO -> VortexScene(1, 2.2f, true)
            SceneType.AURORA -> AuroraScene()
            SceneType.METEORS -> MeteorScene()
            SceneType.GALAXY -> GalaxyScene()
            SceneType.WARP -> StarsScene(speed = 1.4f, trail = 9)
            SceneType.TUNNEL -> TunnelScene()
            SceneType.CLOCK_ANALOG -> AnalogClockScene()
            SceneType.CLOCK_BINARY -> BinaryClockScene()
            SceneType.SNAKE -> SnakeScene()
            SceneType.PONG -> PongScene()
            SceneType.MAZE -> MazeScene()
            SceneType.MANDEL_ZOOM -> MandelZoomScene()
            SceneType.JULIA_ANIM -> JuliaAnimScene()
            SceneType.LISSAJOUS -> LissajousScene()
            SceneType.SPIROGRAPH -> SpirographScene()
            SceneType.PENDULUM -> PendulumScene()
            SceneType.FIRE_TORCH -> FireScene(1)
            SceneType.FIRE_EMBERS -> FireScene(2)
            SceneType.STORM -> StormScene()
            SceneType.LEAVES -> ParticleScene(2)
            SceneType.FIREFLIES -> FirefliesScene()
            SceneType.DNA -> DnaScene()
            SceneType.BRAINFUCK -> BrainfuckScene()
            SceneType.AQUARIUM -> AquariumScene()
            SceneType.ECG -> EcgScene()
            SceneType.EQUALIZER -> EqualizerScene()
            SceneType.RIPPLES -> RipplesScene()
            SceneType.CUBE -> CubeScene()
            SceneType.GLOBE -> GlobeScene()
            SceneType.RADAR -> RadarScene()
            SceneType.FIREWORKS -> FireworksScene()
            SceneType.CLOUDS -> CloudsScene()
            SceneType.MOUNTAINS -> MountainsScene()
            SceneType.LAKE -> LakeScene()
            SceneType.PINES -> PinesScene()
            SceneType.SNOWFLAKE -> SnowflakeScene()
            SceneType.TOPOMAP -> TopoMapScene()
            SceneType.CASTLE -> CastleScene()
            SceneType.CITY -> CityScene()
            SceneType.PYRAMIDS -> PyramidsScene()
            SceneType.CAT -> CatScene()
            SceneType.OWL -> OwlScene()
            SceneType.PENGUIN -> PenguinScene()
            SceneType.BUTTERFLY -> ButterflyScene()
            SceneType.WEB -> WebScene()
            SceneType.EARTH -> EarthScene()
            SceneType.SATURN -> SaturnScene()
            SceneType.MOON -> MoonScene()
            SceneType.MANDELBROT -> MandelbrotScene()
            SceneType.JULIA -> JuliaScene()
            SceneType.SIERPINSKI -> SierpinskiScene()
            SceneType.CARPET -> CarpetScene()
            SceneType.HILBERT -> HilbertScene()
            SceneType.TREE -> TreeScene()
            SceneType.MOSAIC -> MosaicScene()
            SceneType.TRUCHET -> TruchetScene()
            SceneType.SUNFLOWER -> SunflowerScene()
            SceneType.MANDALA -> MandalaScene()
            SceneType.ROSE -> RoseScene()
            SceneType.SYNTHWAVE -> SynthwaveScene()
            SceneType.DROID -> DroidScene()
        }
    }
}

class MatrixScene(
    private val up: Boolean = false,
    private val speedMul: Float = 1f,
    private val lenMul: Float = 1f,
) : Scene() {
    private val rnd = Random(1)
    private var head = FloatArray(0)
    private var spd = FloatArray(0)
    private var len = FloatArray(0)

    override fun onResize() {
        head = FloatArray(cols); spd = FloatArray(cols); len = FloatArray(cols)
        for (c in 0 until cols) { respawn(c, 1f); head[c] = rnd.nextFloat() * rows * 1.3f - rows * 0.3f }
    }

    private fun respawn(c: Int, density: Float) {
        spd[c] = (8f + rnd.nextFloat() * 18f) * speedMul
        len[c] = max(4f, rows * (0.25f + rnd.nextFloat() * 0.45f) * lenMul)
        head[c] = -rnd.nextFloat() * rows * 1.5f / density
    }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        java.util.Arrays.fill(out, 0, cols * rows, 0f)
        for (c in 0 until cols) {
            head[c] += spd[c] * dt
            if (head[c] - len[c] > rows) respawn(c, density)
            val h = head[c]; val l = len[c]
            val from = max(0, (h - l).toInt())
            val to = min(rows - 1, h.toInt())
            for (r in from..to) {
                val d = h - r
                if (d < 0f) continue
                val v = 1f - d / l
                out[(if (up) rows - 1 - r else r) * cols + c] = if (d < 1f) 1f else v * v
            }
        }
    }
}

class FireScene(private val mode: Int = 0) : Scene() {
    private val rnd = Random(2)
    private var heat = FloatArray(0)
    private var acc = 0f

    override fun onResize() { heat = FloatArray(cols * (rows + 2)); acc = 0f }

    private fun step(density: Float) {
        val base = rows * cols
        for (c in 0 until cols) {
            val fx = (c + 0.5f) / cols
            val p = when (mode) {
                1 -> if (fx > 0.3f && fx < 0.7f) 0.55f * density.coerceAtMost(1.5f) else 0f
                2 -> 0.05f * density
                else -> 0.15f * density + 0.3f
            }
            heat[base + c] = if (rnd.nextFloat() < p) 0.7f + rnd.nextFloat() * 0.3f else heat[base + c] * 0.5f
            heat[base + cols + c] = heat[base + c]
        }
        val cool = (if (mode == 2) 4.4f else 2.6f) / rows / density.coerceAtLeast(0.2f)
        for (r in 0 until rows) {
            val rb = (r + 1) * cols
            val rb2 = (r + 2) * cols
            for (c in 0 until cols) {
                val l = if (c > 0) c - 1 else c
                val rr = if (c < cols - 1) c + 1 else c
                val v = (heat[rb + l] + heat[rb + c] + heat[rb + rr] + heat[rb2 + c]) * 0.25f - rnd.nextFloat() * cool
                heat[r * cols + c] = if (v > 0f) v else 0f
            }
        }
    }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        acc += dt * 30f
        var n = 0
        while (acc >= 1f && n < 6) { step(density); acc -= 1f; n++ }
        if (acc > 6f) acc = 0f
        for (i in 0 until cols * rows) out[i] = min(1f, heat[i] * 1.25f)
    }
}

class PlasmaScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val k = 0.12f * density
        for (r in 0 until rows) {
            val y = r * 2f
            val sy = sin(y * k * 1.3f + t * 1.3f)
            for (c in 0 until cols) {
                val x = c.toFloat()
                val dx = x - cols * 0.5f + sin(t * 0.4f) * cols * 0.3f
                val dy = y - rows + cos(t * 0.3f) * rows * 0.5f
                val v = sin(x * k + t) + sy + sin((x + y) * k * 0.6f + t * 0.7f) + sin(sqrt(dx * dx + dy * dy) * k * 0.8f - t)
                out[r * cols + c] = v * 0.125f + 0.5f
            }
        }
    }
}

class WavesScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        java.util.Arrays.fill(out, 0, cols * rows, 0f)
        val layers = 4
        for (i in 0 until layers) {
            val base = rows * (0.3f + 0.17f * i)
            val amp = rows * (0.05f + 0.01f * i)
            val freq = (2f + i * 0.9f) * density * (2f * PI.toFloat()) / cols
            val sp = 0.8f + i * 0.35f
            val peak = 0.95f - 0.15f * i
            for (c in 0 until cols) {
                val line = base + amp * sin(c * freq + t * sp + i * 1.7f) + amp * 0.4f * sin(c * freq * 2.3f - t * sp * 1.4f)
                val r0 = max(0, line.toInt())
                for (r in r0 until min(rows, r0 + (rows * 0.3f).toInt() + 1)) {
                    val d = r - line
                    val v = if (d < 1f) peak else {
                        val x = 1f - d / (rows * 0.3f)
                        peak * 0.55f * x * x
                    }
                    val idx = r * cols + c
                    if (v > out[idx]) out[idx] = v
                }
            }
        }
    }
}

class StarsScene(private val speed: Float = 0.35f, private val trail: Int = 3) : Scene() {
    private val rnd = Random(3)
    private val maxN = 700
    private val sx = FloatArray(maxN); private val sy = FloatArray(maxN); private val sz = FloatArray(maxN)

    override fun onResize() { for (i in 0 until maxN) { spawn(i); sz[i] = rnd.nextFloat() } }

    private fun spawn(i: Int) {
        sx[i] = rnd.nextFloat() * 2f - 1f; sy[i] = rnd.nextFloat() * 2f - 1f; sz[i] = 1f
    }

    private fun plot(out: FloatArray, x: Float, y: Float, v: Float) {
        val cx = x.toInt(); val cy = y.toInt()
        if (x < 0f || y < 0f || cx >= cols || cy >= rows) return
        val idx = cy * cols + cx
        if (v > out[idx]) out[idx] = v
    }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        java.util.Arrays.fill(out, 0, cols * rows, 0f)
        val n = (cols * rows * 0.015f * density).toInt().coerceIn(12, maxN)
        val hx = cols * 0.5f; val hy = rows * 0.5f
        val sc = max(cols.toFloat(), rows * 0.5f) * 0.35f
        for (i in 0 until n) {
            sz[i] -= dt * speed
            if (sz[i] <= 0.03f) { spawn(i); continue }
            val z = sz[i]
            val px = hx + sx[i] / z * sc
            val py = hy + sy[i] / z * sc * 0.5f
            if (px < 0f || py < 0f || px >= cols || py >= rows) { spawn(i); continue }
            val b = min(1f, (1f - z) * 1.4f + 0.1f)
            plot(out, px, py, b)
            val z2 = z + 0.05f * trail / 3f
            for (k in 1..trail) {
                val zz = z + (z2 - z) * k / trail
                plot(out, hx + sx[i] / zz * sc, hy + sy[i] / zz * sc * 0.5f, b * (1f - k * 0.85f / trail))
            }
        }
    }
}

class ClockScene : Scene() {
    // 5 столбцов x 7 строк, по строкам слева направо
    private val glyphs = Array(10) { BooleanArray(35) }
    private val rnd = Random(4)
    private var tzOffset = 0L
    private var tzStamp = 0L

    init {
        val raw = arrayOf(
            arrayOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
            arrayOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
            arrayOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
            arrayOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
            arrayOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
            arrayOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
            arrayOf("00110", "01000", "10000", "11110", "10001", "10001", "01110"),
            arrayOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
            arrayOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
            arrayOf("01110", "10001", "10001", "01111", "00001", "00010", "01100"),
        )
        for (d in 0..9) for (r in 0 until 7) for (c in 0 until 5) glyphs[d][r * 5 + c] = raw[d][r][c] == '1'
    }

    override fun onResize() {}

    private fun hash(a: Int, b: Int, c: Int): Int {
        var h = a * 374761393 + b * 668265263 + c * 1274126177
        h = (h xor (h ushr 13)) * 1274126177
        return h xor (h ushr 16)
    }

    private fun block(out: FloatArray, x0: Int, y0: Int, sx: Int, sy: Int, v: Float) {
        for (yy in y0 until y0 + sy) {
            if (yy < 0 || yy >= rows) continue
            for (xx in x0 until x0 + sx) if (xx in 0 until cols) out[yy * cols + xx] = v
        }
    }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val total = cols * rows
        val slot = (t * 2f).toInt()
        for (i in 0 until total) {
            val h = hash(i, 7, slot)
            out[i] = if ((h and 0x7F) == 0) 0.18f + ((h ushr 8) and 7) * 0.02f else 0f
        }
        val now = System.currentTimeMillis()
        if (now - tzStamp > 30_000L) { tzOffset = java.util.TimeZone.getDefault().getOffset(now).toLong(); tzStamp = now }
        val local = (now + tzOffset) / 1000L
        val sec = (local % 60).toInt()
        val mn = ((local / 60) % 60).toInt()
        val hour = ((local / 3600) % 24).toInt()

        // ширина в «единицах»: 4 цифры * 5 + двоеточие 1 + 4 промежутка = 25
        var sx = max(1, (cols * 0.9f / 25f).toInt())
        var sy = max(1, sx / 2)
        while (sy * 7 > rows * 0.6f && sx > 1) { sx--; sy = max(1, sx / 2) }
        val unit = sx
        val totalW = 25 * unit
        var x = (cols - totalW) / 2
        val y0 = (rows * 0.4f).toInt() - 7 * sy / 2
        val digits = intArrayOf(hour / 10, hour % 10, -1, mn / 10, mn % 10)
        for ((k, d) in digits.withIndex()) {
            if (d < 0) {
                val v = if (sec % 2 == 0) 0.95f else 0.35f
                block(out, x, y0 + 2 * sy, sx, sy, v); block(out, x, y0 + 4 * sy, sx, sy, v)
                x += unit + unit
                continue
            }
            val g = glyphs[d]
            for (r in 0 until 7) for (c in 0 until 5) if (g[r * 5 + c]) {
                val shim = 0.86f + 0.14f * sin(t * 2f + (c + r) * 0.7f + k)
                block(out, x + c * unit, y0 + r * sy, sx, sy, shim)
            }
            x += 5 * unit + unit
        }
        // секундная полоса
        val barW = (totalW * sec / 59f).toInt()
        block(out, (cols - totalW) / 2, y0 + 7 * sy + sy + 1, barW, 1, 0.6f)
        block(out, (cols - totalW) / 2 + barW, y0 + 7 * sy + sy + 1, max(0, totalW - barW), 1, 0.15f)
    }
}

class DonutScene : Scene() {
    private var zbuf = FloatArray(0)
    private val nT = 72
    private val nP = 180
    private val sinT = FloatArray(nT); private val cosT = FloatArray(nT)
    private val sinP = FloatArray(nP); private val cosP = FloatArray(nP)

    init {
        for (i in 0 until nT) { val a = i * 2f * PI.toFloat() / nT; sinT[i] = sin(a); cosT[i] = cos(a) }
        for (i in 0 until nP) { val a = i * 2f * PI.toFloat() / nP; sinP[i] = sin(a); cosP[i] = cos(a) }
    }

    override fun onResize() { zbuf = FloatArray(cols * rows) }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val total = cols * rows
        java.util.Arrays.fill(out, 0, total, 0f)
        java.util.Arrays.fill(zbuf, 0f)
        val a = t * 0.9f; val b = t * 0.55f
        val cA = cos(a); val sA = sin(a); val cB = cos(b); val sB = sin(b)
        val k2 = 5f
        val k1 = min(cols * 0.75f, rows * 1.5f) * density.coerceIn(0.4f, 1.2f) * 0.85f
        val hx = cols * 0.5f; val hy = rows * 0.5f
        for (j in 0 until nT) {
            val ct = cosT[j]; val st = sinT[j]
            val cx = 2f + ct; val cy = st
            for (i in 0 until nP) {
                val cp = cosP[i]; val sp = sinP[i]
                val x = cx * (cB * cp + sA * sB * sp) - cy * cA * sB
                val y = cx * (sB * cp - sA * cB * sp) + cy * cA * cB
                val z = k2 + cA * cx * sp + cy * sA
                val ooz = 1f / z
                val xp = (hx + k1 * ooz * x).toInt()
                val yp = (hy - k1 * ooz * y * 0.5f).toInt()
                if (xp < 0 || yp < 0 || xp >= cols || yp >= rows) continue
                val l = cp * ct * sB - cA * ct * sp - sA * st + cB * (cA * st - ct * sA * sp)
                if (l > 0f) {
                    val idx = yp * cols + xp
                    if (ooz > zbuf[idx]) { zbuf[idx] = ooz; out[idx] = min(1f, l * 0.7071f + 0.05f) }
                }
            }
        }
    }
}

class LifeScene : Scene() {
    private val rnd = Random(5)
    private var cur = ByteArray(0)
    private var nxt = ByteArray(0)
    private var glow = FloatArray(0)
    private var acc = 0f
    private var lastAlive = -1
    private var sameCount = 0
    private var steps = 0

    override fun onResize() {
        cur = ByteArray(cols * rows); nxt = ByteArray(cols * rows); glow = FloatArray(cols * rows)
        seed(1f)
    }

    private fun seed(density: Float) {
        val p = 0.22f * density.coerceIn(0.3f, 1.6f)
        for (i in cur.indices) cur[i] = if (rnd.nextFloat() < p) 1 else 0
        sameCount = 0; steps = 0; lastAlive = -1
    }

    private fun step(density: Float) {
        var alive = 0
        for (r in 0 until rows) {
            val ru = if (r == 0) rows - 1 else r - 1
            val rd = if (r == rows - 1) 0 else r + 1
            for (c in 0 until cols) {
                val cl = if (c == 0) cols - 1 else c - 1
                val cr = if (c == cols - 1) 0 else c + 1
                val n = cur[ru * cols + cl] + cur[ru * cols + c] + cur[ru * cols + cr] +
                    cur[r * cols + cl] + cur[r * cols + cr] +
                    cur[rd * cols + cl] + cur[rd * cols + c] + cur[rd * cols + cr]
                val a = cur[r * cols + c].toInt()
                val v = if (a == 1) (if (n == 2 || n == 3) 1 else 0) else (if (n == 3) 1 else 0)
                nxt[r * cols + c] = v.toByte()
                alive += v
            }
        }
        val tmp = cur; cur = nxt; nxt = tmp
        steps++
        if (alive == lastAlive) sameCount++ else sameCount = 0
        lastAlive = alive
        if (alive < cols * rows / 100 || sameCount > 40 || steps > 600) seed(density)
    }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        acc += dt * 8f
        var n = 0
        while (acc >= 1f && n < 4) { step(density); acc -= 1f; n++ }
        if (acc > 4f) acc = 0f
        val decay = 1f - min(0.9f, dt * 4f)
        for (i in 0 until cols * rows) {
            glow[i] = if (cur[i].toInt() == 1) 1f else glow[i] * decay
            out[i] = glow[i]
        }
    }
}

/** kind: 0 — снег, 1 — дождь, 2 — листья. */
class ParticleScene(private val kind: Int) : Scene() {
    private val rain = kind == 1
    private val rnd = Random(6 + kind)
    private val maxN = 900
    private val px = FloatArray(maxN); private val py = FloatArray(maxN)
    private val vy = FloatArray(maxN); private val ph = FloatArray(maxN); private val br = FloatArray(maxN)

    override fun onResize() { for (i in 0 until maxN) { spawn(i); py[i] = rnd.nextFloat() * rows } }

    private fun spawn(i: Int) {
        px[i] = rnd.nextFloat() * (cols + 8)
        py[i] = -rnd.nextFloat() * 4f
        br[i] = 0.35f + rnd.nextFloat() * 0.65f
        vy[i] = if (kind == 2) 3f + rnd.nextFloat() * 5f else if (rain) 22f + rnd.nextFloat() * 22f else (2f + rnd.nextFloat() * 5f) * (0.5f + br[i] * 0.5f)
        ph[i] = rnd.nextFloat() * 6.28f
    }

    private fun plot(out: FloatArray, x: Int, y: Int, v: Float) {
        if (x < 0 || y < 0 || x >= cols || y >= rows) return
        val idx = y * cols + x
        if (v > out[idx]) out[idx] = v
    }

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        java.util.Arrays.fill(out, 0, cols * rows, 0f)
        val n = (cols * rows * (if (rain) 0.01f else if (kind == 2) 0.007f else 0.014f) * density).toInt().coerceIn(10, maxN)
        for (i in 0 until n) {
            py[i] += vy[i] * dt
            if (rain) px[i] -= 5f * dt else if (kind == 2) px[i] += (sin(t * 1.3f + ph[i]) * 7f + 2f) * dt else px[i] += sin(t * 0.8f + ph[i]) * 2.5f * dt
            if (py[i] >= rows) { spawn(i); continue }
            if (px[i] < -4f) px[i] += cols + 8f
            if (px[i] > cols + 4f) px[i] -= cols + 8f
            val x = px[i].toInt() - 4; val y = py[i].toInt()
            if (kind == 2) {
                val b = br[i] * (0.55f + 0.45f * sin(t * 4f + ph[i]))
                plot(out, x, y, b); plot(out, x + 1, y, b * 0.8f); plot(out, x, y - 1, b * 0.4f)
                continue
            }
            plot(out, x, y, br[i])
            if (rain) for (k in 1..3) plot(out, x + k / 2, y - k, br[i] * (1f - k * 0.27f))
        }
    }
}

class BreathScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val cx = cols * 0.5f; val cy = rows * 0.5f
        val norm = 1f / sqrt(cx * cx + cy * cy * 4f)
        val breath = 0.5f + 0.5f * sin(t * 0.9f)
        for (r in 0 until rows) {
            val dy = (r - cy) * 2f
            for (c in 0 until cols) {
                val dx = c - cx
                val d = sqrt(dx * dx + dy * dy) * norm
                val v = (1f - d) * (0.35f + 0.65f * breath) + 0.18f * sin(d * 9f * density - t * 2f)
                out[r * cols + c] = if (v < 0f) 0f else if (v > 1f) 1f else v
            }
        }
    }
}
