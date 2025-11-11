package com.example.ggchat

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket

object RoomListener {
    interface OnRoomFoundListener {
        fun onRoomFound(name: String, ip: String, port: Int)
    }

    private var listener: OnRoomFoundListener? = null

    fun startListening(onRoomFoundListener: OnRoomFoundListener) {
        listener = onRoomFoundListener

        Thread {
            try {
                val socket = DatagramSocket(9999) // Lắng nghe tại port 9999
                val buffer = ByteArray(1024)

                Log.d("GGChat", "Đang lắng nghe broadcast UDP trên cổng 9999...")

                while (true) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)

                    if (message.startsWith("ROOM:")) {
                        val parts = message.split(":")
                        if (parts.size >= 4) {
                            val name = parts[1]
                            val ip = parts[2]
                            val port = parts[3].toIntOrNull() ?: 0
                            Log.d("GGChat", "Phát hiện phòng: $name ($ip:$port)")
                            listener?.onRoomFound(name, ip, port)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
