package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.fragment.app.Fragment
import android.widget.ImageButton
import androidx.fragment.app.activityViewModels



class SettingFragment : Fragment() {

    private val chatThemeViewModel: ChatThemeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        val switch1 = view.findViewById<SwitchMaterial>(R.id.themesChat)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)

        // trạng thái ban đầu
        switch1.isChecked = chatThemeViewModel.isDarkMode.value == true
        switch1.text = if (switch1.isChecked) "Dark" else "Light"

        switch1.setOnCheckedChangeListener { _, isChecked ->
            switch1.text = if (isChecked) "Dark" else "Light"
            chatThemeViewModel.isDarkMode.value = isChecked
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
            requireActivity().findViewById<View>(R.id.fragment_overlay).visibility = View.GONE
        }

        return view
    }
}

