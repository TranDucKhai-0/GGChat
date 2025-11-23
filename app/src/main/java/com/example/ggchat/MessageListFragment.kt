package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MessageListFragment : Fragment() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    private val myId = UserData.getUserIP(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_message_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvMessages)
        adapter = MessageAdapter(messages, myId)

        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())
    }

    // Hàm RoomChatFragment gọi để thêm tin nhắn
    fun addMessage(msg: Message) {
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)
        rv.scrollToPosition(messages.size - 1)
    }
}
