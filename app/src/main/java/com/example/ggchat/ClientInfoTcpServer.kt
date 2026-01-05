package com.example.ggchat

import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * TCP server that keeps a persistent connection to each client while they are in the room.
 * - On connect: client sends CLIENT_INFO; server upserts FriendsStore and broadcasts FRIENDS_SYNC.
 * - On leave (or disconnect): client sends CLIENT_LEAVE; server removes the member and re-broadcasts sync.
 */
class ClientInfoTcpServer(
    private val tcpPort: Int,
    private val onMembersChanged: () -> Unit,
    private val onChatBroadcastForHost: (Message) -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null

    /**
     * key = "remoteIp:remotePort" (connId) to avoid collisions when multiple connections share the same IP.
     * connId is also used as the sender id (based on the TCP connection) when broadcasting chat.
     */
    private val conns = ConcurrentHashMap<String, Conn>()

    fun start() {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(tcpPort).apply { reuseAddress = true }
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    try {
                        socket.tcpNoDelay = true
                        socket.keepAlive = true
                    } catch (_: Exception) {
                    }
                    launch { handle(socket) }
                }
            } catch (_: Exception) {
            } finally {
                try { serverSocket?.close() } catch (_: Exception) {}
                serverSocket = null
            }
        }
    }

    private suspend fun handle(socket: Socket) = withContext(Dispatchers.IO) {
        val remoteIp = socket.inetAddress?.hostAddress ?: "unknown"
        val remoteKey = "$remoteIp:${socket.port}"
        val conn = Conn(socket)

        conns[remoteKey] = conn

        var registeredName: String? = null
        var registeredAvatar: String? = null
        var removeIdOnClose: String? = null

        try {
            while (true) {
                val msg = TcpJson.read(conn.input) ?: break
                when (msg.optString("type")) {
                    "CLIENT_INFO" -> {
                        // Identify clients by the TCP connection; do not trust any self-reported IP.
                        // Use connId (remoteKey) as the FriendsStore id to prevent duplicates.
                        val name = msg.optString("name", "Unknown")
                        val avatar = msg.optString("avatar", null)

                        registeredName = name
                        registeredAvatar = avatar
                        conn.displayName = name
                        conn.avatarBase64 = avatar

                        FriendsStore.upsert(remoteKey, name, avatar)

                        // ACK cho client (optional)
                        TcpJson.write(conn.output, JSONObject().apply {
                            put("type", "CLIENT_INFO_ACK")
                            put("ok", true)
                        })

                        broadcastFriends()
                        onMembersChangedSafe()
                    }

                    "CLIENT_LEAVE" -> {
                        // On leave: remove the member by connId (TCP connection).
                        removeIdOnClose = remoteKey
                        break
                    }

                    "PING" -> {
                        TcpJson.write(conn.output, JSONObject().apply { put("type", "PONG") })
                    }

                    "CHAT_SEND" -> {
                        val text = msg.optString("text", "")
                        val time = msg.optLong("time", System.currentTimeMillis())

                        // The sender is determined by the connection (remoteKey).
                        val fromName = conn.displayName ?: (registeredName ?: "Unknown")

                        // ✅ Important: do NOT include avatar data in every chat message anymore.
                        // Avatars are synced via FRIENDS_SYNC; the UI resolves them from FriendsStore.

                        // The host UI should also display client messages.
                        try {
                            onChatBroadcastForHost(
                                Message(
                                    senderId = remoteKey,
                                    senderName = fromName,
                                    senderAvatarBase64 = null,
                                    text = text,
                                    time = time
                                )
                            )
                        } catch (_: Exception) {
                        }

                        broadcastChatFromConn(
                            fromId = remoteKey,
                            fromName = fromName,
                            text = text,
                            timeMs = time,
                            excludeConnId = remoteKey
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Network drop / app killed.
        } finally {
            conns.remove(remoteKey)
            conn.close()

            val idToRemove = removeIdOnClose ?: remoteKey
            FriendsStore.remove(idToRemove)

            broadcastFriends()
            onMembersChangedSafe()
        }
    }

    private fun onMembersChangedSafe() {
        try { onMembersChanged() } catch (_: Exception) {}
    }

    private fun broadcastFriends() {
        val arr = JSONArray()
        FriendsStore.getAll().forEach { f ->
            arr.put(JSONObject().apply {
                put("ip", f.ip)
                put("name", f.name)
                if (!f.avatarBase64.isNullOrBlank()) put("avatar", f.avatarBase64)
            })
        }

        val msg = JSONObject().apply {
            put("type", "FRIENDS_SYNC")
            put("friends", arr)
        }

        conns.values.forEach { c ->
            try { TcpJson.write(c.output, msg) } catch (_: Exception) {}
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        conns.values.forEach { try { it.close() } catch (_: Exception) {} }
        conns.clear()
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    /**
     * Host sends messages similarly to a client.
     * - The host adds the message locally in RoomChatFragment.
     * - This layer only broadcasts the message down to clients.
     */
    fun sendHostChat(hostId: String, hostName: String, text: String, timeMs: Long) {
        broadcastChatFromConn(
            fromId = hostId,
            fromName = hostName,
            text = text,
            timeMs = timeMs,
            excludeConnId = null
        )
    }

    private fun broadcastChatFromConn(
        fromId: String,
        fromName: String,
        text: String,
        timeMs: Long,
        excludeConnId: String?
    ) {
        val payload = JSONObject().apply {
            put("type", "CHAT_BROADCAST")
            put("fromId", fromId)
            put("fromName", fromName)
            put("text", text)
            put("time", timeMs)
        }

        conns.forEach { (connId, c) ->
            if (excludeConnId != null && connId == excludeConnId) return@forEach
            try { TcpJson.write(c.output, payload) } catch (_: Exception) {}
        }
    }

    private class Conn(private val socket: Socket) {
        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        @Volatile var displayName: String? = null
        @Volatile var avatarBase64: String? = null
        fun close() { try { socket.close() } catch (_: Exception) {} }
    }
}
