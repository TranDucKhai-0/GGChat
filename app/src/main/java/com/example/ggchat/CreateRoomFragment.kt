package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.net.ServerSocket
import com.example.ggchat.network.RoomBroadcaster


class CreateRoomFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_room, container, false)

        val editName = view.findViewById<EditText>(R.id.editRoomName)

        // Restore the previously saved name.
        val oldName = UserData.getMyRoomName(requireContext())
        if (oldName.isNotEmpty()) {
            editName.setText(oldName)
        }

        // When the keyboard is dismissed, auto-save to UserData.
        editName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) { // Losing focus means the user finished typing or dismissed the keyboard.
                val name = editName.text.toString().trim()
                if (name.isNotEmpty()) {
                    UserData.saveMyRoomName(requireContext(), name) // Save room name
                }
            }
        }

        // Button next
        val nextButton = view.findViewById<ImageButton>(R.id.nextToRoomChat)
        nextButton.setOnClickListener {
            // Read the value directly from the EditText.
            val roomName = editName.text.toString().trim()

            if (roomName.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập tên phòng!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Persist the room name to UserData.
            UserData.saveMyRoomName(requireContext(), roomName)

            // Pick a free (unused) port.
            val roomPort = getAvailablePort()
            UserData.saveMyRoomPort(requireContext(), roomPort)

            // Load the IP address from UserData.
            val ip = UserData.getUserIP(requireContext()).ifEmpty {
                (activity as? MainActivity)?.getLocalIpAddress() ?: "0.0.0.0"
            }

            // Broadcast the room JSON via UDP on port 9999.
            RoomBroadcaster.broadcastOnce(roomName, ip, roomPort)
            // Navigate to the room chat screen.
            (activity as? MainActivity)?.replaceFragment(
                RoomChatFragment.newInstance(
                    roomName = roomName,
                    isHost = true,
                    hostIp = ip,
                    hostPort = roomPort
                )
            )
        }
        return view
    }

    // Pick a random free TCP port.
    private fun getAvailablePort(): Int {
        val socket = ServerSocket(0)
        val port = socket.localPort
        socket.close()
        return port
    }

}
