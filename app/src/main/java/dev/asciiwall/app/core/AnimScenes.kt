package dev.asciiwall.app.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Локальное время суток в секундах без аллокаций (часовой пояс обновляется раз в 30 с). */
internal object WallClock {
    private var tzOffset = 0L
    private var tzStamp = 0L
    fun secondsOfDay(): Float {
        val now = System.currentTimeMillis()
        if (now - tzStamp > 30_000L) { tzOffset = java.util.TimeZone.getDefault().getOffset(now).toLong(); tzStamp = now }
        val ms = Math.floorMod(now + tzOffset, 86_400_000L)
        return ms / 1000f
    }
}

/** Центральные координаты: у клетки есть угол и радиус в квадратных единицах (радиус 1 = половина короткой стороны). */
abstract class PolarScene : Scene() {
    protected var ang = FloatArray(0)
    protected var rad = FloatArray(0)
    protected var sz = 1f
    override fun onResize() {
        ang = FloatArray(cols * rows); rad = FloatArray(cols * rows)
        sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        for (r in 0 until rows) for (c in 0 until cols) {
            val u = (c + 0.5f - cols * 0.5f) / sz
            val v = (r + 0.5f - rows * 0.5f) * 2f / sz
            ang[r * cols + c] = atan2(v, u); rad[r * cols + c] = sqrt(u * u + v * v)
        }
        init2()
    }
    protected open fun init2() {}
}

class VortexScene(private val arms: Int, private val speed: Float, private val hypno: Boolean) : PolarScene() {
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val tt = t * speed
        for (i in 0 until cols * rows) {
            val r = rad[i] + 0.04f
            val a = ang[i]
            val v = if (hypno) {
                val s = sin(r * 14f - tt * 3f + a * arms * 0.5f)
                0.5f + 0.5f * s
            } else {
                val s = sin(a * arms + 2.2f / r * 0.7f - tt * 2f + ln(r) * 4f)
                0.5f + 0.5f * s
            }
            out[i] = if (hypno) v else v * min(1f, 0.25f + r * 1.4f)
        }
    }
    private fun ln(x: Float) = kotlin.math.ln(x)
}

class TunnelScene : PolarScene() {
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        for (i in 0 until cols * rows) {
            val r = rad[i] + 0.02f
            val z = 0.4f / r
            val s = sin((z - t * 2.5f) * 3.2f) * sin(ang[i] * 6f + t * 0.6f + z * 0.5f)
            out[i] = Gfx.clamp01((0.5f + 0.5f * s) * min(1f, r * 2.5f))
        }
    }
}

class RadarScene : PolarScene() {
    private val rnd = Random(21)
    private val bx = FloatArray(8); private val by = FloatArray(8)
    override fun init2() { for (i in 0 until 8) { val a = rnd.nextFloat() * TAUF; val r = 0.2f + rnd.nextFloat() * 0.7f; bx[i] = cos(a) * r; by[i] = sin(a) * r } }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val sweep = Gfx.fract(t * 0.25f) * TAUF - PIF
        for (i in 0 until cols * rows) {
            val r = rad[i]
            var v = 0f
            if (r <= 1f) {
                var d = sweep - ang[i]
                while (d < 0f) d += TAUF
                while (d >= TAUF) d -= TAUF
                v = if (d < 2.2f) exp(-d * 2.2f) * 0.8f else 0f
                val ring = Gfx.fract(r * 4f)
                if (ring < 0.04f || ring > 0.97f) v = max(v, 0.22f)
                if (r > 0.97f) v = max(v, 0.4f)
            }
            out[i] = v
        }
        val cx = cols * 0.5f; val cy = rows * 0.5f
        for (k in 0 until 8) {
            val a = atan2(by[k], bx[k])
            var d = sweep - a
            while (d < 0f) d += TAUF
            while (d >= TAUF) d -= TAUF
            val b = if (d < 4f) 1f - d / 4f else 0f
            if (b > 0.05f) dotf(out, cx + bx[k] * sz, cy + by[k] * sz * 0.5f, 0.4f + 0.6f * b)
        }
        dotf(out, cx, cy, 1f)
    }
}

class LavaScene : Scene() {
    private val n = 7
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        for (r in 0 until rows) for (c in 0 until cols) {
            val u = (c + 0.5f - cols * 0.5f) / sz
            val v = (r + 0.5f - rows * 0.5f) * 2f / sz
            var s = 0f
            for (k in 0 until n) {
                val bx = sin(t * (0.23f + 0.07f * k) + k * 1.7f) * 0.7f
                val by = cos(t * (0.19f + 0.05f * k) + k * 2.3f) * 0.85f
                val rr = 0.12f + 0.03f * (k % 3)
                val dx = u - bx; val dy = v - by
                s += rr * rr / (dx * dx + dy * dy + 0.002f)
            }
            out[r * cols + c] = Gfx.clamp01(s * 0.55f - 0.1f)
        }
    }
}

class OceanScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val hz = rows * 0.32f
        for (r in 0 until rows) for (c in 0 until cols) {
            val i = r * cols + c
            if (r < hz) { out[i] = if (Gfx.h01(c, r, 31) > 0.992f) 0.8f else 0.04f + 0.1f * (r / hz); continue }
            val k = (r - hz + 1f) / (rows - hz + 1f)
            val z = 1f / (k + 0.05f)
            val x = (c - cols * 0.5f) / cols * z * 3f
            val w = sin(z * 2.2f - t * 1.6f + x * 1.5f) + 0.5f * sin(z * 4.7f - t * 2.3f - x * 2f)
            var v = 0.25f + 0.2f * w + 0.3f * k
            if (w > 1.05f) v = 0.95f
            out[i] = Gfx.clamp01(v)
        }
    }
}

class AuroraScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        for (r in 0 until rows) {
            val fy = r.toFloat() / rows
            for (c in 0 until cols) {
                val fx = c.toFloat() / cols
                var v = if (Gfx.h01(c, r, 41) > 0.994f) 0.6f else 0f
                for (k in 0 until 3) {
                    val cy = 0.3f + 0.1f * k + 0.1f * sin(fx * (3f + k) + t * (0.3f + 0.1f * k) + k * 2f) + 0.05f * sin(fx * 11f + t * 0.8f)
                    val d = fy - cy
                    val w = if (d > 0f) exp(-d * 6f) else exp(d * 25f)
                    v += w * (0.55f + 0.45f * sin(fx * 40f + t * 1.3f + k)) * 0.6f
                }
                out[r * cols + c] = Gfx.clamp01(v)
            }
        }
    }
}

class MeteorScene : Scene() {
    private val rnd = Random(51)
    private val maxN = 24
    private val x = FloatArray(maxN); private val y = FloatArray(maxN); private val vx = FloatArray(maxN); private val vy = FloatArray(maxN)
    private val life = FloatArray(maxN)
    private var acc = 0f
    override fun onResize() { java.util.Arrays.fill(life, 0f) }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        for (i in 0 until cols * rows) out[i] = if (Gfx.h01(i % cols, i / cols, 52) > 0.99f) 0.3f else 0f
        acc += dt * 1.6f * density
        while (acc >= 1f) {
            acc -= 1f
            for (i in 0 until maxN) if (life[i] <= 0f) {
                x[i] = rnd.nextFloat() * cols * 1.3f; y[i] = -2f
                val sp = 25f + rnd.nextFloat() * 25f
                vx[i] = -sp * 0.7f; vy[i] = sp * 0.4f; life[i] = 1f
                break
            }
        }
        for (i in 0 until maxN) {
            if (life[i] <= 0f) continue
            x[i] += vx[i] * dt; y[i] += vy[i] * dt
            if (y[i] > rows || x[i] < -10f) { life[i] = 0f; continue }
            for (k in 0..9) dotf(out, x[i] - vx[i] * k * 0.012f, y[i] - vy[i] * k * 0.012f, 1f - k * 0.1f)
        }
    }
}

class GalaxyScene : Scene() {
    private val rnd = Random(61)
    private val n = 900
    private val rr = FloatArray(n); private val ph = FloatArray(n); private val br = FloatArray(n)
    override fun onResize() {
        for (i in 0 until n) {
            rr[i] = sqrt(rnd.nextFloat()) * 0.95f
            val arm = rnd.nextInt(2) * PIF
            ph[i] = arm + rr[i] * 5f + (rnd.nextFloat() - 0.5f) * (0.6f + 0.5f * (1f - rr[i]))
            br[i] = 0.3f + rnd.nextFloat() * 0.7f
        }
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        val cx = cols * 0.5f; val cy = rows * 0.5f
        val m = (n * min(1f, 0.4f + density * 0.6f)).toInt()
        for (i in 0 until m) {
            val a = ph[i] + t * 0.35f / (rr[i] + 0.15f)
            val x = cx + cos(a) * rr[i] * sz
            val y = cy + sin(a) * rr[i] * sz * 0.5f * 0.85f
            dotf(out, x, y, br[i] * (1f - rr[i] * 0.5f))
        }
        for (dy in -1..1) for (dx in -2..2) dot(out, (cx + dx).toInt(), (cy + dy).toInt(), if (dx == 0 && dy == 0) 1f else 0.6f)
    }
}

class AnalogClockScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f)) * 0.92f
        val cx = cols * 0.5f; val cy = rows * 0.5f
        val sx = sz; val sy = sz * 0.5f
        for (k in 0 until 60) {
            val a = k * TAUF / 60f
            val big = k % 5 == 0
            val r0 = if (big) 0.86f else 0.95f
            seg(out, cx + sin(a) * r0 * sx, cy - cos(a) * r0 * sy, cx + sin(a) * sx, cy - cos(a) * sy, if (big) 1f else 0.4f)
        }
        val s = WallClock.secondsOfDay()
        val sec = s % 60f; val min = (s / 60f) % 60f; val hr = (s / 3600f) % 12f
        hand(out, cx, cy, sx, sy, min / 60f * TAUF, 0.8f, 0.8f)
        hand(out, cx, cy, sx, sy, hr / 12f * TAUF, 0.5f, 1f)
        hand(out, cx, cy, sx, sy, sec / 60f * TAUF, 0.88f, 0.55f)
        dotf(out, cx, cy, 1f)
    }
    private fun hand(out: FloatArray, cx: Float, cy: Float, sx: Float, sy: Float, a: Float, len: Float, v: Float) {
        seg(out, cx, cy, cx + sin(a) * len * sx, cy - cos(a) * len * sy, v)
    }
}

class BinaryClockScene : Scene() {
    private val digits = IntArray(6)
    private val bits = intArrayOf(2, 4, 3, 4, 3, 4)
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val s = WallClock.secondsOfDay().toInt()
        val h = s / 3600; val m = (s / 60) % 60; val sec = s % 60
        digits[0] = h / 10; digits[1] = h % 10; digits[2] = m / 10; digits[3] = m % 10; digits[4] = sec / 10; digits[5] = sec % 10
        val cw = max(1, min(cols / 14, rows * 2 / 8)).coerceAtLeast(1)
        val bw = max(1, cw)
        val bh = max(1, min(rows / 6, cw / 2).coerceAtLeast(1))
        val gap = if (cw >= 3) 1 else 0
        val totW = 6 * bw + 5 * (bw / 2 + 1)
        val x0 = (cols - totW) / 2
        val y0 = (rows - 4 * (bh + gap)) / 2
        for (d in 0 until 6) {
            val x = x0 + d * (bw + bw / 2 + 1)
            for (b in 0 until bits[d]) {
                val on = (digits[d] shr b) and 1 == 1
                val y = y0 + (3 - b) * (bh + gap)
                rect(out, x, y, max(1, bw - gap), bh, if (on) 1f else 0.18f)
            }
        }
    }
}

class SnakeScene : Scene() {
    private val rnd = Random(71)
    private var gw = 1; private var gh = 1
    private var bodyX = IntArray(0); private var bodyY = IntArray(0)
    private var len = 3; private var head = 0
    private var fx = 0; private var fy = 0
    private var acc = 0f
    private var occ = BooleanArray(0)
    override fun onResize() {
        gw = max(2, cols / 2); gh = max(2, rows)
        val cap = gw * gh
        bodyX = IntArray(cap); bodyY = IntArray(cap); occ = BooleanArray(cap)
        reset()
    }
    private fun reset() {
        java.util.Arrays.fill(occ, false)
        len = 3; head = 2
        for (i in 0 until 3) { bodyX[i] = gw / 2 - 2 + i; bodyY[i] = gh / 2; occ[bodyY[i] * gw + bodyX[i]] = true }
        food()
    }
    private fun food() {
        for (k in 0 until 40) {
            val x = rnd.nextInt(gw); val y = rnd.nextInt(gh)
            if (!occ[y * gw + x]) { fx = x; fy = y; return }
        }
        fx = 0; fy = 0
    }
    private fun free(x: Int, y: Int) = x in 0 until gw && y in 0 until gh && !occ[y * gw + x]
    private fun step() {
        val cap = bodyX.size
        val hx = bodyX[head % cap]; val hy = bodyY[head % cap]
        var best = -1; var bd = Int.MAX_VALUE
        for (d in 0 until 4) {
            val nx = hx + (if (d == 0) 1 else if (d == 1) -1 else 0); val ny = hy + (if (d == 2) 1 else if (d == 3) -1 else 0)
            if (!free(nx, ny)) continue
            val dist = abs(nx - fx) + abs(ny - fy)
            var open = 0
            if (free(nx + 1, ny)) open++; if (free(nx - 1, ny)) open++; if (free(nx, ny + 1)) open++; if (free(nx, ny - 1)) open++
            val score = dist * 4 - open
            if (score < bd) { bd = score; best = d }
        }
        if (best < 0) { reset(); return }
        val nx = hx + (if (best == 0) 1 else if (best == 1) -1 else 0); val ny = hy + (if (best == 2) 1 else if (best == 3) -1 else 0)
        head++
        bodyX[head % cap] = nx; bodyY[head % cap] = ny; occ[ny * gw + nx] = true
        if (nx == fx && ny == fy) { len++; if (len >= cap - 2) { reset(); return }; food() }
        else {
            val ti = (head - len) % cap
            val t2 = if (ti < 0) ti + cap else ti
            occ[bodyY[t2] * gw + bodyX[t2]] = false
        }
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        acc += dt * 14f
        var guard = 0
        while (acc >= 1f && guard++ < 6) { acc -= 1f; step() }
        if (acc > 6f) acc = 0f
        wipe(out)
        val cap = bodyX.size
        for (k in 0 until len) {
            val i = (head - k) % cap
            val ii = if (i < 0) i + cap else i
            val v = if (k == 0) 1f else 0.35f + 0.5f * (1f - k.toFloat() / len)
            rect(out, bodyX[ii] * 2, bodyY[ii], 2, 1, v)
        }
        val blink = if (((acc * 2f).toInt() and 1) == 0) 1f else 0.6f
        rect(out, fx * 2, fy, 2, 1, blink)
    }
}

class PongScene : Scene() {
    private var bx = 0f; private var by = 0f; private var vx = 0f; private var vy = 0f
    private var pl = 0f; private var pr = 0f
    private val rnd = Random(81)
    private var heat = FloatArray(0)
    override fun onResize() { heat = FloatArray(cols * rows); serve() }
    private fun serve() {
        bx = cols * 0.5f; by = rows * 0.5f
        val sp = max(20f, cols * 0.35f)
        vx = if (rnd.nextBoolean()) sp else -sp
        vy = (rnd.nextFloat() - 0.5f) * sp * 0.4f
        pl = rows * 0.5f; pr = rows * 0.5f
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val d = min(dt, 0.05f)
        bx += vx * d; by += vy * d
        if (by < 0f) { by = -by; vy = abs(vy) }
        if (by > rows - 1f) { by = 2f * (rows - 1f) - by; vy = -abs(vy) }
        val ph = max(2f, rows * 0.22f)
        val sp = rows * 1.1f
        val tl = if (vx < 0f) by else rows * 0.5f
        val tr = if (vx > 0f) by else rows * 0.5f
        pl += (tl - pl).coerceIn(-sp * d, sp * d); pr += (tr - pr).coerceIn(-sp * d, sp * d)
        if (bx < 2f) { if (abs(by - pl) <= ph * 0.5f + 1f) { vx = abs(vx) * 1.03f; vy += (by - pl) * 2f; bx = 2f } else { serve() } }
        if (bx > cols - 3f) { if (abs(by - pr) <= ph * 0.5f + 1f) { vx = -abs(vx) * 1.03f; vy += (by - pr) * 2f; bx = cols - 3f } else { serve() } }
        vy = vy.coerceIn(-cols * 0.6f, cols * 0.6f)
        vx = vx.coerceIn(-cols * 1.2f, cols * 1.2f)
        val dk = exp(-d * 4f)
        for (i in 0 until cols * rows) heat[i] *= dk
        dotf(heat, bx, by, 1f); dotf(heat, bx + 1f, by, 1f)
        for (i in 0 until cols * rows) out[i] = heat[i] * 0.7f
        for (r in 0 until rows step 2) dot(out, cols / 2, r, 0.3f)
        val h = ph.toInt()
        for (k in -h / 2..h / 2) { dot(out, 0, (pl + k).toInt(), 1f); dot(out, 1, (pl + k).toInt(), 1f); dot(out, cols - 1, (pr + k).toInt(), 1f); dot(out, cols - 2, (pr + k).toInt(), 1f) }
        dotf(out, bx, by, 1f); dotf(out, bx + 1f, by, 1f)
    }
}

class MazeScene : Scene() {
    private val rnd = Random(91)
    private var mw = 1; private var mh = 1
    private var wall = BooleanArray(0)
    private var seen = BooleanArray(0)
    private var stack = IntArray(0)
    private var sp = 0
    private var phase = 0
    private var hold = 0f
    private var acc = 0f
    private var cur = 0
    override fun onResize() {
        mw = max(1, (cols / 2 - 1) / 2); mh = max(1, (rows - 1) / 2)
        wall = BooleanArray(cols * rows); stack = IntArray(mw * mh + 1); seen = BooleanArray(mw * mh)
        restart()
    }
    private fun restart() {
        java.util.Arrays.fill(wall, true); java.util.Arrays.fill(seen, false)
        sp = 0; phase = 0; hold = 0f
        cur = 0; seen[0] = true; stack[sp++] = 0
        carve(0)
    }
    private fun cellX(i: Int) = (i % mw) * 4 + 2
    private fun cellY(i: Int) = (i / mw) * 2 + 1
    private fun carve(i: Int) { val x = cellX(i); val y = cellY(i); for (k in 0..1) setW(x + k, y, false) }
    private fun setW(x: Int, y: Int, v: Boolean) { if (x in 0 until cols && y in 0 until rows) wall[y * cols + x] = v }
    private fun stepGen() {
        if (sp == 0) { phase = 1; return }
        val i = stack[sp - 1]
        val cx = i % mw; val cy = i / mw
        var nb = 0; var pick = -1
        for (d in 0 until 4) {
            val nx = cx + (if (d == 0) 1 else if (d == 1) -1 else 0); val ny = cy + (if (d == 2) 1 else if (d == 3) -1 else 0)
            if (nx < 0 || ny < 0 || nx >= mw || ny >= mh || seen[ny * mw + nx]) continue
            nb++
            if (rnd.nextInt(nb) == 0) pick = d
        }
        if (pick < 0) { sp--; return }
        val nx = cx + (if (pick == 0) 1 else if (pick == 1) -1 else 0); val ny = cy + (if (pick == 2) 1 else if (pick == 3) -1 else 0)
        val j = ny * mw + nx
        seen[j] = true; stack[sp++] = j
        val ax = cellX(i); val ay = cellY(i); val bx = cellX(j); val by = cellY(j)
        val minx = min(ax, bx); val maxx = max(ax, bx) + 1; val miny = min(ay, by); val maxy = max(ay, by)
        for (y in miny..maxy) for (x in minx..maxx) setW(x, y, false)
        cur = j
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        if (phase == 0) {
            acc += dt * (mw * mh * 0.9f).coerceIn(30f, 400f)
            var g = 0
            while (acc >= 1f && phase == 0 && g++ < 200) { acc -= 1f; stepGen() }
        } else { hold += dt; if (hold > 4f) restart() }
        for (i in 0 until cols * rows) out[i] = if (wall[i]) 0.55f else 0f
        if (phase == 0 && sp > 0) {
            for (k in 0 until sp) { val i = stack[k]; rect(out, cellX(i), cellY(i), 2, 1, 0.25f) }
            rect(out, cellX(cur), cellY(cur), 2, 1, 1f)
        } else if (phase == 1) {
            rect(out, cellX(0), cellY(0), 2, 1, 1f)
            rect(out, cellX(mw * mh - 1), cellY(mw * mh - 1), 2, 1, 0.9f)
        }
    }
}
