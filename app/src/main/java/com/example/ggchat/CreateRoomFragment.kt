package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.net.ServerSocket

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
            val roomName = UserData.getMyRoomName(requireContext()) // get RoomName
            // Kiểm tra xem tên phòng đã có chưa, nếu chưa thì bắt người dùng nhập
            if (roomName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên phòng!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lấy port khả dụng ngẫu nhiên
            val roomPort = getAvailablePort()

            // Lưu vào UserData
            UserData.saveMyRoomPort(requireContext(), roomPort)

            // TODO: Gọi phát UDP broadcast (broadcast lên port 9999) để thông báo phòng LAN, ông có thể tùy chỉnh việc gọi ở file riêng cũng được

            (activity as? MainActivity)?.replaceFragment(RoomChatFragment())
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
