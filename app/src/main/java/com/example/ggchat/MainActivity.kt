package com.example.ggchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    //-----------------Tương tác của Action Bar-------------------
    private lateinit var actionBarFragment: ActionBarFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Nạp ActionBarFragment
        actionBarFragment = ActionBarFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_action_bar, actionBarFragment)
            .commit()

        // Hiển thị HomeFragment mặc định
        // replaceFragment(Profile())
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // gọi từ ActionBar khi bấm setting
    fun openSettingFragment() {
        replaceFragment(SettingFragment())
    }

    // Dùng khi muốn đổi tiêu đề action bar sang chat mode
    fun showChatBar(chatName: String) {
        actionBarFragment.showChatBar(chatName)
    }

    fun showDefaultBar() {
        actionBarFragment.showDefault()
    }
    //----------------------------------------------------------------------
}
