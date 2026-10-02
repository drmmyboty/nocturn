package app.nocturn

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class NocturnService : WallpaperService() {
    override fun onCreateEngine(): Engine = E()

    inner class E : Engine() {
        private val h = Handler(Looper.getMainLooper())
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val prefs = getSharedPreferences("n", MODE_PRIVATE)
        private var custom: Bitmap? = null; private var stamp = 0L
        private var w = 1f; private var ht = 1f; private var t = 0f; private var vis = false
        private val sx = FloatArray(140) { Random.nextFloat() }
        private val sy = FloatArray(140) { Random.nextFloat() }
        private val sp = FloatArray(140) { Random.nextFloat() * 6f }
        private val ex = FloatArray(60) { Random.nextFloat() }
        private val ey = FloatArray(60) { Random.nextFloat() }
        private val ev = FloatArray(60) { 0.0004f + Random.nextFloat() * 0.0009f }
        private val run = Runnable { frame() }

        private fun col(hue: Float, l: Float, a: Float) =
            Color.HSVToColor((a.coerceIn(0f, 1f) * 255).toInt(), floatArrayOf(hue % 360f, 0.55f, l / 100f))

        override fun onVisibilityChanged(v: Boolean) { vis = v; h.removeCallbacks(run); if (v) h.post(run) }
        override fun onSurfaceChanged(holder: SurfaceHolder, f: Int, width: Int, height: Int) { w = width.toFloat(); ht = height.toFloat() }
        override fun onSurfaceDestroyed(holder: SurfaceHolder) { vis = false; h.removeCallbacks(run) }

        private fun frame() {
            val c = surfaceHolder.lockCanvas()
            if (c != null) try { render(c) } finally { surfaceHolder.unlockCanvasAndPost(c) }
            t += 0.033f
            if (vis) h.postDelayed(run, if (prefs.getInt("style", 0) == 4) 1000L else 33L)
        }

        private fun render(c: Canvas) {
            val style = prefs.getInt("style", 0); val hue = prefs.getInt("hue", 228).toFloat()
            val sc = w / 540f
            c.drawColor(Color.BLACK)
            p.shader = null; p.style = Paint.Style.FILL; p.clearShadowLayer()
            when (style) {
                0 -> {
                    for (i in sx.indices) {
                        p.color = col(hue, 95f, 0.35f + 0.65f * abs(sin(t * 0.8f + sp[i])))
                        c.drawCircle(sx[i] * w, sy[i] * ht, (0.6f + sp[i] % 1.4f) * sc, p)
                        sy[i] += 0.00012f; if (sy[i] > 1f) sy[i] = 0f
                    }
                    p.shader = RadialGradient(w * 0.7f, ht * 0.18f, w * 0.25f,
                        intArrayOf(col(hue, 100f, 1f), col(hue, 80f, 0.35f), Color.TRANSPARENT), floatArrayOf(0f, 0.3f, 1f), Shader.TileMode.CLAMP)
                    c.drawRect(0f, 0f, w, ht, p)
                }
                1 -> for (i in 0..3) {
                    val path = Path(); path.moveTo(0f, ht)
                    var px = 0f
                    while (px <= w) { path.lineTo(px, ht * 0.45f + sin(px / sc * 0.012f + t * 0.6f + i) * 70f * sc + i * 45f * sc); px += 18f * sc }
                    path.lineTo(w, ht); path.close()
                    p.shader = LinearGradient(0f, ht * 0.3f, 0f, ht, col(hue + i * 28, 90f, 0.28f), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    c.drawPath(path, p)
                }
                2 -> for (i in ex.indices) {
                    ey[i] -= ev[i]; if (ey[i] < -0.01f) { ey[i] = 1.01f; ex[i] = Random.nextFloat() }
                    val r = (1f + ev[i] * 3000f) * sc
                    p.color = col(hue, 90f, 0.2f + r / (6f * sc))
                    p.setShadowLayer(14f * sc, 0f, 0f, col(hue, 85f, 1f))
                    c.drawCircle((ex[i] + sin(t + ey[i] * 10f) * 0.02f) * w, ey[i] * ht, r, p)
                }
                4 -> {
                    val f = File(filesDir, "custom.jpg")
                    if (f.exists() && f.lastModified() != stamp) { custom = BitmapFactory.decodeFile(f.path); stamp = f.lastModified() }
                    custom?.let { b ->
                        val k = maxOf(w / b.width, ht / b.height)
                        val m = Matrix(); m.postScale(k, k); m.postTranslate((w - b.width * k) / 2f, (ht - b.height * k) / 2f)
                        c.drawBitmap(b, m, p)
                    }
                }
                else -> {
                    p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f * sc
                    for (i in 0 until 26) {
                        val y = ht * 0.2f + i * 26f * sc * (ht / w) / 2.17f
                        val path = Path(); var px = 0f
                        while (px <= w) { val yy = y + sin(px / sc * 0.02f + t * 0.7f + i * 0.4f) * (6f + i * 0.5f) * sc; if (px == 0f) path.moveTo(px, yy) else path.lineTo(px, yy); px += 10f * sc }
                        p.color = col(hue, 55f + i * 1.6f, 0.1f + i / 40f)
                        c.drawPath(path, p)
                    }
                }
            }
        }
    }
}
