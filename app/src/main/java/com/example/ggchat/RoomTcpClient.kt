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
 * TCP client keeps the connection alive for the entire time it is in the room.
 * - Right after connecting, send CLIENT_INFO on the same socket.
 * - When leaving the room, send CLIENT_LEAVE and then close the socket.
 */
class RoomTcpClient(
    private val hostIp: String,
    private val tcpPort: Int,
    private val onFriendsSync: (List<FriendInfo>) -> Unit,
    private val onChatBroadcast: (Message) -> Unit
) {
    private var socket: Socket? = null
    private var job: Job? = null
    private var pingJob: Job? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null

    @Volatile private var started = false

    /**
     * Start and send CLIENT_INFO immediately after connecting.
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

                // 1) Send join/client info on this connection.
                sendClientInfo(myIp, myName, avatarBase64)

                // 2) optional keep-alive ping
                startPingLoop()

                // 3) Listen for messages from the host.
                while (isActive) {
                    val msg = TcpJson.read(input!!) ?: break
                    when (msg.optString("type")) {
                        "FRIENDS_SYNC" -> onFriendsSync(parseFriends(msg.optJSONArray("friends")))
                        "CHAT_BROADCAST" -> onChatBroadcast(parseChatBroadcast(msg))
                        // "PONG", "CLIENT_INFO_ACK", ... => ignore
                    }
                }
            } catch (_: Exception) {
                // Lost network / host stopped / etc.
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
        // Send LEAVE first, then close the socket.
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

    fun sendChat(text: String, timeMs: Long = System.currentTimeMillis()) {
        val payload = JSONObject().apply {
            put("type", "CHAT_SEND")
            put("text", text)
            put("time", timeMs)
            // clientMsgId for optional future de-duplication / acknowledgements.
            put("clientMsgId", "${timeMs}-${(0..9999).random()}")
        }
        send(payload)
    }

    private fun parseChatBroadcast(msg: JSONObject): Message {
        val fromId = msg.optString("fromId", "")
        val fromName = msg.optString("fromName", "")
        val fromAvatar = msg.optString("fromAvatar", null)
        val text = msg.optString("text", "")
        val time = msg.optLong("time", System.currentTimeMillis())

        return Message(
            senderId = fromId,
            senderName = fromName,
            senderAvatarBase64 = fromAvatar,
            text = text,
            time = time
        )
    }
}
