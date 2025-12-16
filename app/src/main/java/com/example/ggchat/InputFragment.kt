package com.example.ggchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.EditText


class InputFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val listFriend = view.findViewById<ImageButton>(R.id.buttonListFriend)

        val edt = view.findViewById<EditText>(R.id.edtMessage)
        val btnSend = view.findViewById<ImageButton>(R.id.buttonSendMessage)

        // hiện danh sách người có trong phòng
        listFriend.setOnClickListener {
            // Gọi sang MainActivity để thay fragment (overlay)
            (activity as? MainActivity)?.openOverlayFragment(ListFriendFragment())
        }

        // --- GỬI TIN NHẮN TRỰC TIẾP TỪ ĐÂY ---
        btnSend.setOnClickListener {
            val text = edt.text.toString().trim()
            if (text.isNotEmpty()) {
                // Gọi thẳng ChatService để gửi tin nhắn
                ChatService.sendMessage(text)
                // Xóa nội dung đã nhập
                edt.setText("")
            }
        }
    }
}
