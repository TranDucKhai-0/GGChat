package com.example.ggchat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max

object UserData {

    // User
    private const val PREF_NAME = "UserProfile"
    private const val KEY_NAME = "user_name"
    private const val KEY_AVATAR = "avatarPath"
    private const val KEY_IP = "user_ip"
    private const val MY_ROOM_PORT = "my_room_port"
    private const val MY_ROOM_NAME = "my_room_name"

    // Save the display name.
    fun saveUserName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    /**
     * Save the avatar as a small file (reduces crash/OOM risk and reduces Base64 payload size).
     */
    fun saveAvatar(context: Context, bitmap: Bitmap) {
        try {
            val scaled = scaleDown(bitmap, 512)
            val file = File(context.filesDir, "avatar.jpg")
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled !== bitmap) {
                try { scaled.recycle() } catch (_: Exception) {}
            }

            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_AVATAR, file.absolutePath).apply()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Load the avatar Bitmap using a "lightweight" decode (scaled to maxDim).
     * Default 512px is usually sufficient quality for avatar UI.
     */
    fun getAvatar(context: Context): Bitmap? = getAvatarScaled(context, 512)

    fun getAvatarScaled(context: Context, maxDim: Int): Bitmap? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_AVATAR, null) ?: return null
        val file = File(path)
        if (!file.exists()) return null
        return decodeScaledFromFile(file.absolutePath, maxDim)
    }

    private fun decodeScaledFromFile(path: String, maxDim: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)

            var inSampleSize = 1
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w > 0 && h > 0) {
                val m = max(w, h)
                while (m / inSampleSize > maxDim) {
                    inSampleSize *= 2
                }
            }

            val opts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(path, opts)
        } catch (_: Exception) {
            null
        }
    }

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

    // ====== IP LAN USER ======
    fun saveUserIP(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_IP, ip).apply()
    }

    fun getUserIP(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_IP, "") ?: ""
    }

    // ====== PORT Room User =====
    fun saveMyRoomPort(context: Context, port: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(MY_ROOM_PORT, port).apply()
    }

    fun getMyRoomPort(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(MY_ROOM_PORT, 0) // 0 is the default value if nothing has been saved yet.
    }

    // Save the user's room name.
    fun saveMyRoomName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(MY_ROOM_NAME, name).apply()
    }

    fun getMyRoomName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(MY_ROOM_NAME, "") ?: ""
    }
}
