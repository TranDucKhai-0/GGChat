package com.example.ggchat

import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * TCP client giữ kết nối xuyên suốt khi ở trong phòng.
 * - Vừa connect xong sẽ gửi CLIENT_INFO trên chính socket này.
 * - Khi thoát phòng sẽ gửi CLIENT_LEAVE rồi đóng socket.
 */
class RoomTcpClient(
    private val hostIp: String,
    private val tcpPort: Int,
    private val onFriendsSync: (List<FriendInfo>) -> Unit
) {
    private var socket: Socket? = null
    private var job: Job? = null
    private var pingJob: Job? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    @Volatile private var started = false

    /**
     * Start + gửi CLIENT_INFO ngay sau khi connect.
     */
    fun start(myIp: String, myName: String, avatarBase64: String?) {
        if (started) return
        started = true

        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val s = Socket().apply {
                    connect(InetSocketAddress(hostIp, tcpPort), 6000)
                    soTimeout = 0
                    keepAlive = true
                    tcpNoDelay = true
                }
                socket = s
                input = DataInputStream(BufferedInputStream(s.getInputStream()))
                output = DataOutputStream(BufferedOutputStream(s.getOutputStream()))

                // 1) gửi info join trên kết nối này
                sendClientInfo(myIp, myName, avatarBase64)

                // 2) optional keep-alive ping
                startPingLoop()

                // 3) listen messages từ host
                while (isActive) {
                    val msg = TcpJson.read(input!!) ?: break
                    when (msg.optString("type")) {
                        "FRIENDS_SYNC" -> onFriendsSync(parseFriends(msg.optJSONArray("friends")))
                        // "PONG", "CLIENT_INFO_ACK", ... => ignore
                    }
                }
            } catch (_: Exception) {
                // mất mạng / host tắt / vv
            } finally {
                stopInternal()
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(10_000)
                send(JSONObject().apply { put("type", "PING") })
            }
        }
    }

    private fun sendClientInfo(myIp: String, myName: String, avatarBase64: String?) {
        val payload = JSONObject().apply {
            put("type", "CLIENT_INFO")
            put("ip", myIp)
            put("name", myName)
            if (!avatarBase64.isNullOrBlank()) put("avatar", avatarBase64)
        }
        send(payload)
    }

    fun leaveRoom(myIp: String) {
        // gửi LEAVE trước rồi đóng socket
        try {
            send(JSONObject().apply {
                put("type", "CLIENT_LEAVE")
                put("ip", myIp)
            })
        } catch (_: Exception) {
        } finally {
            stop()
        }
    }

    fun send(json: JSONObject) {
        val out = output ?: return
        synchronized(this) {
            TcpJson.write(out, json)
        }
    }

    fun stop() {
        job?.cancel()
        pingJob?.cancel()
        job = null
        pingJob = null
        stopInternal()
    }

    private fun stopInternal() {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        input = null
        output = null
        started = false
    }

    private fun parseFriends(arr: JSONArray?): List<FriendInfo> {
        if (arr == null) return emptyList()
        val list = ArrayList<FriendInfo>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list.add(
                FriendInfo(
                    ip = o.optString("ip"),
                    name = o.optString("name"),
                    avatarBase64 = o.optString("avatar", null)
                )
            )
        }
        return list
    }
}
