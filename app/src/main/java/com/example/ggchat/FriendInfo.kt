package com.example.ggchat

data class FriendInfo(
    val ip: String,
    val name: String,
    val avatarBase64: String? = null,
    val lastSeenMs: Long = System.currentTimeMillis()
)
