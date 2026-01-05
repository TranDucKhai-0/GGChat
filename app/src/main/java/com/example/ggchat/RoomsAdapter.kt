package com.example.ggchat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class RoomsAdapter(
    private val onJoinClick: (RoomInfo) -> Unit
) : ListAdapter<RoomInfo, RoomsAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RoomInfo>() {
            override fun areItemsTheSame(oldItem: RoomInfo, newItem: RoomInfo): Boolean {
                // identity theo ip:port
                return oldItem.hostIp == newItem.hostIp && oldItem.port == newItem.port
            }

            override fun areContentsTheSame(oldItem: RoomInfo, newItem: RoomInfo): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_room_join, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val room = getItem(position)
        holder.tv.text = room.roomName

        // Click
        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        holder.itemView.setOnClickListener { onJoinClick(room) }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tvRoomJoin)
    }
}
