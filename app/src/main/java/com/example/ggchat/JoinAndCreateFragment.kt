package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class JoinAndCreateFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Nạp layout cho fragment
        val view = inflater.inflate(R.layout.fragment_join_and_create, container, false)

        // Tìm object trong layout
        val textIPLAN = view.findViewById<TextView>(R.id.textIPLAN)
        // Create Room
        val createRoomButton = view.findViewById<FrameLayout>(R.id.buttonCreateRoom)
        // Join Room
        val joinRoomButton = view.findViewById<FrameLayout>(R.id.buttonJoinRoom)

        // Lấy IP trong UserData
        val currentIP = UserData.getUserIP(requireContext())
        // Đổi địa chỉ IP hiển thị
        textIPLAN.text = "IP: $currentIP"

        // tạo biến chứa Main Activity
        val mainActivity = activity as? MainActivity

        // Chuyển fragment khi bấm nút tạo phòng
        createRoomButton.setOnClickListener {
            mainActivity?.replaceFragment(CreateRoomFragment())
        }

        // Chuyển fragment khi bấm nút tham gia phòng
        joinRoomButton.setOnClickListener {
            mainActivity?.replaceFragment(JoinRoomFragment())
        }

        return view
    }
}

