package com.example.ggchat

import java.util.concurrent.ConcurrentHashMap

object FriendsStore {
    // key = id (host: ip, client: ip:port)
    private val map = ConcurrentHashMap<String, FriendInfo>()

    fun upsert(id: String, name: String, avatarBase64: String?) {
        map[id] = FriendInfo(
            ip = id,
            name = name,
            avatarBase64 = avatarBase64,
            lastSeenMs = System.currentTimeMillis()
        )
    }

    fun remove(id: String) {
        map.remove(id)
    }

    fun get(id: String): FriendInfo? = map[id]

    fun getAll(): List<FriendInfo> {
        return map.values.sortedByDescending { it.lastSeenMs }
    }

    fun replaceAll(list: List<FriendInfo>) {
        map.clear()
        val now = System.currentTimeMillis()
        list.forEach { f ->
            map[f.ip] = f.copy(lastSeenMs = now)
        }
    }

    fun clear() {
        map.clear()
    }
}
