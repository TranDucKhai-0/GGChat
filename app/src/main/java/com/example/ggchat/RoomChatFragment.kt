package com.example.ggchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.ggchat.network.RoomBroadcaster
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch


class RoomChatFragment : Fragment() {

    private var joinServer: HostJoinServer? = null

    private var tcpServer: ClientInfoTcpServer? = null
    private var tcpPort: Int = 0
    private var hostIp: String? = null
    private var hostTcpPort: Int = 0

    private var roomTcpClient: RoomTcpClient? = null


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
            val msg = Message(
                senderId = UserData.getUserIP(requireContext()),
                text = text,
                time = System.currentTimeMillis()
            )
            listFrag.addMessage(msg)
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

            // ✅ Host vừa là Server vừa là Client => phải tự hiện trong danh sách bạn bè
            val myName = UserData.getUserName(requireContext()).ifBlank { ip }
            val avatarB64 = UserData.getAvatar(requireContext())
                ?.let { bmp: android.graphics.Bitmap -> AvatarCodec.bitmapToSmallBase64(bmp) }

            // Để mọi người đều thấy đúng, host name sẽ kèm tag (Chủ phòng)
            FriendsStore.upsert(ip, "$myName (Chủ phòng)", avatarB64)

            // 1) Broadcast phòng lên 9999 mỗi 3s
            RoomBroadcaster.startPeriodic(
                scope = lifecycleScope,
                roomName = roomName,
                ip = ip,
                port = port,
                intervalMs = 1000
            )

            // ✅ 1.5) Bật TCP server để nhận CLIENT_INFO
            if (tcpServer == null) {
                tcpServer = ClientInfoTcpServer(
                    tcpPort = tcpPort,
                    onMembersChanged = {
                        // callback này chạy khi có người vào/thoát (server đã upsert/remove + broadcast rồi)
                        val act = activity
                        if (act != null) {
                            act.runOnUiThread {
                                if (!isAdded) return@runOnUiThread
                                // nếu muốn: toast / cập nhật badge số người
                            }
                        }
                    }
                )
                tcpServer?.start()
            }

            // 2) Bật server nhận yêu cầu join tại roomPort (UDP)
            if (joinServer == null) {
                joinServer = HostJoinServer(port) { clientIp, clientPort, clientName, reqRoomName ->
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
                    onFriendsSync = { list -> FriendsStore.replaceAll(list) }
                )

                val myIp = UserData.getUserIP(requireContext())
                val myName = UserData.getUserName(requireContext()).ifBlank { myIp }
                val avatarB64 = UserData.getAvatar(requireContext())
                    ?.let { bmp: android.graphics.Bitmap -> AvatarCodec.bitmapToSmallBase64(bmp) }

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
            // clear danh sách để lần vào phòng sau không bị dính data cũ
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
            roomTcpClient?.leaveRoom(myIp)
            roomTcpClient = null
        }
        super.onDestroyView()
    }


}
