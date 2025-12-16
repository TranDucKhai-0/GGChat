package com.example.ggchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment



class MainActivity : AppCompatActivity() {

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

        // --- TÍCH HỢP LOGIC CHAT ---
        // 1. Khi mở app -> Join luôn
        ChatService.joinChat("Đức Mobile")

        // 2. Lắng nghe tin nhắn đến
        ChatService.listenForMessages { msg: ChatMessage -> // <-- Sửa ở đây
            // Code hiển thị tin nhắn lên màn hình (RecyclerView hoặc TextView)
            // Ví dụ test:
            Log.d("TIN_NHAN_MOI", "TỪ ${msg.userName}: ${msg.content}")
        }

        // 3. Gửi tin nhắn
        // Giao diện nút gửi và ô nhập liệu của bạn có thể nằm trong một Fragment khác.
        // Hãy đặt đoạn code tương tự như sau vào trong Fragment đó, tại sự kiện click của nút gửi.
        /*
        buttonSend.setOnClickListener {
            val text = editText.text.toString()
            ChatService.sendMessage(text)
            editText.text.clear()
        }
        */
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // gọi từ ActionBar khi bấm setting
    fun openOverlayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_overlay, fragment)
            .addToBackStack(null)
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
