package com.example.ggchat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object AvatarCodec {

    /**
     * Decode Base64 into a Bitmap using a memory-friendly configuration.
     */
    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val opts = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Encode bitmap -> base64 thumbnail.
     * - Resize to maxDim (default 96px) to keep the payload small.
     * - Compress as JPEG (default quality 60).
     */
    fun bitmapToThumbBase64(bitmap: Bitmap, maxDim: Int = 96, quality: Int = 60): String {
        val scaled = scaleDown(bitmap, maxDim)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 95), out)
        if (scaled !== bitmap) {
            try { scaled.recycle() } catch (_: Exception) {}
        }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Keep the legacy API for compatibility; it now delegates to thumbnail encoding.
     */
    fun bitmapToSmallBase64(bitmap: Bitmap): String = bitmapToThumbBase64(bitmap, maxDim = 96, quality = 60)

    private fun scaleDown(src: Bitmap, maxDim: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return src
        val m = max(w, h)
        if (m <= maxDim) return src

        val ratio = maxDim.toFloat() / m.toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, nw, nh, true)
    }
}
