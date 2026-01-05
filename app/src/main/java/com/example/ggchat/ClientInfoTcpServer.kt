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
 * TCP server giữ kết nối với từng client trong suốt thời gian client ở trong phòng.
 * - Client connect -> gửi CLIENT_INFO -> server upsert FriendsStore + broadcast FRIENDS_SYNC
 * - Client thoát -> gửi CLIENT_LEAVE (hoặc rớt mạng) -> server remove + broadcast lại
 */
class ClientInfoTcpServer(
    private val tcpPort: Int,
    private val onMembersChanged: () -> Unit
) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null

    // key = "remoteIp:remotePort" để tránh đè khi nhiều kết nối cùng IP
    private val conns = ConcurrentHashMap<String, Conn>()

    fun start() {
        if (job != null) return
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(tcpPort).apply { reuseAddress = true }
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
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

        var registeredIp: String? = null
        var removeIpOnClose: String? = null

        try {
            while (true) {
                val msg = TcpJson.read(conn.input) ?: break
                when (msg.optString("type")) {
                    "CLIENT_INFO" -> {
                        val ip = msg.optString("ip", remoteIp)
                        val name = msg.optString("name", "Unknown")
                        val avatar = msg.optString("avatar", null)

                        registeredIp = ip
                        FriendsStore.upsert(ip, name, avatar)

                        // ACK cho client (optional)
                        TcpJson.write(conn.output, JSONObject().apply {
                            put("type", "CLIENT_INFO_ACK")
                            put("ok", true)
                        })

                        broadcastFriends()
                        onMembersChangedSafe()
                    }

                    "CLIENT_LEAVE" -> {
                        val ip = msg.optString("ip", registeredIp ?: remoteIp)
                        removeIpOnClose = ip
                        break
                    }

                    "PING" -> {
                        TcpJson.write(conn.output, JSONObject().apply { put("type", "PONG") })
                    }
                }
            }
        } catch (_: Exception) {
            // rớt mạng / kill app
        } finally {
            conns.remove(remoteKey)
            conn.close()

            val ipToRemove = removeIpOnClose ?: registeredIp ?: remoteIp
            FriendsStore.remove(ipToRemove)

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

    private class Conn(private val socket: Socket) {
        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        fun close() { try { socket.close() } catch (_: Exception) {} }
    }
}
