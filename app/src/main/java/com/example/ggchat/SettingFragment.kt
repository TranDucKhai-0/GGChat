package com.example.ggchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import androidx.fragment.app.Fragment

class SettingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Lấy switch
        val switch1 = view.findViewById<SwitchMaterial>(R.id.themesChat)
        val switch2 = view.findViewById<SwitchMaterial>(R.id.volume)

        // Cập nhật chữ lúc khởi tạo
        switch1.text = if (switch1.isChecked) "ON" else "OFF"
        switch2.text = if (switch2.isChecked) "ON" else "OFF"

        // Lắng nghe sự kiện bật/tắt
        switch1.setOnCheckedChangeListener { _, Checked ->
            switch1.text = if (Checked) "ON" else "OFF"
        }
        switch2.setOnCheckedChangeListener { _, Checked ->
            switch2.text = if (Checked) "ON" else "OFF"
        }

        return view
    }
}
