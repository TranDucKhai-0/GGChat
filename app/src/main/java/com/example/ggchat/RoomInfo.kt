package com.example.ggchat

data class RoomInfo(
    val roomName: String,
    val hostIp: String,
    val port: Int,
    val lastSeenMs: Long = System.currentTimeMillis()
)
