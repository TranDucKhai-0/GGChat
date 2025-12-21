package com.example.ggchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.fragment.app.Fragment


class MainActivity : AppCompatActivity() {

    val chatThemeViewModel: ChatThemeViewModel by viewModels()

    //-----------------Tương tác của Action Bar-------------------
    private lateinit var actionBarFragment: ActionBarFragment
    private lateinit var profileFragment: ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //View
        setContentView(R.layout.activity_main)

        // Nạp ActionBarFragment
        actionBarFragment = ActionBarFragment()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_action_bar, actionBarFragment)
            .commit()

        // --- Hiển thị ProfileFragment mặc định ---
        if (savedInstanceState == null) {
            profileFragment = ProfileFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .commit() // KHÔNG addToBackStack
        }

        // --- Lấy IP LAN của máy-----
        val ip = getLocalIpAddress()
        UserData.saveUserIP(this, ip)
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,  // enter
                R.anim.slide_out_left,  // exit
                R.anim.slide_in_left,   // popEnter (khi back)
                R.anim.slide_out_right  // popExit  (khi back)
            )
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // gọi từ ActionBar khi bấm setting
    fun openOverlayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_down,   // enter
                R.anim.slide_out_down,  // exit
                R.anim.slide_in_up,  // popEnter (khi back)
                R.anim.slide_out_up  // popExit
            )
            .add(R.id.fragment_overlay, fragment)
            .addToBackStack("overlay")
            .commit()

        // hiện vùng overlay lên
        findViewById<FrameLayout>(R.id.fragment_overlay).visibility = View.VISIBLE
    }

    // Dùng khi muốn đổi tiêu đề action bar sang chat mode
    fun showChatBar(chatName: String) {
        actionBarFragment.showChatBar(chatName)
    }

    fun showDefaultBar() {
        actionBarFragment.showDefault()
    }

    // Hàm lấy IP LAN
    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val name = intf.name
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    android.util.Log.d("DEBUG_IP", "Interface: $name  ->  ${addr.hostAddress}")
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "NULL"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "NULL"
    }

    fun reNameActionBar(chatName: String) {
        actionBarFragment.showChatBar(chatName)
    }



    //----------------------------------------------------------------------
}
