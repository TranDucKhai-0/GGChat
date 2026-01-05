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
    private var pendingNewCount = 0 // If you want a "new message" badge.


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

        rv.itemAnimator = null // Reduce jank when chatting quickly.
        rv.setHasFixedSize(true)
    }



    // Add the message to the chat list.
    fun addMessage(msg: Message) {
        //  Check whether the user is near the bottom (before inserting).
        val shouldAutoScroll = isNearBottom()

        //  add + notify
        messages.add(msg)
        adapter.notifyItemInserted(messages.size - 1)

        //  Decide whether to auto-scroll.
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

