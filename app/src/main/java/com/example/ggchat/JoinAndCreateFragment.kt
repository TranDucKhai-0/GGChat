package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment

class JoinAndCreateFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the fragment layout.
        val view = inflater.inflate(R.layout.fragment_join_and_create, container, false)

        // Find views in the layout.
        val textIPLAN = view.findViewById<TextView>(R.id.textIPLAN)
        // Create Room
        val createRoomButton = view.findViewById<FrameLayout>(R.id.buttonCreateRoom)
        // Join Room
        val joinRoomButton = view.findViewById<FrameLayout>(R.id.buttonJoinRoom)

        // Load the IP from UserData.
        val currentIP = UserData.getUserIP(requireContext())
        // Update the displayed IP address.
        textIPLAN.text = "IP: $currentIP"

        // Get a reference to MainActivity.
        val mainActivity = activity as? MainActivity

        // Navigate when the Create Room button is clicked.
        createRoomButton.setOnClickListener {
            mainActivity?.replaceFragment(CreateRoomFragment())
        }

        // Navigate when the Join Room button is clicked.
        joinRoomButton.setOnClickListener {
            mainActivity?.replaceFragment(JoinRoomFragment())
        }

        return view
    }
}

