package com.example.ggchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val list: MutableList<Message>,
    private val myId: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_ME = 1
    private val TYPE_OTHER = 2

    override fun getItemViewType(position: Int): Int {
        return if (list[position].senderId == myId) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_me, parent, false)
            MeHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_other, parent, false)
            OtherHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = list[position]
        if (holder is MeHolder) holder.textView.text = message.text
        if (holder is OtherHolder) holder.textView.text = message.text
    }

    override fun getItemCount(): Int = list.size

    fun addMessage(msg: Message) {
        list.add(msg)
        notifyItemInserted(list.size - 1)
    }

    class MeHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tvMessage)
    }

    class OtherHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.tvMessage)
    }
}
