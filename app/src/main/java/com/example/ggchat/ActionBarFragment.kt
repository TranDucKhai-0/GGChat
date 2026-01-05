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

        // Default: show the title.
        showDefault()

        // Handle the Back button click.
        backButton.setOnClickListener {
            (activity as? MainActivity)?.onBackPressedDispatcher?.onBackPressed()
        }

        // Handle the Settings button click (open the settings fragment).
        settingButton.setOnClickListener {
            // Call MainActivity to replace the current fragment.
            (activity as? MainActivity)?.openOverlayFragment(SettingFragment())
        }

        return view
    }

    /** Reset the action bar to its default state. */
    fun showDefault() {
        tvTitle.text = "GGChat"
        backButton.visibility = View.VISIBLE
        settingButton.visibility = View.VISIBLE
    }

    /** Update the action bar title for the current chat. */
    fun showChatBar(chatName: String) {
        tvTitle.text = chatName
    }

}