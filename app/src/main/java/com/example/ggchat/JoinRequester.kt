package com.example.ggchat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

sealed class JoinResult {
    data class Accepted(val tcpPort: Int, val message: String? = null) : JoinResult()
    data class Denied(val reason: String? = null) : JoinResult()
    object Timeout : JoinResult()
    data class Error(val message: String) : JoinResult()
}

object JoinRequester {

    suspend fun requestJoin(
        hostIp: String,
        hostPort: Int,
        roomName: String,
        clientName: String,
        timeoutMs: Int = 5000
    ): JoinResult = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            // Open a socket on any available port so we can send and receive the reply.
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val payload = JSONObject().apply {
                put("type", "JOIN_REQUEST")
                put("roomName", roomName)
                put("clientPort", socket.localPort)
                put("name", clientName)
            }.toString()

            val data = payload.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                data,
                data.size,
                InetAddress.getByName(hostIp),
                hostPort
            )
            socket.send(packet)

            // Wait for the response.
            val buf = ByteArray(2048)
            val respPacket = DatagramPacket(buf, buf.size)
            socket.receive(respPacket)

            val respStr = String(respPacket.data, 0, respPacket.length, Charsets.UTF_8)
            val json = JSONObject(respStr)
            when (json.optString("type")) {
                "JOIN_ACCEPT" -> {
                    val tcpPort = json.optInt("tcpPort", -1)
                    if (tcpPort <= 0) JoinResult.Error("JOIN_ACCEPT missing tcpPort")
                    else JoinResult.Accepted(tcpPort = tcpPort, message = json.optString("message"))
                }
                "JOIN_DENY" -> JoinResult.Denied(json.optString("reason"))
                else -> JoinResult.Error("Unknown response: ${json.optString("type")}")
            }
        } catch (e: java.net.SocketTimeoutException) {
            JoinResult.Timeout
        } catch (e: Exception) {
            JoinResult.Error(e.message ?: "unknown error")
        } finally {
            socket?.close()
        }
    }
}
