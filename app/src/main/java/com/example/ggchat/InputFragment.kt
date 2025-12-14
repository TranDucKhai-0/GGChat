package com.example.ggchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.EditText


class InputFragment : Fragment() {

    private var onSendMessage: ((String) -> Unit)? = null

    fun setOnSendMessageListener(listener: (String) -> Unit) {
        onSendMessage = listener
    }

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

        // gọi về RoomChatFragment để gửi mess đi
        btnSend.setOnClickListener {
            val text = edt.text.toString().trim()
            if (text.isNotEmpty()) {
                val finalText = "$text.Gâu"
                onSendMessage?.invoke(finalText)
                edt.setText("")
            }
        }
    }
}

