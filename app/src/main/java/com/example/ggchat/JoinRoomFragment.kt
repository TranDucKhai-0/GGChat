package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class JoinRoomFragment : Fragment() {

    private lateinit var containerLayout: LinearLayout
    private val knownRooms = mutableSetOf<String>() // lưu danh sách phòng đã hiển thị


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_join_room, container, false)
        containerLayout = view.findViewById(R.id.roomListContainer)

        // Bắt đầu lắng nghe các phòng broadcast trong LAN
        RoomListener.startListening(object : RoomListener.OnRoomFoundListener {
            override fun onRoomFound(name: String, ip: String, port: Int) {
                requireActivity().runOnUiThread {
                    addRoomView(name, ip, port)
                }
            }
        })

        return view
    }


    // Hàm tạo khung phòng tự động
    private fun addRoomView(name: String, ip: String, port: Int) {
        val key = "$ip:$port"
        if (knownRooms.contains(key)) return // phòng này đã hiển thị rồi

        knownRooms.add(key)
        val roomView = layoutInflater.inflate(R.layout.item_room_frame, containerLayout, false)

        val textView = roomView.findViewById<TextView>(R.id.ipText1)
        textView.text = "$name"

        val frame = roomView.findViewById<FrameLayout>(R.id.buttonJoinRoom)
        frame.setOnClickListener {
            // Toast.makeText(requireContext(), "Đang kết nối tới $name ($ip:$port)...", Toast.LENGTH_SHORT).show()
            // mở RoomChat và truyền tên phòng để đổi ActionBar
            // (activity as? MainActivity)?.replaceFragment(RoomChatFragment.newInstance(name))

            // TODO: Gửi request join tới host (TCP/UDP)
        }

        containerLayout.addView(roomView)
    }

}
