package app.nocturn

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("n", MODE_PRIVATE)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.BLACK); setPadding(pad, pad * 2, pad, pad)
        }
        fun label(s: String, size: Float) = TextView(this).apply { text = s; textSize = size; setTextColor(Color.parseColor("#e8ecf7")); setPadding(0, pad / 2, 0, pad / 2) }
        root.addView(label("NOCTURN", 28f).apply { letterSpacing = 0.3f; gravity = Gravity.CENTER_HORIZONTAL })
        root.addView(label("Pick a style and color, then set it as your wallpaper.", 14f))
        listOf("Stars", "Aurora", "Embers", "Tides").forEachIndexed { i, n ->
            root.addView(Button(this).apply { text = n; setOnClickListener { prefs.edit().putInt("style", i).apply(); Toast.makeText(context, "$n selected", Toast.LENGTH_SHORT).show() } })
        }
        root.addView(Button(this).apply {
            text = "Use my own photo"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type = "image/*"; addCategory(Intent.CATEGORY_OPENABLE) }, 1)
            }
        })
        root.addView(label("Color", 14f))
        root.addView(SeekBar(this).apply {
            max = 360; progress = prefs.getInt("hue", 228)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, v: Int, u: Boolean) { prefs.edit().putInt("hue", v).apply() }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
        root.addView(Button(this).apply {
            text = "Set as wallpaper"
            setOnClickListener {
                startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, NocturnService::class.java)))
            }
        })
        setContentView(ScrollView(this).apply { setBackgroundColor(Color.BLACK); addView(root) })
    }

    override fun onActivityResult(rq: Int, rs: Int, d: Intent?) {
        super.onActivityResult(rq, rs, d)
        if (rq != 1 || rs != RESULT_OK) return
        val uri = d?.data ?: return
        try {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
            var k = 1
            while (o.outWidth / k > 2400 || o.outHeight / k > 2400) k *= 2
            val bmp = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = k })
            } ?: return
            File(filesDir, "custom.jpg").outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            getSharedPreferences("n", MODE_PRIVATE).edit().putInt("style", 4).apply()
            Toast.makeText(this, "Photo ready. Tap Set as wallpaper.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open that image", Toast.LENGTH_SHORT).show()
        }
    }
}
