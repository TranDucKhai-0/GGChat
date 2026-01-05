package com.example.ggchat

import java.util.concurrent.ConcurrentHashMap

object FriendsStore {
    // key = ip (tạm dùng ip)
    private val map = ConcurrentHashMap<String, FriendInfo>()

    fun upsert(ip: String, name: String, avatarBase64: String?) {
        map[ip] = FriendInfo(
            ip = ip,
            name = name,
            avatarBase64 = avatarBase64,
            lastSeenMs = System.currentTimeMillis()
        )
    }

    fun remove(ip: String) {
        map.remove(ip)
    }

    fun getAll(): List<FriendInfo> {
        return map.values.sortedByDescending { it.lastSeenMs }
    }

    fun replaceAll(list: List<FriendInfo>) {
        map.clear()
        list.forEach { f ->
            map[f.ip] = f.copy(lastSeenMs = System.currentTimeMillis())
        }
    }

    fun clear() {
        map.clear()
    }

}
