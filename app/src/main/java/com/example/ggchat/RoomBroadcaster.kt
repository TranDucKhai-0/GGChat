package com.example.ggchat.network

import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object RoomBroadcaster {

    private const val DISCOVERY_PORT = 9999
    private const val BROADCAST_IP = "255.255.255.255"

    private var job: Job? = null

    // Tránh set interval quá nhỏ làm spam socket/network và dễ crash/jank
    private const val MIN_INTERVAL_MS = 500L

    /** Gửi 1 lần (UDP broadcast JSON) */
    fun broadcastOnce(roomName: String, ip: String, port: Int) {
        try {
            val json = JSONObject().apply {
                put("name", roomName)
                put("ip", ip)
                put("port", port)
            }
            val data = json.toString().toByteArray(Charsets.UTF_8)

            DatagramSocket().use { socket ->
                socket.broadcast = true
                val addr = InetAddress.getByName(BROADCAST_IP)
                val packet = DatagramPacket(data, data.size, addr, DISCOVERY_PORT)
                socket.send(packet)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Start broadcast định kỳ mỗi [intervalMs].
     * truyền vào 1 CoroutineScope (ví dụ lifecycleScope) để nó chạy đúng vòng đời màn hình.
     */
    fun startPeriodic(
        scope: CoroutineScope,
        roomName: String,
        ip: String,
        port: Int,
        intervalMs: Long = 5000
    ) {
        stop() // stop job cũ nếu có

        val safeInterval = if (intervalMs < MIN_INTERVAL_MS) MIN_INTERVAL_MS else intervalMs

        job = scope.launch(Dispatchers.IO) {
            var socket: DatagramSocket? = null
            try {
                val json = JSONObject().apply {
                    put("name", roomName)
                    put("ip", ip)
                    put("port", port)
                }
                val addr = InetAddress.getByName(BROADCAST_IP)

                socket = DatagramSocket().apply { broadcast = true }

                while (isActive) {
                    val data = json.toString().toByteArray(Charsets.UTF_8)
                    val packet = DatagramPacket(data, data.size, addr, DISCOVERY_PORT)
                    socket.send(packet)
                    delay(safeInterval)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket?.close() } catch (_: Exception) {}
            }
        }
    }

    /** Stop broadcast định kỳ */
    fun stop() {
        job?.cancel()
        job = null
    }
}
