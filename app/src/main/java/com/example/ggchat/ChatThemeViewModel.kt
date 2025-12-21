package com.example.ggchat

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatThemeViewModel : ViewModel() {
    val isDarkMode = MutableLiveData(false)
}
