package com.example.ggchat

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object UserData {

    // User
    private const val PREF_NAME = "UserProfile"
    private const val KEY_NAME = "user_name"
    private const val KEY_AVATAR = "avatarPath"
    private const val KEY_IP = "user_ip"
    private const val MY_ROOM_PORT = "my_room_port"
    private const val MY_ROOM_NAME = "my_room_name"

    //Host




    // Lưu tên
    fun saveUserName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NAME, name).apply()
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, "") ?: ""
    }

    // Lưu avatar thành file
    fun saveAvatar(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.filesDir, "avatar.png")
            val out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()

            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_AVATAR, file.absolutePath).apply()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Lấy Bitmap avatar
    fun getAvatar(context: Context): Bitmap? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_AVATAR, null) ?: return null
        val file = File(path)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
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
        return prefs.getInt(MY_ROOM_PORT, 0) // 0 là mặc định nếu chưa lưu
    }

    // Lưu tên phòng của user
    fun saveMyRoomName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(MY_ROOM_NAME, name).apply()
    }

    fun getMyRoomName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(MY_ROOM_NAME, "") ?: ""
    }
}
