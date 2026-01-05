package com.example.ggchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import com.example.ggchat.network.RoomBroadcaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class RoomChatFragment : Fragment() {

    private var joinServer: HostJoinServer? = null

    private var tcpServer: ClientInfoTcpServer? = null
    private var tcpPort: Int = 0
    private var hostIp: String? = null
    private var hostTcpPort: Int = 0

    private var roomTcpClient: RoomTcpClient? = null

    // Cache the local profile for the host to reuse.
    private var hostDisplayName: String? = null
    private var hostAvatarB64: String? = null


    lateinit var listFrag: MessageListFragment
    lateinit var inputFrag: InputFragment

    companion object {
        private const val ARG_ROOM_NAME = "ARG_ROOM_NAME"
        private const val ARG_IS_HOST = "ARG_IS_HOST"
        private const val ARG_HOST_IP = "ARG_HOST_IP"
        private const val ARG_HOST_PORT = "ARG_HOST_PORT"

        fun newInstance(
            roomName: String,
            isHost: Boolean = true,
            hostIp: String? = null,
            hostPort: Int = 0
        ) = RoomChatFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ROOM_NAME, roomName)
                putBoolean(ARG_IS_HOST, isHost)
                putString(ARG_HOST_IP, hostIp)
                putInt(ARG_HOST_PORT, hostPort)
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_room_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listFrag = MessageListFragment()
        inputFrag = InputFragment()

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_message_list, listFrag)
            .replace(R.id.fragment_input, inputFrag)
            .commit()

        inputFrag.setOnSendMessageListener { text ->
            val timeMs = System.currentTimeMillis()
            val myIp = UserData.getUserIP(requireContext())
            val myName = UserData.getUserName(requireContext()).ifBlank { myIp }
            val isHost = arguments?.getBoolean(ARG_IS_HOST, true) ?: true

            if (isHost) {
                val displayName = hostDisplayName ?: "$myName (Chủ phòng)"

                // Host adds the message locally (the host is also a chat participant).
                listFrag.addMessage(
                    Message(
                        senderId = myIp,
                        senderName = displayName,
                        senderAvatarBase64 = null,
                        text = text,
                        time = timeMs
                    )
                )

                // ✅ Send TCP data on an IO thread (do not block the UI).
                lifecycleScope.launch(Dispatchers.IO) {
                    tcpServer?.sendHostChat(
                        hostId = myIp,
                        hostName = displayName,
                        text = text,
                        timeMs = timeMs
                    )
                }
            } else {
                // Client adds the message locally first (the server broadcasts to others and does not echo back to the sender).
                listFrag.addMessage(
                    Message(
                        senderId = myIp,
                        senderName = myName,
                        senderAvatarBase64 = null,
                        text = text,
                        time = timeMs
                    )
                )

                // ✅ Send TCP data on an IO thread (do not re-encode avatar on every Send).
                lifecycleScope.launch(Dispatchers.IO) {
                    roomTcpClient?.sendChat(text, timeMs)
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()

        val roomName = arguments?.getString(ARG_ROOM_NAME).orEmpty().ifBlank { "Room Chat" }
        val isHost = arguments?.getBoolean(ARG_IS_HOST, true) ?: true

        (activity as? MainActivity)?.showChatBar(roomName)

        if (isHost) {
            val ip = UserData.getUserIP(requireContext())
            val port = UserData.getMyRoomPort(requireContext())
            tcpPort = port + 1   // ✅ TCP port

            // ✅ The host is both Server and Client, so it must appear in the member list as well.
            val myName = UserData.getUserName(requireContext()).ifBlank { ip }

            // ✅ Avatar: create a small thumbnail (avoid OOM and reduce payload).
            val avatarB64 = UserData.getAvatar(requireContext())
                ?.let { bmp -> AvatarCodec.bitmapToThumbBase64(bmp) }

            hostDisplayName = "$myName (Chủ phòng)"
            hostAvatarB64 = avatarB64

            // To make it clear to everyone, the host name includes a "(Host)" tag.
            FriendsStore.upsert(ip, hostDisplayName!!, avatarB64)

            // 1) Broadcast the room advertisement to UDP 9999 every 1.5 second.
            RoomBroadcaster.startPeriodic(
                scope = lifecycleScope,
                roomName = roomName,
                ip = ip,
                port = port,
                intervalMs = 1500 // Broadcast UDP 1/1.5s
            )

            // 1.5) Start the TCP server to receive CLIENT_INFO and CHAT_SEND.
            if (tcpServer == null) {
                tcpServer = ClientInfoTcpServer(
                    tcpPort = tcpPort,
                    onMembersChanged = {
                        // This callback runs when someone joins/leaves (the server has already upserted/removed and broadcasted sync).
                        val act = activity
                        if (act != null) {
                            act.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                // Optional: show a toast / update the member-count badge.
                            }
                        }
                    },
                    onChatBroadcastForHost = { msg ->
                        val act = activity
                        if (act != null) {
                            act.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                listFrag.addMessage(msg)
                            }
                        }
                    }
                )
                tcpServer?.start()
            }

            // 2) Start the server to receive join requests on roomPort (UDP).
            if (joinServer == null) {
                joinServer = HostJoinServer(port) { clientIp, clientPort, clientName, _ ->
                    showJoinDialog(clientIp, clientPort, clientName)
                }
                joinServer?.start()
            }
        } else {
            val ep = ServerEndpointStore.load(requireContext())
            hostIp = ep?.hostIp ?: (arguments?.getString(ARG_HOST_IP) ?: return)
            hostTcpPort = ep?.tcpPort ?: ((arguments?.getInt(ARG_HOST_PORT) ?: 0) + 1)

            if (roomTcpClient == null) {
                roomTcpClient = RoomTcpClient(
                    hostIp = hostIp!!,
                    tcpPort = hostTcpPort,
                    onFriendsSync = { list -> FriendsStore.replaceAll(list) },
                    onChatBroadcast = { msg ->
                        val act = activity
                        if (act != null) {
                            act.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                listFrag.addMessage(msg)
                            }
                        }
                    }
                )

                val myIp = UserData.getUserIP(requireContext())
                val myName = UserData.getUserName(requireContext()).ifBlank { myIp }

                // ✅ Avatar is encoded only once when joining (CLIENT_INFO).
                val avatarB64 = UserData.getAvatar(requireContext())
                    ?.let { bmp -> AvatarCodec.bitmapToThumbBase64(bmp) }

                roomTcpClient?.start(myIp = myIp, myName = myName, avatarBase64 = avatarB64)
            }
        }

    }

    private fun showJoinDialog(clientIp: String, clientPort: Int, clientName: String) {

        if (!isAdded) return

        requireActivity().runOnUiThread {
            AlertDialog.Builder(requireContext())
                .setTitle("Yêu cầu tham gia")
                .setMessage("$clientName yêu cầu tham gia")
                .setNegativeButton("Từ chối") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        joinServer?.replyDeny(clientIp, clientPort, "Bị từ chối")
                    }
                }
                .setPositiveButton("Đồng ý") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        joinServer?.replyAccept(
                            clientIp = clientIp,
                            clientPort = clientPort,
                            tcpPort = tcpPort,
                            message = "OK"
                        )
                    }
                }

                .setCancelable(false)
                .show()
        }
    }


    override fun onStop() {
        super.onStop()
        (activity as? MainActivity)?.showDefaultBar()
    }


    override fun onDestroy() {
        val isHost = arguments?.getBoolean(ARG_IS_HOST, true) ?: true
        if (isHost) {
            // Clear the list so the next session does not reuse stale data.
            FriendsStore.clear()
        }
        joinServer?.stop()
        joinServer = null
        RoomBroadcaster.stop()
        tcpServer?.stop()
        tcpServer = null
        super.onDestroy()
    }

    override fun onDestroyView() {
        val isHost = arguments?.getBoolean(ARG_IS_HOST, true) ?: true
        if (!isHost) {
            val myIp = UserData.getUserIP(requireContext())
            // ✅ Leave and close the socket on an IO thread.
            lifecycleScope.launch(Dispatchers.IO) {
                roomTcpClient?.leaveRoom(myIp)
                roomTcpClient = null
            }
        }
        super.onDestroyView()
    }

}
