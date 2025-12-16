package com.example.ggchat

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class RoomChatFragment : Fragment() {

    private lateinit var listFrag: MessageListFragment
    private lateinit var inputFrag: InputFragment

    companion object {
        private const val ARG_ROOM_NAME = "ARG_ROOM_NAME"
        fun newInstance(roomName: String) = RoomChatFragment().apply {
            arguments = Bundle().apply { putString(ARG_ROOM_NAME, roomName) }
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

        // Dòng code cũ "inputFrag.setOnSendMessageListener" gây lỗi đã được xóa.

        // LOGIC MỚI: Lắng nghe tin nhắn trực tiếp từ ChatService để cập nhật giao diện
        ChatService.listenForMessages { chatMessage ->
            Log.d("RoomChatFragment", "New message received: ${chatMessage.content}")

            // Chuyển đổi ChatMessage (từ Firebase) thành Message (cho UI)
            // Dựa trên cấu trúc file Message.kt của bạn
            val uiMessage = Message(
                senderId = chatMessage.userId,
                text = chatMessage.content,
                timestamp = chatMessage.timestamp // Sửa từ 'time' thành 'timestamp'
            )

            // Cập nhật giao diện trên Main Thread để đảm bảo an toàn
            activity?.runOnUiThread {
                listFrag.addMessage(uiMessage)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val roomName = arguments?.getString(ARG_ROOM_NAME).orEmpty().ifBlank { "Room Chat" }
        (activity as? MainActivity)?.showChatBar(roomName)
    }

    override fun onStop() {
        super.onStop()
        (activity as? MainActivity)?.showDefaultBar()
    }


}
