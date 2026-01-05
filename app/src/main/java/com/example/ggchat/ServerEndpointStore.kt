package com.example.ggchat

import android.content.Context

object ServerEndpointStore {
    private const val PREF = "server_endpoint"

    fun save(context: Context, hostIp: String, roomPort: Int, tcpPort: Int, roomName: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString("hostIp", hostIp)
            .putInt("roomPort", roomPort)
            .putInt("tcpPort", tcpPort)
            .putString("roomName", roomName)
            .apply()
    }

    fun load(context: Context): Endpoint? {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val hostIp = sp.getString("hostIp", null) ?: return null
        val roomPort = sp.getInt("roomPort", -1)
        val tcpPort = sp.getInt("tcpPort", -1)
        val roomName = sp.getString("roomName", "") ?: ""
        if (roomPort <= 0 || tcpPort <= 0) return null
        return Endpoint(hostIp, roomPort, tcpPort, roomName)
    }

    data class Endpoint(val hostIp: String, val roomPort: Int, val tcpPort: Int, val roomName: String)
}
