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

    private lateinit var lm: LinearLayoutManager
    private var pendingNewCount = 0 // nếu muốn badge tin mới


    private lateinit var myId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_message_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        myId = UserData.getUserIP(requireContext())
        rv = view.findViewById(R.id.rvMessages)

        adapter = MessageAdapter(messages, myId)
        rv.adapter = adapter

        lm = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rv.layoutManager = lm

        rv.itemAnimator = null // đỡ giật khi chat nhanh
        rv.setHasFixedSize(true)
    }



    // Add tin nhắn vào đoạn chat
    fun addMessage(msg: Message) {
        //  check user có đang ở gần đáy không (trước khi insert)
        val shouldAutoScroll = isNearBottom()

        //  add + notify
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)

        //  quyết định scroll
        if (shouldAutoScroll) {
            rv.scrollToPosition(messages.size - 1)
            pendingNewCount = 0
        } else {
            pendingNewCount += 1
        }
    }

    private fun isNearBottom(threshold: Int = 2): Boolean {
        if (!::lm.isInitialized || adapter.itemCount == 0) return true
        val lastVisible = lm.findLastVisibleItemPosition()
        val lastIndex = adapter.itemCount - 1
        return lastVisible >= (lastIndex - threshold)
    }

}

