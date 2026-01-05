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

    // Avoid setting the interval too small to prevent spamming the network/socket and causing crash/jank.
    private const val MIN_INTERVAL_MS = 500L

    /** Send once (UDP broadcast JSON). */
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
     * Start periodic broadcasting every [intervalMs].
     * Pass in a CoroutineScope (e.g., lifecycleScope) so it runs with the screen lifecycle.
     */
    fun startPeriodic(
        scope: CoroutineScope,
        roomName: String,
        ip: String,
        port: Int,
        intervalMs: Long = 5000
    ) {
        stop() // Stop any previous job if it exists.

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

    /** Stop periodic broadcasting. */
    fun stop() {
        job?.cancel()
        job = null
    }
}
