package com.example.ggchat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object AvatarCodec {

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun bitmapToSmallBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)

        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
