package com.example.ggchat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class RoomChatFragment : Fragment() {

    lateinit var listFrag: MessageListFragment
    lateinit var inputFrag: InputFragment

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
        (activity as? MainActivity)?.showChatBar(roomName)
    }

    override fun onStop() {
        super.onStop()
        (activity as? MainActivity)?.showDefaultBar()
    }


}
