package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class JoinAndCreateFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Nạp layout cho fragment
        val view = inflater.inflate(R.layout.fragment_join_and_create, container, false)

        // Tìm TextView trong layout
        val textIPLAN = view.findViewById<TextView>(R.id.textIPLAN)

        // Lấy IP trong UserData
        val currentIP = UserData.getUserIP(requireContext())

        textIPLAN.text = "IP: $currentIP"


        return view
    }
}
