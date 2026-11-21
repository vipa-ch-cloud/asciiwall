package dev.asciiwall.app.core

import kotlin.math.floor

internal const val PIF = 3.1415927f
internal const val TAUF = 6.2831855f

/** Мелкие числовые помощники для сцен. Без аллокаций. */
object Gfx {
    fun hash(a: Int, b: Int, c: Int = 0): Int {
        var h = a * 374761393 + b * 668265263 + c * 1274126177
        h = (h xor (h ushr 13)) * 1274126177
        return h xor (h ushr 16)
    }

    /** Псевдослучайное 0..1 по целочисленным координатам. */
    fun h01(a: Int, b: Int, c: Int = 0): Float = (hash(a, b, c) and 0xFFFF) / 65535f

    fun vnoise(x: Float, y: Float, seed: Int = 0): Float {
        val xi = floor(x).toInt(); val yi = floor(y).toInt()
        val fx = x - xi; val fy = y - yi
        val sx = fx * fx * (3f - 2f * fx); val sy = fy * fy * (3f - 2f * fy)
        val a = h01(xi, yi, seed); val b = h01(xi + 1, yi, seed)
        val c = h01(xi, yi + 1, seed); val d = h01(xi + 1, yi + 1, seed)
        return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
    }

    fun fbm(x: Float, y: Float, seed: Int = 0, oct: Int = 4): Float {
        var s = 0f; var a = 0.5f; var f = 1f; var n = 0f
        for (i in 0 until oct) { s += a * vnoise(x * f, y * f, seed + i); n += a; a *= 0.5f; f *= 2f }
        return s / n
    }

    fun clamp01(x: Float) = if (x < 0f) 0f else if (x > 1f) 1f else x

    fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = clamp01((x - e0) / (e1 - e0))
        return t * t * (3f - 2f * t)
    }

    fun fract(x: Float) = x - floor(x)

    /** Нормированный квадрат расстояния до центра эллипса (<1 — внутри). */
    fun ell(x: Float, y: Float, cx: Float, cy: Float, rx: Float, ry: Float): Float {
        val a = (x - cx) / rx; val b = (y - cy) / ry
        return a * a + b * b
    }

    fun tri(x: Float, y: Float, ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Boolean {
        val d1 = (x - bx) * (ay - by) - (ax - bx) * (y - by)
        val d2 = (x - cx) * (by - cy) - (bx - cx) * (y - cy)
        val d3 = (x - ax) * (cy - ay) - (cx - ax) * (y - ay)
        val neg = d1 < 0f || d2 < 0f || d3 < 0f
        val pos = d1 > 0f || d2 > 0f || d3 > 0f
        return !(neg && pos)
    }
}
