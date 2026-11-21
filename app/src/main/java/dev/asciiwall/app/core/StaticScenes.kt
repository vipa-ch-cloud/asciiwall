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
import kotlin.math.asin

/**
 * Статичная картинка: кадр считается один раз в onResize(), update() только копирует его.
 * Координаты: u — по горизонтали, v — по вертикали, обе в квадратных единицах (клетка вдвое выше ширины),
 * [-1..1] вписывается в экран; fx, fy — доли ширины и высоты 0..1.
 */
abstract class StaticScene : Scene() {
    private var img = FloatArray(0)
    protected var sz = 1f
        private set

    protected fun uOf(c: Int) = (c + 0.5f - cols * 0.5f) / sz
    protected fun vOf(r: Int) = (r + 0.5f - rows * 0.5f) * 2f / sz
    protected fun vOfFy(fy: Float) = (fy - 0.5f) * 2f * rows / sz
    protected fun colOfU(u: Float) = cols * 0.5f + u * sz
    protected fun rowOfV(v: Float) = rows * 0.5f + v * sz * 0.5f

    override fun onResize() {
        img = FloatArray(cols * rows)
        sz = max(1f, min(rows.toFloat(), cols * 0.5f))
        paint(img)
        for (i in img.indices) { val x = img[i]; img[i] = if (x.isNaN()) 0f else if (x < 0f) 0f else if (x > 1f) 1f else x }
    }

    protected open fun paint(img: FloatArray) {
        for (r in 0 until rows) for (c in 0 until cols)
            img[r * cols + c] = shade(c, r, uOf(c), vOf(r), (c + 0.5f) / cols, (r + 0.5f) / rows)
    }

    protected open fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float = 0f

    override fun update(t: Float, dt: Float, density: Float, out: FloatArray) {
        System.arraycopy(img, 0, out, 0, cols * rows)
    }

    protected fun stars(c: Int, r: Int, seed: Int = 11, thr: Float = 0.993f): Float {
        val h = Gfx.h01(c, r, seed)
        return if (h > thr) 0.35f + (h - thr) / (1f - thr) * 0.5f else 0f
    }
}

class MountainsScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.03f + 0.10f * fy
        if (fy < 0.5f) o = max(o, stars(c, r))
        val du = u - 0.45f; val dv = v + 0.35f
        val d = sqrt(du * du + dv * dv)
        if (d < 0.22f) o = 0.95f else if (d < 0.7f) o = max(o, 0.3f * (1f - (d - 0.22f) / 0.48f))
        for (i in 0 until 4) {
            val y = 0.42f + 0.13f * i - (0.2f - 0.025f * i) * Gfx.fbm(fx * (2.2f + i * 1.1f) + i * 5.3f, 0.5f, 20 + i, 4)
            if (fy > y) o = if (i < 2 && fy - y < 0.03f) 0.95f else min(1f, 0.2f + 0.16f * i + (fy - y) * 0.7f)
        }
        return o
    }
}

class LakeScene : StaticScene() {
    private fun sky(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.03f + 0.14f * fy
        o = max(o, stars(c, r, 12, 0.99f))
        val du = u - 0.35f; val dv = v + 0.5f
        val d = sqrt(du * du + dv * dv)
        if (d < 0.16f) o = 1f else if (d < 0.6f) o = max(o, 0.3f * (1f - (d - 0.16f) / 0.44f))
        val y = 0.5f - 0.14f * Gfx.fbm(fx * 3f + 2f, 0.5f, 40, 4)
        if (fy > y) o = 0.1f + (fy - y) * 0.5f
        return o
    }

    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        if (fy < 0.6f) return sky(c, r, u, v, fx, fy)
        val fyr = 1.2f - fy
        val u2 = u + 0.03f * sin(r * 1.7f)
        val rip = 0.55f + 0.45f * sin(r * 2.3f + c * 0.15f)
        return 0.7f * sky(c, r, u2, vOfFy(fyr), fx, fyr) * rip + 0.03f
    }
}

class PinesScene : StaticScene() {
    private fun layer(fx: Float, fy: Float, n: Int, seed: Int, ground: Float, hMul: Float, asp: Float): Boolean {
        val k = fx * n
        val i = floor(k).toInt()
        val lx = (k - i - 0.5f) / asp           // -0.5..0.5 в долях ширины дерева
        val hh = (0.3f + 0.25f * Gfx.h01(i, seed)) * hMul
        val top = ground - hh
        if (fy < top || fy > ground + 0.02f) return false
        val t = (fy - top) / hh
        if (t > 0.9f) return abs(lx) < 0.05f
        val tier = Gfx.fract(t * 3.2f)
        val w = (0.1f + 0.4f * (t * 0.6f + tier * 0.4f)) * 1f
        return abs(lx) < w
    }

    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.04f + 0.10f * fy
        o = max(o, stars(c, r, 13, 0.992f))
        val du = u + 0.5f; val dv = v + 0.45f
        if (du * du + dv * dv < 0.03f) o = 1f
        val asp = max(0.4f, rows * 0.9f / max(1, cols) * 1.0f)
        val n1 = max(3, cols / 22)
        if (layer(fx, fy, n1, 5, 0.95f, 0.9f, asp)) o = 0.3f + 0.1f * Gfx.h01(c, r, 1)
        val n2 = max(4, cols / 14)
        if (layer(fx + 0.03f, fy, n2, 9, 1.0f, 0.75f, asp)) o = 0.6f + 0.15f * Gfx.h01(c, r, 2)
        if (fy > 0.93f) o = 0.7f + 0.1f * Gfx.h01(c, r, 3)
        return o
    }
}

class SnowflakeScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val rad = sqrt(u * u + v * v)
        var o = 0.04f * (1f - min(1f, rad)) + (if (rad > 1.1f) stars(c, r, 14, 0.985f) * 0.7f else 0f)
        if (rad > 0.95f) return o
        val th = atan2(v, u) + PI.toFloat()
        val seg = PIF / 3f
        val a = (th % seg) - seg * 0.5f
        val x = rad * cos(a); val y = rad * sin(a)
        val w = 1.1f / sz
        if (abs(y) < w * (1.6f - x) && x > 0.04f) o = 1f
        var x0 = 0.25f
        while (x0 < 0.8f) {
            val len = 0.32f * (1f - x0 * 0.7f)
            if (x > x0 && x < x0 + len) {
                val yy = (x - x0) * 0.85f
                if (abs(abs(y) - yy) < w * 1.4f) o = max(o, 0.85f)
            }
            x0 += 0.2f
        }
        if (rad < 0.06f) o = 1f
        return o
    }
}

class TopoMapScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val n = Gfx.fbm(u * 1.4f + 10f, v * 1.4f + 3f, 31, 4)
        val h = n * 12f
        val f = Gfx.fract(h)
        var o = 0.06f + 0.3f * n
        if (n < 0.42f) o = 0.04f + 0.1f * n
        val line = if (f < 0.09f) 1f else 0f
        val major = if (floor(h).toInt() % 4 == 0 && f < 0.12f) 1f else 0f
        o += line * 0.45f + major * 0.25f
        return o
    }
}

class CastleScene : StaticScene() {
    // cx, полуширина, верх башни
    private val tw = floatArrayOf(-0.62f, 0.11f, -0.35f, 0.62f, 0.11f, -0.35f, -0.28f, 0.09f, -0.55f, 0.28f, 0.09f, -0.55f, 0f, 0.14f, -0.78f)

    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.04f + 0.08f * fy
        o = max(o, stars(c, r, 15, 0.992f))
        val mx = u - 0.78f; val my = v + 0.62f
        val md = mx * mx + my * my
        if (md < 0.016f) o = 1f else if (md < 0.1f) o = max(o, 0.25f * (1f - md / 0.1f))
        val ground = 0.78f
        if (v > ground) return 0.25f + 0.12f * Gfx.vnoise(c * 0.3f, r * 0.5f, 3)
        var body = false
        var b = 0.5f
        if (abs(u) < 0.48f && v > -0.12f) body = true
        if (abs(u) < 0.48f && v > -0.2f && v <= -0.12f && ((u + 0.48f) / 0.08f).toInt() % 2 == 0) body = true
        var i = 0
        while (i < tw.size) {
            val tx = tw[i]; val hw = tw[i + 1]; val top = tw[i + 2]
            if (abs(u - tx) < hw && v > top) body = true
            val apex = top - hw * 2.6f
            if (v in apex..top && abs(u - tx) < (hw + 0.03f) * (v - apex) / (top - apex)) { body = true; b = 0.78f }
            if (abs(u - tx) < 0.018f && v > top + 0.12f && v < top + 0.2f) return 1f
            i += 3
        }
        if (!body) return o
        if (abs(u) < 0.1f && (v > 0.56f || u * u + (v - 0.56f) * (v - 0.56f) < 0.01f)) return 0.03f
        if (v > 0.02f && v < 0.6f && abs(u) < 0.46f) {
            val fu = Gfx.fract((u + 0.48f) / 0.16f); val fv = Gfx.fract(v / 0.2f)
            if (fu > 0.4f && fu < 0.65f && fv > 0.25f && fv < 0.7f && Gfx.h01((u * 6.25f).toInt() + 50, (v * 5f).toInt() + 50) > 0.4f) return 1f
        }
        return b * (0.85f + 0.15f * Gfx.h01(c / 2 + (r and 1) * 2, r, 4))
    }
}

class CityScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.04f + 0.22f * fy * fy
        o = max(o, stars(c, r, 16, 0.992f) * (1f - fy))
        if (u * u + (v + 0.55f) * (v + 0.55f) < 0.012f && fy < 0.5f) o = 1f
        for (layer in 0 until 2) {
            val bw = max(4, cols / (10 + layer * 6))
            val off = layer * (bw / 2)
            val idx = (c + off) / bw
            val h = 0.2f + 0.5f * Gfx.h01(idx, 3, layer) * (1f - 0.35f * layer)
            if (fy > 1f - h) {
                val local = (c + off) % bw
                if (layer == 0) o = 0.12f else {
                    o = 0.22f
                    if (local == 0) o = 0.4f
                    if (local % 3 == 1 && r % 3 == 1 && Gfx.h01((c + off) / 3, r / 3, idx) > 0.45f) o = 0.95f
                }
            }
        }
        return o
    }
}

class PyramidsScene : StaticScene() {
    private val px = floatArrayOf(-0.55f, 0.1f, 0.65f)
    private val hw = floatArrayOf(0.4f, 0.5f, 0.3f)
    private val ph = floatArrayOf(0.5f, 0.6f, 0.38f)

    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.05f + 0.35f * fy * fy * 1.5f
        o = max(o, stars(c, r, 17, 0.99f) * (1f - fy * 1.5f))
        val du = u + 0.15f; val dv = v + 0.15f
        val d = sqrt(du * du + dv * dv)
        if (d < 0.2f) o = 1f else if (d < 0.65f) o = max(o, 0.35f * (1f - (d - 0.2f) / 0.45f))
        val base = 0.45f
        for (i in 0 until 3) {
            val apex = base - ph[i]
            if (v in apex..base && abs(u - px[i]) < hw[i] * (v - apex) / ph[i]) {
                val lit = u < px[i]
                o = (if (lit) 0.78f else 0.38f) + (if ((r + c / 6) % 5 == 0) -0.08f else 0f)
            }
        }
        if (v > base) o = 0.3f + 0.08f * sin(u * 9f + v * 3f) + 0.06f * Gfx.vnoise(c * 0.2f, r * 0.7f, 5)
        val dune = 0.8f + 0.06f * sin(u * 5f + 1f)
        if (v > dune) o = 0.45f + 0.1f * sin(u * 7f)
        return o
    }
}

class CatScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.03f
        val au = abs(u); val s = if (u < 0f) -1f else 1f
        var head = Gfx.ell(u, v, 0f, 0.1f, 0.62f, 0.52f) < 1f
        var inner = false
        if (Gfx.tri(au, v, 0.15f, -0.35f, 0.62f, -0.2f, 0.52f, -0.85f)) { head = true; inner = Gfx.tri(au, v, 0.25f, -0.35f, 0.54f, -0.28f, 0.5f, -0.68f) }
        if (head) {
            o = 0.5f + 0.25f * Gfx.vnoise(c * 0.9f, r * 1.7f, 7)
            if (v < -0.2f && au < 0.3f && sin(u * 42f) > 0.6f && v > -0.55f) o = 0.2f
            if (inner) o = 0.85f
            val e = Gfx.ell(au, v, 0.25f, 0f, 0.15f, 0.1f)
            if (e < 1f) o = if (au in 0.225f..0.275f) 0.04f else 1f
            if (Gfx.tri(u, v, 0f, 0.17f, -0.07f, 0.1f, 0.07f, 0.1f)) o = 0.95f
            val m = Gfx.ell(au, v, 0.07f, 0.2f, 0.07f, 0.06f)
            if (m in 0.5f..1.2f && v > 0.19f) o = 0.1f
            if (v > 0.38f && Gfx.ell(u, v, 0f, 0.45f, 0.3f, 0.12f) < 1f) o = max(o, 0.8f)
        }
        for (k in -1..1) {
            val y0 = 0.2f + k * 0.09f + (au - 0.28f) * k * 0.3f
            if (au > 0.3f && au < 0.98f && abs(v - y0) < 1.1f / sz) o = max(o, 0.85f)
        }
        return o * (if (s > 0f) 1f else 1f)
    }
}

class OwlScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.03f
        val au = abs(u)
        if (v > 0.8f && v < 0.9f) o = 0.45f + 0.15f * Gfx.vnoise(c * 0.2f, r.toFloat(), 8)
        var body = Gfx.ell(u, v, 0f, 0.25f, 0.5f, 0.6f) < 1f
        if (Gfx.ell(au, v, 0.55f, 0.2f, 0.18f, 0.5f) < 1f) { o = 0.3f }
        val head = Gfx.ell(u, v, 0f, -0.35f, 0.5f, 0.4f) < 1f
        val tuft = Gfx.tri(au, v, 0.2f, -0.6f, 0.52f, -0.5f, 0.5f, -0.95f)
        if (body || head || tuft) {
            val sc = floor(v * 7f)
            val odd = ((floor(u * 9f + 0.5f * (sc.toInt() and 1)) + sc).toInt() and 1) == 0
            o = if (odd) 0.55f else 0.4f
            if (head && v < -0.1f) o = 0.5f + 0.15f * Gfx.vnoise(c * 0.8f, r * 1.6f, 9)
            if (body && v > 0.1f && au < 0.25f) o = 0.75f
            val e = Gfx.ell(au, v, 0.2f, -0.35f, 0.2f, 0.2f)
            if (e < 1f) o = 0.75f
            if (e < 0.5f) o = 1f
            if (e < 0.1f) o = 0.04f
            if (Gfx.tri(u, v, 0f, -0.15f, -0.07f, -0.3f, 0.07f, -0.3f)) o = 0.95f
            if (v > 0.8f && au in 0.12f..0.3f && v < 0.92f) o = 0.7f
        }
        if (v in 0.8f..0.9f && o < 0.4f) o = 0.45f
        return o
    }
}

class PenguinScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.02f
        val au = abs(u)
        if (v > 0.82f) o = 0.35f + 0.1f * Gfx.vnoise(c * 0.3f, r * 0.8f, 6)
        if (Gfx.ell(au, v, 0.2f, 0.85f, 0.25f, 0.07f) < 1f) o = 0.8f
        val body = Gfx.ell(u, v, 0f, 0.15f, 0.5f, 0.7f) < 1f
        val head = Gfx.ell(u, v, 0f, -0.45f, 0.33f, 0.3f) < 1f
        if (body || head) o = 0.3f
        if (Gfx.ell(au, v, 0.55f, 0.2f, 0.12f, 0.45f) < 1f && au > 0.38f) o = 0.35f
        if (Gfx.ell(u, v, 0f, 0.28f, 0.33f, 0.5f) < 1f && v > -0.15f) o = 0.95f
        if (Gfx.ell(u, v, 0f, -0.4f, 0.23f, 0.17f) < 1f) o = 0.95f
        if (Gfx.ell(au, v, 0.12f, -0.5f, 0.07f, 0.06f) < 1f) o = 0.95f
        if (Gfx.ell(au, v, 0.12f, -0.5f, 0.03f, 0.035f) < 1f) o = 0.03f
        if (Gfx.tri(u, v, 0f, -0.2f, -0.15f, -0.38f, 0.15f, -0.38f)) o = 0.7f
        return o
    }
}

class ButterflyScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.03f
        val au = abs(u)
        val t = atan2(au, -v)
        val rad = sqrt(u * u + v * v)
        val s5 = sin(t / 12f)
        val rr = (exp(cos(t)) - 2f * cos(4f * t) + s5 * s5 * s5 * s5 * s5) * 0.2f
        if (rad < rr) {
            val edge = rr - rad
            o = 0.35f + 0.4f * (0.5f + 0.5f * sin(rad * 28f)) 
            if (edge < 0.04f) o = 1f
            if (Gfx.ell(au, v, 0.45f, -0.2f, 0.1f, 0.1f) < 1f) o = 0.95f
        }
        if (Gfx.ell(u, v, 0f, 0.05f, 0.04f, 0.45f) < 1f) o = 1f
        if (Gfx.ell(u, v, 0f, -0.45f, 0.06f, 0.07f) < 1f) o = 1f
        if (abs(au - (-0.5f - v) * 0.5f - 0.03f) < 1.2f / sz && v < -0.5f && v > -0.9f) o = 0.9f
        return o
    }
}

class WebScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.02f
        val uu = u; val vv = v
        val rad = sqrt(uu * uu + vv * vv)
        if (rad > 1.0f) return o
        val n = 12
        val seg = TAUF / n
        val th = atan2(vv, uu) + PIF
        val a = (th % seg) - seg * 0.5f
        val w = 1.0f / sz
        if (abs(sin(a) * rad) < w * 0.9f) o = 0.7f
        val rp = rad * cos(seg * 0.5f) / cos(a)
        val ring = Gfx.fract(rp * 7f)
        if (rp < 0.95f && (ring < 0.1f || ring > 0.92f)) o = max(o, 0.85f)
        if (rad < 0.06f) o = 1f
        if (Gfx.h01((rp * 40f).toInt(), (th * 8f).toInt(), 2) > 0.995f && rp < 0.9f) o = 1f
        return o
    }
}

class EarthScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val R = 0.78f
        val rad2 = u * u + v * v
        if (rad2 > R * R) return stars(c, r, 18, 0.985f)
        val nx = u / R; val ny = v / R
        val nz = sqrt(max(0f, 1f - nx * nx - ny * ny))
        val lon = atan2(nx, nz) + 0.6f
        val lat = asin(-ny)
        val l = max(0f, -0.5f * nx - 0.4f * ny + 0.77f * nz)
        val n = Gfx.fbm(cos(lon) * 1.8f + 5f + lat * 0.7f, sin(lon) * 1.8f + lat * 2.2f, 21, 5)
        val land = n > 0.52f
        var o = if (land) 0.55f + 0.4f * (n - 0.52f) * 2f else 0.2f
        if (abs(lat) > 1.25f) o = 0.95f
        val cl = Gfx.fbm(cos(lon) * 2.4f + lat * 1.2f + 9f, sin(lon) * 2.4f + lat * 3f, 22, 4)
        if (cl > 0.6f) o = max(o, 0.75f)
        o *= 0.15f + 0.85f * l
        if (nz < 0.35f) o = max(o, 0.25f * (1f - nz / 0.35f) * (0.4f + l))
        return o
    }
}

class SaturnScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = stars(c, r, 19, 0.988f)
        val ca = cos(-0.38f); val sa = sin(-0.38f)
        val ur = u * ca - v * sa; val vr = u * sa + v * ca
        val R = 0.5f
        val d = sqrt(u * u + v * v)
        val e = sqrt(ur * ur + (vr / 0.27f) * (vr / 0.27f))
        val ringHit = e > 0.72f && e < 1.15f && !(e > 0.97f && e < 1.0f)
        val ringV = 0.35f + 0.5f * (0.5f + 0.5f * sin(e * 60f)) * (if (e > 0.9f) 0.6f else 1f)
        if (ringHit && (vr > 0f || d >= R)) o = ringV
        if (d < R) {
            val nx = u / R; val ny = v / R
            val nz = sqrt(max(0f, 1f - nx * nx - ny * ny))
            val l = max(0f, -0.45f * nx - 0.4f * ny + 0.8f * nz)
            val band = 0.55f + 0.2f * sin(ny * 11f + 0.8f * sin(nx * 4f)) + 0.1f * sin(ny * 27f)
            o = band * (0.15f + 0.85f * l)
            if (ringHit && vr > 0f) o = ringV
        }
        return o
    }
}

class MoonScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val R = 0.8f
        if (u * u + v * v > R * R) return stars(c, r, 20, 0.985f)
        val nx = u / R; val ny = v / R
        val nz = sqrt(max(0f, 1f - nx * nx - ny * ny))
        val l = max(0f, -0.6f * nx - 0.2f * ny + 0.77f * nz)
        var o = 0.55f + 0.2f * Gfx.fbm(u * 3f + 4f, v * 3f, 23, 4)
        if (Gfx.fbm(u * 1.3f + 20f, v * 1.3f, 24, 3) < 0.42f) o *= 0.62f
        val cell = 0.28f
        for (dy in -1..1) for (dx in -1..1) {
            val gx = floor(u / cell).toInt() + dx; val gy = floor(v / cell).toInt() + dy
            if (Gfx.h01(gx, gy, 25) < 0.55f) continue
            val cx = (gx + 0.2f + 0.6f * Gfx.h01(gx, gy, 26)) * cell
            val cy = (gy + 0.2f + 0.6f * Gfx.h01(gx, gy, 27)) * cell
            val rr = cell * (0.15f + 0.3f * Gfx.h01(gx, gy, 28))
            val dd = sqrt((u - cx) * (u - cx) + (v - cy) * (v - cy))
            if (abs(dd - rr) < 0.02f) o = if (u < cx) 0.95f else 0.2f
            else if (dd < rr) o *= 0.85f
        }
        return o * (0.1f + 0.9f * l)
    }
}

class MandelbrotScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val cr = -0.55f + u * 1.35f; val ci = v * 1.35f
        var zr = 0f; var zi = 0f
        var n = 0
        val maxIt = 120
        while (n < maxIt && zr * zr + zi * zi <= 16f) {
            val t = zr * zr - zi * zi + cr
            zi = 2f * zr * zi + ci; zr = t; n++
        }
        if (n >= maxIt) return 0f
        val f = n / 30f
        return 0.15f + 0.85f * (0.5f + 0.5f * sin(sqrt(n.toFloat()) * 2.2f)) * min(1f, 0.4f + f)
    }
}

class JuliaScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var zr = u * 1.45f; var zi = v * 1.45f
        val cr = -0.8f; val ci = 0.156f
        var n = 0
        val maxIt = 100
        while (n < maxIt && zr * zr + zi * zi <= 16f) {
            val t = zr * zr - zi * zi + cr
            zi = 2f * zr * zi + ci; zr = t; n++
        }
        if (n >= maxIt) return 0.05f
        return 0.2f + 0.8f * (0.5f + 0.5f * sin(sqrt(n.toFloat()) * 1.9f))
    }
}

class SierpinskiScene : StaticScene() {
    private var n = 8
    override fun paint(img: FloatArray) {
        var p = 4
        while (p * 2 <= rows && p < 128) p *= 2
        n = p
        super.paint(img)
    }

    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val a = (v + 0.92f) / 1.84f
        if (a < 0f || a >= 1f) return 0f
        val i = (a * n).toInt()
        val j = (((u / 0.92f) + a) / 2f * n).toInt()
        if (j < 0 || j > i) return 0.02f
        return if ((j and (i - j)) == 0) 0.55f + 0.45f * (1f - a) else 0.03f
    }
}

class CarpetScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val k = 81
        val x = ((u + 0.95f) / 1.9f * k).toInt(); val y = ((v + 0.95f) / 1.9f * k).toInt()
        if (u < -0.95f || u >= 0.95f || v < -0.95f || v >= 0.95f) return 0f
        var xx = x; var yy = y
        var depth = 0
        for (l in 0 until 4) {
            if (xx % 3 == 1 && yy % 3 == 1) return 0.1f + 0.22f * depth
            xx /= 3; yy /= 3; depth++
        }
        return 0.95f
    }
}

class HilbertScene : StaticScene() {
    private fun d2xy(n: Int, d0: Int, out: IntArray) {
        var t = d0; var x = 0; var y = 0; var s = 1
        while (s < n) {
            val rx = 1 and (t / 2); val ry = 1 and (t xor rx)
            if (ry == 0) { if (rx == 1) { x = s - 1 - x; y = s - 1 - y }; val tmp = x; x = y; y = tmp }
            x += s * rx; y += s * ry; t /= 4; s *= 2
        }
        out[0] = x; out[1] = y
    }

    override fun paint(img: FloatArray) {
        val order = if (sz >= 60f) 4 else if (sz >= 20f) 3 else 2
        val n = 1 shl order
        val xy = IntArray(2)
        var px = 0f; var py = 0f
        val total = n * n
        for (d in 0 until total) {
            d2xy(n, d, xy)
            val u = -0.92f + 1.84f * xy[0] / (n - 1)
            val v = -0.92f + 1.84f * xy[1] / (n - 1)
            val x = colOfU(u); val y = rowOfV(v)
            if (d > 0) seg(img, px, py, x, y, 0.35f + 0.65f * d / total)
            px = x; py = y
        }
    }
}

class TreeScene : StaticScene() {
    private var buf = FloatArray(0)
    private fun branch(x: Float, y: Float, ang: Float, len: Float, depth: Int) {
        val x2 = x + sin(ang) * len; val y2 = y - cos(ang) * len
        val b = 0.35f + 0.65f * (1f - depth / 9f)
        seg(buf, colOfU(x), rowOfV(y), colOfU(x2), rowOfV(y2), b)
        if (depth <= 0) {
            dotf(buf, colOfU(x2), rowOfV(y2), 1f)
            return
        }
        branch(x2, y2, ang - 0.42f, len * 0.74f, depth - 1)
        branch(x2, y2, ang + 0.46f, len * 0.72f, depth - 1)
    }

    override fun paint(img: FloatArray) {
        buf = img
        val d = if (sz >= 60f) 9 else if (sz >= 25f) 7 else 5
        for (i in 0..2) branch(0f + 0.002f * i, 0.95f, 0f, 0.3f, d)
        buf = FloatArray(0)
    }
}

class MosaicScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val x = u * 4f; val y = v * 4f
        val ix = floor(x).toInt(); val iy = floor(y).toInt()
        var d1 = 9f; var d2 = 9f; var id = 0
        for (dy in -1..1) for (dx in -1..1) {
            val gx = ix + dx; val gy = iy + dy
            val px = gx + Gfx.h01(gx, gy, 1) * 0.9f; val py = gy + Gfx.h01(gx, gy, 2) * 0.9f
            val d = (x - px) * (x - px) + (y - py) * (y - py)
            if (d < d1) { d2 = d1; d1 = d; id = Gfx.hash(gx, gy, 3) } else if (d < d2) d2 = d
        }
        if (sqrt(d2) - sqrt(d1) < 0.07f) return 0.04f
        return 0.25f + 0.7f * ((id and 0xFF) / 255f) - 0.15f * sqrt(d1)
    }
}

class TruchetScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val t = 0.3f
        val x = u / t; val y = v / t
        val ix = floor(x).toInt(); val iy = floor(y).toInt()
        val lx = x - ix; val ly = y - iy
        val flip = (Gfx.hash(ix, iy, 5) and 1) == 0
        val ax = if (flip) lx else 1f - lx
        val d1 = sqrt(ax * ax + ly * ly)
        val d2 = sqrt((1f - ax) * (1f - ax) + (1f - ly) * (1f - ly))
        val w = 0.07f
        val line = min(abs(d1 - 0.5f), abs(d2 - 0.5f))
        return if (line < w) 1f - line / w * 0.4f else 0.05f
    }
}

class SunflowerScene : StaticScene() {
    override fun paint(img: FloatArray) {
        val n = (sz * 18f).toInt().coerceIn(40, 1800)
        val k = 0.93f / sqrt(n.toFloat())
        for (i in 0 until n) {
            val rad = k * sqrt(i.toFloat())
            val a = i * 2.3999631f
            val x = colOfU(rad * cos(a)); val y = rowOfV(rad * sin(a))
            val b = 0.3f + 0.7f * (i.toFloat() / n)
            dotf(img, x, y, b)
            if (sz > 40f && i > n / 3) { dotf(img, x + 1f, y, b * 0.7f) }
        }
    }
}

class MandalaScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val rad = sqrt(u * u + v * v)
        if (rad > 1f) return 0f
        val n = 12
        val seg = TAUF / n
        val a = abs((atan2(v, u) + PIF) % seg - seg * 0.5f)
        val x = rad * cos(a); val y = rad * sin(a)
        val p = sin(x * 24f) * sin(y * 24f + 1f) + 0.7f * cos(rad * 30f)
        val ring = if (Gfx.fract(rad * 5f) < 0.07f) 0.5f else 0f
        return 0.1f + 0.45f * (0.5f + 0.5f * p) + ring + 0.4f * Gfx.smooth(0.95f, 0.99f, rad) 
    }
}

class RoseScene : StaticScene() {
    override fun paint(img: FloatArray) {
        val n = (sz * 90f).toInt().coerceIn(500, 9000)
        val k = 7f / 4f
        for (i in 0 until n) {
            val th = i * (TAUF * 4f) / n
            val rad = cos(k * th) * 0.9f
            val x = colOfU(rad * cos(th)); val y = rowOfV(rad * sin(th))
            val b = 0.4f + 0.6f * (0.5f + 0.5f * sin(th * 3f))
            dotf(img, x, y, b)
            dotf(img, x + 1f, y, b * 0.5f)
        }
        for (i in 0 until n) {
            val th = i * TAUF / n
            val rad = cos(5f * th) * 0.55f
            dotf(img, colOfU(rad * cos(th)), rowOfV(rad * sin(th)), 1f)
        }
    }
}

class SynthwaveScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        val hz = 0.58f
        if (fy < hz) {
            var o = 0.05f + 0.12f * fy
            o = max(o, stars(c, r, 29, 0.99f) * (1f - fy / hz))
            val vh = vOfFy(hz)
            val dv = v - (vh - 0.45f)
            val d = sqrt(u * u + dv * dv)
            if (d < 0.45f) {
                val band = Gfx.fract((dv + 0.45f) * 6f)
                val cut = (dv + 0.45f) / 0.9f
                if (!(cut > 0.5f && band < cut - 0.35f)) o = 0.5f + 0.5f * (1f - (dv + 0.45f) / 0.9f)
            } else if (d < 0.8f) o = max(o, 0.3f * (1f - (d - 0.45f) / 0.35f))
            val mt = 0.12f * Gfx.fbm(fx * 5f, 0.5f, 33, 3)
            if (fy > hz - mt) o = 0.04f
            return o
        }
        val z = 1f / (fy - hz + 0.001f) * 0.05f
        val x = u * z * 3f / (1f + 0f)
        val zl = Gfx.fract(z * 1.6f)
        var o = 0.04f
        if (zl < 0.12f) o = 0.55f
        val xl = Gfx.fract(u / (fy - hz + 0.02f) * 0.35f)
        if (xl < 0.05f || xl > 0.95f) o = max(o, 0.7f)
        if (fy - hz < 0.012f) o = 1f
        return if (x.isNaN()) 0f else o
    }
}

class DroidScene : StaticScene() {
    override fun shade(c: Int, r: Int, u: Float, v: Float, fx: Float, fy: Float): Float {
        var o = 0.02f
        val au = abs(u)
        val head = v < -0.05f && v > -0.6f && Gfx.ell(u, v, 0f, -0.05f, 0.55f, 0.55f) < 1f
        val body = au < 0.55f && v in 0.03f..0.62f && !(au > 0.45f && v > 0.5f && sqrt((au - 0.45f) * (au - 0.45f) + (v - 0.5f) * (v - 0.5f)) > 0.12f)
        val arm = au in 0.6f..0.8f && v in 0.03f..0.55f
        val leg = au in 0.15f..0.35f && v in 0.6f..0.92f
        if (head || body || arm || leg) o = 0.85f
        if (Gfx.ell(au, v, 0.27f, -0.3f, 0.07f, 0.07f) < 1f && head) o = 0.02f
        if (abs(au - (-0.78f - v) * 0.0f - (0.28f + (-0.5f - v) * -0.5f)) < 1.1f / sz && v < -0.5f && v > -0.85f) o = 0.85f
        return o
    }
}
