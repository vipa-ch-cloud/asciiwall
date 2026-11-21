package dev.asciiwall.app.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MandelZoomScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        val ph = 0.5f - 0.5f * cos(t * 0.18f)
        val scale = 1.6f * exp(-ph * 9f)
        val cx0 = -0.743643887f; val cy0 = 0.131825904f
        val maxIt = 60 + (ph * 140f).toInt()
        for (r in 0 until rows) for (c in 0 until cols) {
            val cr = cx0 + (c + 0.5f - cols * 0.5f) / sz * scale
            val ci = cy0 + (r + 0.5f - rows * 0.5f) * 2f / sz * scale
            var zr = 0f; var zi = 0f; var n = 0
            while (n < maxIt && zr * zr + zi * zi <= 16f) { val q = zr * zr - zi * zi + cr; zi = 2f * zr * zi + ci; zr = q; n++ }
            out[r * cols + c] = if (n >= maxIt) 0f else 0.12f + 0.88f * (0.5f + 0.5f * sin(sqrt(n.toFloat()) * 2.2f - t))
        }
    }
}

class JuliaAnimScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        val cr = 0.7885f * cos(t * 0.3f); val ci = 0.7885f * sin(t * 0.3f)
        for (r in 0 until rows) for (c in 0 until cols) {
            var zr = (c + 0.5f - cols * 0.5f) / sz * 1.5f; var zi = (r + 0.5f - rows * 0.5f) * 2f / sz * 1.5f
            var n = 0
            while (n < 48 && zr * zr + zi * zi <= 16f) { val q = zr * zr - zi * zi + cr; zi = 2f * zr * zi + ci; zr = q; n++ }
            out[r * cols + c] = if (n >= 48) 0.04f else 0.15f + 0.85f * (n / 48f)
        }
    }
}

/** Базовый класс «кривая с затуханием следа». */
abstract class TrailScene : Scene() {
    protected var heat = FloatArray(0)
    protected var sz = 1f
    override fun onResize() { heat = FloatArray(cols * rows); sz = max(1f, min(rows.toFloat(), cols * 0.5f)) }
    protected abstract fun point(t: Float, k: Int, o: FloatArray)
    protected open val steps = 12
    private val pt = FloatArray(2)
    private var last = 0f
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val dk = exp(-dt * 1.6f)
        for (i in 0 until cols * rows) heat[i] *= dk
        val d = if (dt <= 0f) 0.03f else min(dt, 0.1f)
        val n = steps * 8
        for (k in 0..n) {
            val tt = t - d + d * k / n
            point(tt, 0, pt)
            val x = cols * 0.5f + pt[0] * sz * 0.92f; val y = rows * 0.5f + pt[1] * sz * 0.46f
            dotf(heat, x, y, 1f)
        }
        System.arraycopy(heat, 0, out, 0, cols * rows)
    }
}

class LissajousScene : TrailScene() {
    override fun point(t: Float, k: Int, o: FloatArray) {
        val a = 3f + 2f * sin(t * 0.05f); val b = 2f
        o[0] = sin(a * t * 0.9f + 1.2f); o[1] = sin(b * t * 0.9f)
    }
    override val steps get() = 40
}

class SpirographScene : TrailScene() {
    override fun point(t: Float, k: Int, o: FloatArray) {
        val tt = t * 3.5f
        val R = 1f; val r = 0.36f + 0.1f * sin(t * 0.1f); val dd = 0.7f
        val x = (R - r) * cos(tt) + dd * r * cos((R - r) / r * tt)
        val y = (R - r) * sin(tt) - dd * r * sin((R - r) / r * tt)
        o[0] = x / 1.4f; o[1] = y / 1.4f
    }
    override val steps get() = 40
}

class PendulumScene : TrailScene() {
    override fun point(t: Float, k: Int, o: FloatArray) {
        val a1 = 1.2f * sin(t * 1.7f); val a2 = a1 + 1.0f * sin(t * 2.9f + 1f)
        o[0] = 0.45f * sin(a1) + 0.45f * sin(a2)
        o[1] = -0.9f + 0.45f * cos(a1) + 0.45f * cos(a2) + 0.5f
    }
    override val steps get() = 30
}

class StormScene : Scene() {
    private val rain = ParticleScene(1)
    private val rnd = Random(101)
    private var flash = 0f
    private var next = 2f
    private var bolt = FloatArray(0)
    private var bx = 0
    override fun onResize() { rain.resize(cols, rows); bolt = FloatArray(rows); }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        rain.update(t, dt, density * 1.4f, out)
        for (i in 0 until cols * rows) out[i] *= 0.7f
        flash = max(0f, flash - dt * 2.5f)
        next -= dt
        if (next <= 0f) {
            next = 2f + rnd.nextFloat() * 4f; flash = 1f
            var x = cols * (0.2f + rnd.nextFloat() * 0.6f)
            for (r in 0 until rows) { bolt[r] = x; x += (rnd.nextFloat() - 0.5f) * 3f }
            bx = 1
        }
        if (flash > 0.3f && bx == 1) for (r in 0 until rows) { dotf(out, bolt[r], r.toFloat(), 1f); dotf(out, bolt[r] + 1f, r.toFloat(), flash) }
        if (flash > 0f) for (i in 0 until cols * rows) out[i] = min(1f, out[i] + flash * 0.3f)
    }
}

class FirefliesScene : Scene() {
    private val rnd = Random(111)
    private val n = 70
    private val x = FloatArray(n); private val y = FloatArray(n); private val ph = FloatArray(n); private val sp = FloatArray(n)
    override fun onResize() { for (i in 0 until n) { x[i] = rnd.nextFloat() * cols; y[i] = rnd.nextFloat() * rows; ph[i] = rnd.nextFloat() * 6.28f; sp[i] = 0.4f + rnd.nextFloat() }; }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val m = (n * min(1f, density)).toInt().coerceAtLeast(8)
        for (i in 0 until m) {
            x[i] += sin(t * 0.6f * sp[i] + ph[i] * 3f) * 4f * dt
            y[i] += cos(t * 0.5f * sp[i] + ph[i]) * 2.5f * dt
            if (x[i] < 0f) x[i] += cols; if (x[i] >= cols) x[i] -= cols
            if (y[i] < 0f) y[i] += rows; if (y[i] >= rows) y[i] -= rows
            val b = max(0f, sin(t * sp[i] * 1.5f + ph[i]))
            val bb = b * b * b
            dotf(out, x[i], y[i], 0.25f + 0.75f * bb)
            if (bb > 0.4f) { dotf(out, x[i] + 1f, y[i], bb * 0.5f); dotf(out, x[i] - 1f, y[i], bb * 0.5f) }
        }
    }
}

class DnaScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val amp = cols * 0.22f
        for (r in 0 until rows) {
            val a = r * 0.45f - t * 2f
            val x1 = cols * 0.5f + sin(a) * amp; val x2 = cols * 0.5f - sin(a) * amp
            val z1 = cos(a)
            dotf(out, x1, r.toFloat(), 0.55f + 0.45f * z1); dotf(out, x1 + 1f, r.toFloat(), 0.4f + 0.4f * z1)
            dotf(out, x2, r.toFloat(), 0.55f - 0.45f * z1); dotf(out, x2 + 1f, r.toFloat(), 0.4f - 0.4f * z1)
            if (r % 3 == 0) {
                val lo = min(x1, x2).toInt(); val hi = max(x1, x2).toInt()
                for (x in lo + 1 until hi) dot(out, x, r, 0.3f)
            }
        }
    }
}

class BrainfuckScene : Scene() {
    private val prog = "++++++++[>++++[>++>+++>+++>+<<<<-]>+>+>->>+[<]<-]>>.>---.+++++++..+++.>>.<-.<.+++.------.--------.>>+.>++."
    private val code = prog.toCharArray()
    private val jump = IntArray(code.size)
    private val tape = IntArray(64)
    private var ip = 0; private var dp = 0
    private var acc = 0f
    private var tick = 0
    private val flash = FloatArray(64)
    init {
        val st = IntArray(code.size); var sp = 0
        for (i in code.indices) { if (code[i] == '[') st[sp++] = i else if (code[i] == ']') { val j = st[--sp]; jump[i] = j; jump[j] = i } }
    }
    override fun onResize() { reset() }
    private fun reset() { java.util.Arrays.fill(tape, 0); java.util.Arrays.fill(flash, 0f); ip = 0; dp = 0 }
    private fun step() {
        if (ip >= code.size) { tick++; if (tick > 30) { tick = 0; reset() }; return }
        when (code[ip]) {
            '>' -> dp = (dp + 1) % tape.size
            '<' -> dp = (dp + tape.size - 1) % tape.size
            '+' -> { tape[dp] = (tape[dp] + 1) and 255; flash[dp] = 1f }
            '-' -> { tape[dp] = (tape[dp] - 1) and 255; flash[dp] = 1f }
            '[' -> if (tape[dp] == 0) ip = jump[ip]
            ']' -> if (tape[dp] != 0) ip = jump[ip]
        }
        ip++
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        acc += dt * 60f
        var g = 0
        while (acc >= 1f && g++ < 8) { acc -= 1f; step() }
        if (acc > 8f) acc = 0f
        wipe(out)
        val cellW = max(1, cols / 32)
        val n = min(tape.size, max(1, cols / cellW))
        val baseY = rows * 3 / 4
        val maxH = max(2, rows / 2)
        for (i in 0 until n) {
            flash[i] = max(0f, flash[i] - dt * 3f)
            val h = tape[i] * maxH / 255 + (if (tape[i] > 0) 1 else 0)
            for (k in 0 until h) rect(out, i * cellW, baseY - k, max(1, cellW - 1), 1, 0.35f + 0.4f * k / maxH + flash[i] * 0.3f)
            if (i == dp) rect(out, i * cellW, baseY + 1, max(1, cellW - 1), 1, 1f)
        }
        // лента программы
        val ribbon = max(1, rows / 5)
        val w = cols
        for (x in 0 until w) {
            val idx = ip - w / 2 + x
            if (idx < 0 || idx >= code.size) continue
            val v = if (idx == ip) 1f else 0.3f + 0.25f * (code[idx].code % 3)
            for (y in 0 until max(1, ribbon / 2)) dot(out, x, y + 1, v * (if (idx == ip) 1f else 0.7f))
        }
    }
}

class AquariumScene : Scene() {
    private val rnd = Random(121)
    private val nf = 7
    private val fx = FloatArray(nf); private val fy = FloatArray(nf); private val fv = FloatArray(nf); private val fs = FloatArray(nf); private val fb = FloatArray(nf)
    private val nb = 24
    private val bx = FloatArray(nb); private val by = FloatArray(nb); private val bv = FloatArray(nb)
    override fun onResize() {
        for (i in 0 until nf) { fx[i] = rnd.nextFloat() * cols; fy[i] = 1f + rnd.nextFloat() * (rows * 0.7f); fv[i] = (3f + rnd.nextFloat() * 6f) * (if (rnd.nextBoolean()) 1f else -1f); fs[i] = 0.5f + rnd.nextFloat() * 1f; fb[i] = 0.5f + rnd.nextFloat() * 0.5f }
        for (i in 0 until nb) { bx[i] = rnd.nextFloat() * cols; by[i] = rnd.nextFloat() * rows; bv[i] = 3f + rnd.nextFloat() * 5f }
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        for (r in 0 until rows) for (c in 0 until cols) out[r * cols + c] = 0.05f + 0.12f * (1f - r.toFloat() / rows) * (0.6f + 0.4f * sin(c * 0.2f + t * 0.5f + r * 0.1f))
        for (i in 0 until nb) {
            by[i] -= bv[i] * dt; bx[i] += sin(t * 2f + i) * dt
            if (by[i] < 0f) { by[i] = rows.toFloat(); bx[i] = rnd.nextFloat() * cols }
            dotf(out, bx[i], by[i], 0.65f)
        }
        // водоросли
        var c = 2
        while (c < cols) {
            val h = rows * (0.2f + 0.25f * Gfx.h01(c, 5, 7))
            var k = 0f
            while (k < h) { dotf(out, c + sin(t * 1.2f + k * 0.4f + c) * (k / h) * 1.5f, rows - 1 - k, 0.45f + 0.2f * (k / h)); k += 1f }
            c += max(5, cols / 10)
        }
        for (i in 0 until nf) {
            fx[i] += fv[i] * dt
            if (fv[i] > 0f && fx[i] > cols + 6f) { fx[i] = -6f; fy[i] = 1f + rnd.nextFloat() * (rows * 0.7f) }
            if (fv[i] < 0f && fx[i] < -6f) { fx[i] = cols + 6f; fy[i] = 1f + rnd.nextFloat() * (rows * 0.7f) }
            val d = if (fv[i] > 0f) 1f else -1f
            val len = 3f + fs[i] * 2f
            for (k in -len.toInt()..len.toInt()) {
                val body = 1f - (k / (len + 0.5f)) * (k / (len + 0.5f))
                val hh = body * (0.4f + fs[i] * 0.8f)
                var dy = -hh
                while (dy <= hh) { dotf(out, fx[i] + k * d, fy[i] + dy, fb[i]); dy += 1f }
            }
            val tail = sin(t * 8f + i) * 0.7f
            dotf(out, fx[i] - (len + 1f) * d, fy[i] + tail - 0.5f, fb[i] * 0.8f); dotf(out, fx[i] - (len + 1f) * d, fy[i] + tail + 0.5f, fb[i] * 0.8f)
            dotf(out, fx[i] + (len - 1.5f) * d, fy[i] - 0.2f, 0.1f)
        }
    }
}

class EcgScene : Scene() {
    private var trace = FloatArray(0)
    override fun onResize() { trace = FloatArray(cols); }
    private fun g(x: Float, m: Float, w: Float) = exp(-((x - m) / w) * ((x - m) / w))
    private fun beat(p: Float): Float =
        0.12f * g(p, 0.15f, 0.04f) - 0.1f * g(p, 0.28f, 0.012f) + 1f * g(p, 0.31f, 0.012f) - 0.22f * g(p, 0.34f, 0.014f) + 0.25f * g(p, 0.55f, 0.06f)
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val period = max(10f, cols * 0.4f)
        val sweep = (t * cols * 0.2f) % cols
        val mid = rows * 0.62f; val amp = rows * 0.5f
        for (r in 0 until rows step 4) for (c in 0 until cols step 4) dot(out, c, r, 0.12f)
        for (c in 0 until cols) {
            val xx = c.toFloat()
            val p = Gfx.fract(xx / period)
            val y = mid - beat(p) * amp
            var d = sweep - xx; if (d < 0f) d += cols
            val b = if (d < cols * 0.7f) 1f - d / (cols * 0.7f) else 0f
            if (b > 0.02f) {
                val y2 = mid - beat(Gfx.fract((xx + 1f) / period)) * amp
                seg(out, xx, y, xx + 1f, y2, b)
            }
        }
        dotf(out, sweep, mid - beat(Gfx.fract(sweep / period)) * amp, 1f)
    }
}

class EqualizerScene : Scene() {
    private var peak = FloatArray(0); private var h = FloatArray(0)
    override fun onResize() { val n = max(1, cols / 3); peak = FloatArray(n); h = FloatArray(n) }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        val n = h.size
        for (i in 0 until n) {
            val f = i.toFloat() / n
            val target = (0.5f + 0.5f * sin(t * (2.1f + 3f * f) + i * 1.3f)) * (0.5f + 0.5f * sin(t * 0.9f + i * 0.4f)) * (1f - 0.5f * f) + 0.12f * Gfx.h01(i, (t * 12f).toInt(), 3)
            h[i] += (target - h[i]) * min(1f, dt * 10f)
            peak[i] = max(h[i], peak[i] - dt * 0.3f)
            val hh = (h[i] * rows * 0.95f).toInt()
            for (k in 0 until hh) rect(out, i * 3, rows - 1 - k, 2, 1, 0.35f + 0.65f * k / max(1, rows))
            rect(out, i * 3, rows - 1 - (peak[i] * rows * 0.95f).toInt(), 2, 1, 1f)
        }
    }
}

class RipplesScene : Scene() {
    private val rnd = Random(131)
    private val n = 6
    private val rx = FloatArray(n); private val ry = FloatArray(n); private val age = FloatArray(n) { 99f }
    private var next = 0f
    override fun onResize() { java.util.Arrays.fill(age, 99f) }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        next -= dt
        if (next <= 0f) {
            next = (0.5f + rnd.nextFloat()) / max(0.3f, density)
            var j = 0; for (i in 0 until n) if (age[i] > age[j]) j = i
            rx[j] = rnd.nextFloat() * cols; ry[j] = rnd.nextFloat() * rows; age[j] = 0f
        }
        for (i in 0 until n) age[i] += dt
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        for (r in 0 until rows) for (c in 0 until cols) {
            var v = 0.08f
            for (i in 0 until n) {
                if (age[i] > 5f) continue
                val dx = (c - rx[i]) / sz; val dy = (r - ry[i]) * 2f / sz
                val d = sqrt(dx * dx + dy * dy)
                val rr = age[i] * 0.3f
                val w = d - rr
                v += exp(-w * w * 120f) * (1f - age[i] / 5f) * 0.9f * (0.6f + 0.4f * cos(w * 60f))
            }
            out[r * cols + c] = Gfx.clamp01(v)
        }
    }
}

class CubeScene : Scene() {
    private val vx = floatArrayOf(-1f, 1f, 1f, -1f, -1f, 1f, 1f, -1f)
    private val vy = floatArrayOf(-1f, -1f, 1f, 1f, -1f, -1f, 1f, 1f)
    private val vz = floatArrayOf(-1f, -1f, -1f, -1f, 1f, 1f, 1f, 1f)
    private val px = FloatArray(8); private val py = FloatArray(8)
    private val edges = intArrayOf(0, 1, 1, 2, 2, 3, 3, 0, 4, 5, 5, 6, 6, 7, 7, 4, 0, 4, 1, 5, 2, 6, 3, 7)
    override fun onResize() {}
    private fun draw(out: FloatArray, t: Float, s: Float, v: Float) {
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        val ca = cos(t * 0.8f); val sa = sin(t * 0.8f); val cb = cos(t * 0.55f); val sb = sin(t * 0.55f)
        for (i in 0 until 8) {
            var x = vx[i] * s; var y = vy[i] * s; var z = vz[i] * s
            val x1 = x * ca + z * sa; val z1 = -x * sa + z * ca
            val y1 = y * cb - z1 * sb; val z2 = y * sb + z1 * cb
            val k = 1f / (3.2f - z2 * 0.5f)
            px[i] = cols * 0.5f + x1 * k * sz * 1.6f
            py[i] = rows * 0.5f + y1 * k * sz * 0.8f
        }
        var e = 0
        while (e < edges.size) { seg(out, px[edges[e]], py[edges[e]], px[edges[e + 1]], py[edges[e + 1]], v); e += 2 }
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        draw(out, t, 1f, 1f)
        draw(out, -t * 1.3f + 1f, 0.5f, 0.55f)
    }
}

class GlobeScene : Scene() {
    private var lon = FloatArray(0); private var lat = FloatArray(0); private var lit = FloatArray(0)
    override fun onResize() {
        lon = FloatArray(cols * rows); lat = FloatArray(cols * rows); lit = FloatArray(cols * rows)
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        val R = 0.9f
        for (r in 0 until rows) for (c in 0 until cols) {
            val u = (c + 0.5f - cols * 0.5f) / sz / R; val v = (r + 0.5f - rows * 0.5f) * 2f / sz / R
            val d2 = u * u + v * v
            val i = r * cols + c
            if (d2 > 1f) { lit[i] = -1f; continue }
            val z = sqrt(1f - d2)
            lat[i] = kotlin.math.asin(-v); lon[i] = kotlin.math.atan2(u, z)
            lit[i] = max(0.1f, -0.4f * u - 0.2f * v + 0.9f * z)
        }
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val rot = t * 0.4f
        for (i in 0 until cols * rows) {
            if (lit[i] < 0f) { out[i] = if (Gfx.h01(i % cols, i / cols, 61) > 0.99f) 0.5f else 0f; continue }
            val lo = lon[i] + rot; val la = lat[i]
            val n = Gfx.fbm(cos(lo) * 1.6f + 4f + la * 0.8f, sin(lo) * 1.6f + la * 2f, 21, 4)
            var o = if (n > 0.52f) 0.6f + 0.4f * (n - 0.52f) * 3f else 0.2f
            val gl = Gfx.fract(lo * 3f / PIF); val gla = Gfx.fract(la * 3f / PIF)
            if (gl < 0.04f || gla < 0.04f) o = max(o, 0.45f)
            out[i] = Gfx.clamp01(o * lit[i])
        }
    }
}

class FireworksScene : Scene() {
    private val rnd = Random(141)
    private val maxP = 360
    private val px = FloatArray(maxP); private val py = FloatArray(maxP); private val vx = FloatArray(maxP); private val vy = FloatArray(maxP); private val life = FloatArray(maxP)
    private var rx = 0f; private var ry = 0f; private var rvy = 0f; private var rock = false; private var tgt = 0f
    private var wait = 0.5f
    private var cursor = 0
    override fun onResize() { java.util.Arrays.fill(life, 0f); rock = false }
    private fun burst(x: Float, y: Float) {
        val n = 70
        for (k in 0 until n) {
            val a = k * TAUF / n; val s = (0.5f + rnd.nextFloat() * 0.5f) * cols * 0.18f
            val j = cursor; cursor = (cursor + 1) % maxP
            px[j] = x; py[j] = y; vx[j] = cos(a) * s; vy[j] = sin(a) * s * 0.5f; life[j] = 0.9f + rnd.nextFloat() * 0.8f
        }
    }
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        wipe(out)
        wait -= dt
        if (!rock && wait <= 0f) { rock = true; rx = cols * (0.2f + 0.6f * rnd.nextFloat()); ry = rows - 1f; tgt = rows * (0.15f + 0.3f * rnd.nextFloat()); rvy = rows * 0.9f }
        if (rock) {
            ry -= rvy * dt
            dotf(out, rx, ry, 1f); dotf(out, rx, ry + 1f, 0.5f); dotf(out, rx, ry + 2f, 0.25f)
            if (ry <= tgt) { rock = false; burst(rx, ry); wait = (0.8f + rnd.nextFloat() * 1.5f) / max(0.4f, density) }
        }
        for (i in 0 until maxP) {
            if (life[i] <= 0f) continue
            life[i] -= dt
            px[i] += vx[i] * dt; py[i] += vy[i] * dt; vy[i] += rows * 0.5f * dt; vx[i] *= (1f - dt * 0.8f)
            dotf(out, px[i], py[i], min(1f, life[i] * 1.2f))
        }
    }
}

class CloudsScene : Scene() {
    override fun onResize() {}
    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        val sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        for (r in 0 until rows) for (c in 0 until cols) {
            val u = c / sz * 1.3f + t * 0.12f; val v = r * 2f / sz * 1.3f
            val n = Gfx.fbm(u, v + 0.3f * sin(t * 0.1f), 71, 3)
            out[r * cols + c] = Gfx.smooth(0.38f, 0.8f, n)
        }
    }
}
