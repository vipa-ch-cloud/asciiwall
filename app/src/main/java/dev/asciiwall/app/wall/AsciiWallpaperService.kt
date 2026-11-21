package dev.asciiwall.app.wall

import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class AsciiWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = AsciiEngine()

    private inner class AsciiEngine : Engine() {
        private val thread = HandlerThread("ascii-render").also { it.start() }
        private val handler = Handler(thread.looper)
        private val renderer = Renderer()
        private val lock = Any()
        private lateinit var sp: SharedPreferences
        private var holder: SurfaceHolder? = null
        private var surfaceOk = false
        private var w = 0
        private var h = 0
        private var visible = false
        private var xOffset = 0.5f
        private var lastNs = 0L
        private var nextAt = 0L

        private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ACTIVE) handler.post { applyConfig() }
        }

        private val tick = object : Runnable {
            override fun run() {
                if (!visible) return
                val start = SystemClock.uptimeMillis()
                draw()
                val interval = 1000L / renderer.fps.coerceIn(5, 60)
                nextAt = max(nextAt + interval, start + 1)
                if (nextAt < SystemClock.uptimeMillis()) nextAt = SystemClock.uptimeMillis() + 1
                handler.postAtTime(this, nextAt)
            }
        }

        private fun max(a: Long, b: Long) = if (a > b) a else b

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            sp = prefs(this@AsciiWallpaperService)
            sp.registerOnSharedPreferenceChangeListener(listener)
            setOffsetNotificationsEnabled(true)
        }

        private fun applyConfig() {
            synchronized(lock) {
                renderer.configure(
                    loadActive(sp), w, h, resources.displayMetrics.density, systemAccent(this@AsciiWallpaperService),
                )
                applyFrameRate()
            }
        }

        private fun applyFrameRate() {
            if (Build.VERSION.SDK_INT >= 30 && surfaceOk) {
                runCatching {
                    holder?.surface?.setFrameRate(renderer.fps.toFloat(), android.view.Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
            }
        }

        override fun onSurfaceCreated(h: SurfaceHolder) {
            synchronized(lock) { holder = h; surfaceOk = true }
        }

        override fun onSurfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
            synchronized(lock) { holder = h; surfaceOk = true; w = width; this.h = height }
            handler.post { applyConfig(); lastNs = 0L; draw() }
        }

        override fun onSurfaceDestroyed(h: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(tick)
            synchronized(lock) { surfaceOk = false; holder = null }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            handler.removeCallbacks(tick)
            if (v) {
                handler.post { applyConfig(); lastNs = 0L; nextAt = SystemClock.uptimeMillis(); tick.run() }
            }
        }

        override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xStep: Float, yStep: Float, xPixels: Int, yPixels: Int) {
            this.xOffset = if (xStep <= 0f || xStep >= 1f) 0.5f else xOffset
        }

        private fun draw() {
            synchronized(lock) {
                val hd = holder ?: return
                if (!surfaceOk || w <= 0) return
                val now = System.nanoTime()
                val dt = if (lastNs == 0L) 1f / 30f else ((now - lastNs) / 1e9f).coerceAtMost(0.1f)
                lastNs = now
                val c = try {
                    if (Build.VERSION.SDK_INT >= 26) hd.lockHardwareCanvas() else hd.lockCanvas()
                } catch (e: Exception) {
                    try { hd.lockCanvas() } catch (e2: Exception) { null }
                } ?: return
                try {
                    renderer.frame(c, dt, xOffset)
                } finally {
                    try { hd.unlockCanvasAndPost(c) } catch (_: Exception) {}
                }
            }
        }

        override fun onDestroy() {
            visible = false
            sp.unregisterOnSharedPreferenceChangeListener(listener)
            handler.removeCallbacksAndMessages(null)
            thread.quitSafely()
            super.onDestroy()
        }
    }
}
