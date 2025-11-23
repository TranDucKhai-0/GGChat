package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment

class ActionBarFragment : Fragment() {
    private lateinit var tvTitle: TextView
    private lateinit var backButton: ImageButton
    private lateinit var settingButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_action_bar, container, false)

        tvTitle = view.findViewById(R.id.tvTitle)
        backButton = view.findViewById(R.id.backButton)
        settingButton = view.findViewById(R.id.settingButton)

        // Mặc định: hiện tiêu đề
        showDefault()

        // xử lý sự kiện click back
        backButton.setOnClickListener {
            (activity as? MainActivity)?.onBackPressedDispatcher?.onBackPressed()
        }

        // xử lý sự kiện click setting (mở fragment Setting)
        settingButton.setOnClickListener {
            // Gọi sang MainActivity để thay fragment
            (activity as? MainActivity)?.openOverlayFragment(SettingFragment())
        }

        return view
    }

    /** Đặt Action Bar về trạng thái mặc định */
    fun showDefault() {
        tvTitle.text = "GGChat"
        backButton.visibility = View.VISIBLE
        settingButton.visibility = View.VISIBLE
    }

    /** Đổi tên của đoạn chat */
    fun showChatBar(chatName: String) {
        tvTitle.text = chatName
    }
}