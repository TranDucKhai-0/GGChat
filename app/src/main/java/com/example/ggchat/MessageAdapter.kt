package com.example.ggchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messages: MutableList<Message>,
    private val myId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_OTHER = 2
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (msg.senderId == myId) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ME) {
            val v = inflater.inflate(R.layout.item_message_me, parent, false)
            MeVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_other, parent, false)
            OtherVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        when (holder) {
            is MeVH -> holder.bind(msg)
            is OtherVH -> holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = messages.size

    // (UI helper) gọi khi muốn add từ ngoài adapter thay vì add ở Fragment
    fun addMessage(msg: Message) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    class MeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(m: Message) {
            tvMessage.text = m.text
        }
    }

    class OtherVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(m: Message) {
            tvMessage.text = m.text
        }
    }
}
