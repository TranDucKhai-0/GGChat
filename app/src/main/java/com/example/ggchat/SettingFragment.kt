package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.ImageButton


class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        val switch1 = view.findViewById<SwitchMaterial>(R.id.themesChat)
        val switch2 = view.findViewById<SwitchMaterial>(R.id.volume)
        val backButton = view.findViewById<ImageButton>(R.id.backButton)


        // Cập nhật chữ lúc khởi tạo
        switch1.text = if (switch1.isChecked) "Light" else "Dark"
        switch2.text = if (switch2.isChecked) "ON" else "OFF"

        // Lắng nghe sự kiện bật/tắt
        switch1.setOnCheckedChangeListener { _, isChecked ->
            switch1.text = if (isChecked) "ON" else "OFF"
        }
        switch2.setOnCheckedChangeListener { _, isChecked ->
            switch2.text = if (isChecked) "ON" else "OFF"
        }

        // Nút quay lại (đóng Setting)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            requireActivity().findViewById<View>(R.id.fragment_overlay).visibility = View.GONE
        }


        return view
    }
}
