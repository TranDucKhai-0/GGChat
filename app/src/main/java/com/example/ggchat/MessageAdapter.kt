package com.example.ggchat

import android.graphics.Bitmap
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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

    fun addMessage(msg: Message) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    // Cache bitmaps by Base64 to avoid decoding repeatedly.
    private val avatarCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt().coerceAtLeast(1024)
    ) {}

    private fun getAvatarCached(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        val hit = avatarCache.get(base64)
        if (hit != null) return hit
        val bmp = AvatarCodec.decodeBase64ToBitmap(base64)
        if (bmp != null) avatarCache.put(base64, bmp)
        return bmp
    }

    class MeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        fun bind(m: Message) {
            tvMessage.text = m.text
        }
    }

    inner class OtherVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvSenderName: TextView? = itemView.findViewById(R.id.tvSenderName)
        private val imgAvatar: ImageView? = itemView.findViewById(R.id.imgAvatar)

        fun bind(m: Message) {
            tvMessage.text = m.text
            tvSenderName?.text = m.senderName

            // Avatar: chat payload does NOT carry avatar data anymore (to keep packets small).
            // -> Prefer the avatar embedded in the message (if present); otherwise resolve it from FriendsStore.
            val avatarB64 = m.senderAvatarBase64 ?: FriendsStore.get(m.senderId)?.avatarBase64
            val bmp = getAvatarCached(avatarB64)
            if (bmp != null) {
                imgAvatar?.setImageBitmap(bmp)
            }
        }
    }
}
