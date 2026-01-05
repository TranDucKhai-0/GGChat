package com.example.ggchat

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import org.json.JSONObject


object RoomListener {
    interface OnRoomFoundListener {
        fun onRoomFound(name: String, ip: String, port: Int)
    }

    private var listener: OnRoomFoundListener? = null

    @Volatile private var running = false
    private var socket: DatagramSocket? = null
    private var thread: Thread? = null

    fun startListening(onRoomFoundListener: OnRoomFoundListener) {
        listener = onRoomFoundListener

        if (running) return
        running = true

        thread = Thread {
            try {
                socket = DatagramSocket(9999).apply {
                    reuseAddress = true
                }
                val buffer = ByteArray(1024)

                Log.d("GGChat", "Đang lắng nghe broadcast UDP trên cổng 9999...")

                while (running && socket != null && socket?.isClosed == false) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket?.receive(packet) ?: break
                    val message = String(packet.data, 0, packet.length)

                    try {
                        // TH1: JSON
                        val obj = JSONObject(message)
                        val name = obj.optString("name")
                        val ip = obj.optString("ip")
                        val port = obj.optInt("port", 0)

                        if (name.isNotEmpty() && ip.isNotEmpty() && port > 0) {
                            Log.d("GGChat", "Phát hiện phòng(JSON): $name ($ip:$port)")
                            listener?.onRoomFound(name, ip, port)
                            continue
                        }
                    } catch (_: Exception) {
                        // Ignore: not a JSON payload.
                    }

                    // Case 2: backward-compatible legacy format ROOM:name:ip:port.
                    if (message.startsWith("ROOM:")) {
                        val parts = message.split(":")
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val ip = parts[2]
                            val port = parts[3].toIntOrNull() ?: 0
                            Log.d("GGChat", "Phát hiện phòng(TEXT): $name ($ip:$port)")
                            listener?.onRoomFound(name, ip, port)
                        }
                    }

                }
            } catch (e: Exception) {
                if (running) e.printStackTrace()
            } finally {
                try { socket?.close() } catch (_: Exception) {}
                socket = null
                running = false
            }
        }.apply { isDaemon = true }

        thread?.start()
    }

    fun stopListening() {
        running = false
        listener = null
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        thread = null
    }
}
