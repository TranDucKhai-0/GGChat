package com.example.ggchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class RoomChatFragment : Fragment() {

    lateinit var listFrag: MessageListFragment
    lateinit var inputFrag: InputFragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        listFrag = MessageListFragment()
        inputFrag = InputFragment()

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_message_list, listFrag)
            .replace(R.id.fragment_input, inputFrag)
            .commit()

        // ⚡ Nhận tin nhắn từ InputFragment
        inputFrag.setOnSendMessageListener { text ->
            val msg = Message(
                senderId = UserData.getUserIP(requireContext()),
                text = text,
                time = System.currentTimeMillis()
            )

            // ⚡ Thêm tin nhắn vào danh sách
            listFrag.addMessage(msg)
        }
    }


}