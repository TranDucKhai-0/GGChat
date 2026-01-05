package com.example.ggchat

import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class HostJoinServer(
    private val roomPort: Int,
    private val onJoinRequest: (clientIp: String, clientPort: Int, clientName: String, roomName: String) -> Unit
) {
    private var job: Job? = null
    private var socket: DatagramSocket? = null

    fun start() {
        if (job != null) return

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                socket = DatagramSocket(roomPort)
                val buf = ByteArray(2048)

                while (isActive) {
                    val packet = DatagramPacket(buf, buf.size)
                    socket?.receive(packet) ?: break

                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val json = JSONObject(msg)

                    if (json.optString("type") == "JOIN_REQUEST") {
                        val clientPort = json.optInt("clientPort", -1)
                        val roomName = json.optString("roomName", "")
                        val clientName = json.optString("name", "Unknown")
                        val clientIp = packet.address.hostAddress ?: ""

                        if (clientPort > 0) {
                            // Notify the UI so the host can tap accept/deny.
                            onJoinRequest(clientIp, clientPort, clientName, roomName)
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore (or log, if desired).
            } finally {
                socket?.close()
                socket = null
                job = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        socket?.close()
        socket = null
    }

    suspend fun replyAccept(
        clientIp: String,
        clientPort: Int,
        tcpPort: Int,
        message: String = "OK"
    ) {
        reply(clientIp, clientPort, JSONObject().apply {
            put("type", "JOIN_ACCEPT")
            put("tcpPort", tcpPort)
            put("message", message)
        }.toString())
    }


    suspend fun replyDeny(clientIp: String, clientPort: Int, reason: String = "DENY") {
        reply(clientIp, clientPort, JSONObject().apply {
            put("type", "JOIN_DENY")
            put("reason", reason)
        }.toString())
    }

    private suspend fun reply(clientIp: String, clientPort: Int, payload: String) =
        withContext(Dispatchers.IO) {
            val s = socket ?: return@withContext
            val data = payload.toByteArray(Charsets.UTF_8)
            val addr = InetAddress.getByName(clientIp)
            s.send(DatagramPacket(data, data.size, addr, clientPort))
        }
}
