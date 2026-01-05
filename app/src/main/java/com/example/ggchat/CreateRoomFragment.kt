package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.net.ServerSocket
import com.example.ggchat.network.RoomBroadcaster


class CreateRoomFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_room, container, false)

        val editName = view.findViewById<EditText>(R.id.editRoomName)

        // Hiện lại tên đã lưu trước đó
        val oldName = UserData.getMyRoomName(requireContext())
        if (oldName.isNotEmpty()) {
            editName.setText(oldName)
        }

        // Khi bàn phím bị tắt -> tự động lưu vào userdata
        editName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // mất focus = người dùng đã gõ xong hoặc tắt bàn phím
                val name = editName.text.toString().trim()
                if (name.isNotEmpty()) {
                    UserData.saveMyRoomName(requireContext(), name) // Save room name
                }
            }
        }

        // Button next
        val nextButton = view.findViewById<ImageButton>(R.id.nextToRoomChat)
        nextButton.setOnClickListener {
            // lấy trực tiếp từ editName
            val roomName = editName.text.toString().trim()

            if (roomName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên phòng!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // lưu tên phòng vào UserData
            UserData.saveMyRoomName(requireContext(), roomName)

            // lấy PORT chưa bị chiếm
            val roomPort = getAvailablePort()
            UserData.saveMyRoomPort(requireContext(), roomPort)

            // Lấy IP từ Userdata
            val ip = UserData.getUserIP(requireContext()).ifEmpty {
                (activity as? MainActivity)?.getLocalIpAddress() ?: "0.0.0.0"
            }

            // Broadcast JSON lên UDP 9999
            RoomBroadcaster.broadcastOnce(roomName, ip, roomPort)
            // vào room chat
            (activity as? MainActivity)?.replaceFragment(
                RoomChatFragment.newInstance(
                    roomName = roomName,
                    isHost = true,
                    hostIp = ip,
                    hostPort = roomPort
                )
            )
        }
        return view
    }

    // Lấy cổng TCP ngẫu nhiên chưa bị chiếm
    private fun getAvailablePort(): Int {
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

}
