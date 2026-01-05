package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog


class JoinRoomFragment : Fragment() {

    private var waitingDialog: AlertDialog? = null
    private var isRequesting = false

    private lateinit var rvRooms: RecyclerView
    private lateinit var adapter: RoomsAdapter

    // key = "ip:port"
    private val rooms = linkedMapOf<String, RoomInfo>()

    private var cleanupJob: Job? = null
    private val ROOM_TTL_MS = 3_000L
    private val CLEANUP_INTERVAL_MS = 1_000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_join_room, container, false)

        rvRooms = view.findViewById(R.id.rvRooms)
        rvRooms.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        adapter = RoomsAdapter { room -> requestJoin(room) }
        rvRooms.adapter = adapter
        rvRooms.itemAnimator = RoomItemAnimator()

        RoomListener.startListening(object : RoomListener.OnRoomFoundListener {
            override fun onRoomFound(name: String, ip: String, port: Int) {
                // Nếu fragment đã detach mà vẫn nhận broadcast => bỏ qua để tránh crash
                val act = activity ?: return
                act.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    upsertRoom(name, ip, port)
                }
            }
        })

        startCleanupLoop()
        return view
    }

    private fun upsertRoom(name: String, ip: String, port: Int) {
        val key = "$ip:$port"
        val now = System.currentTimeMillis()

        val existing = rooms[key]
        if (existing != null) {
            // update lastSeen + (nếu đổi tên)
            rooms[key] = existing.copy(
                roomName = name,
                lastSeenMs = now
            )
        } else {
            // add mới
            rooms[key] = RoomInfo(
                roomName = name,
                hostIp = ip,
                port = port,
                lastSeenMs = now
            )
        }

        submitRooms()
    }

    private fun startCleanupLoop() {
        cleanupJob?.cancel()
        cleanupJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL_MS)
                cleanupExpiredRooms()
            }
        }
    }

    private fun cleanupExpiredRooms() {
        val now = System.currentTimeMillis()
        val expiredKeys = rooms
            .filterValues { now - it.lastSeenMs > ROOM_TTL_MS }
            .keys
            .toList()

        if (expiredKeys.isEmpty()) return

        expiredKeys.forEach { rooms.remove(it) }
        submitRooms()
    }

    private fun submitRooms() {
        // tuỳ mày muốn sort kiểu gì (mới thấy lên trước)
        val list = rooms.values
            .sortedByDescending { it.lastSeenMs }

        adapter.submitList(list)
    }

    override fun onDestroyView() {
        cleanupJob?.cancel()
        cleanupJob = null
        RoomListener.stopListening()
        hideWaitingDialog()
        super.onDestroyView()
    }

    private fun requestJoin(room: RoomInfo) {
        if (isRequesting) return
        isRequesting = true

        showWaitingDialog(room.roomName) {
            hideWaitingDialog()
            isRequesting = false
            android.widget.Toast.makeText(requireContext(), "Đã hủy yêu cầu", android.widget.Toast.LENGTH_SHORT).show()
        }

        val myName = UserData.getUserName(requireContext()).ifBlank {
            UserData.getUserIP(requireContext()) // fallback nếu chưa có tên
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val result = JoinRequester.requestJoin(
                hostIp = room.hostIp,
                hostPort = room.port,
                roomName = room.roomName,
                clientName = myName
            )

            if (!isAdded) return@launch   // tránh crash nếu fragment gone

            hideWaitingDialog()
            isRequesting = false

            when (result) {
                is JoinResult.Accepted -> {
                    // 1) Lưu endpoint server để lần sau còn reconnect
                    ServerEndpointStore.save(
                        context = requireContext(),
                        hostIp = room.hostIp,
                        roomPort = room.port,
                        tcpPort = result.tcpPort,
                        roomName = room.roomName
                    )

                    (activity as? MainActivity)?.replaceFragment(
                        RoomChatFragment.newInstance(
                            roomName = room.roomName,
                            isHost = false,
                            hostIp = room.hostIp,
                            hostPort = room.port
                        )
                    )
                }
                is JoinResult.Denied -> {
                    android.widget.Toast.makeText(requireContext(), "Bị từ chối", android.widget.Toast.LENGTH_SHORT).show()
                }
                is JoinResult.Timeout -> {
                    android.widget.Toast.makeText(requireContext(), "Host không phản hồi", android.widget.Toast.LENGTH_SHORT).show()
                }
                is JoinResult.Error -> {
                    android.widget.Toast.makeText(requireContext(), "Lỗi: ${result.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun showWaitingDialog(roomName: String, onCancel: () -> Unit) {
        if (!isAdded) return
        waitingDialog?.dismiss()

        waitingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Đang chờ xác nhận")
            .setMessage("Đang gửi yêu cầu tham gia \"$roomName\"...\nVui lòng chờ host chấp nhận.")
            .setCancelable(false)
            .setNegativeButton("Hủy") { _, _ ->
                onCancel()
            }
            .create()

        waitingDialog?.show()
    }

    private fun hideWaitingDialog() {
        waitingDialog?.dismiss()
        waitingDialog = null
    }

}


