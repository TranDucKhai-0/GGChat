package com.example.ggchat

import FriendsAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive


class ListFriendFragment : Fragment() {

    private lateinit var adapter: FriendsAdapter
    private var job: kotlinx.coroutines.Job? = null
    private var lastSig: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_list_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvFriends)
        rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        adapter = FriendsAdapter(mutableListOf())
        rv.adapter = adapter

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            requireActivity().findViewById<View>(R.id.fragment_overlay).visibility = View.GONE
        }

        // Update the list in near-real-time (poll), but only re-render when the data changes.
        job = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                val list = FriendsStore.getAll()
                val sig = buildString {
                    list.forEach {
                        append(it.ip)
                        append('|')
                        append(it.name)
                        append('|')
                        append(it.avatarBase64?.hashCode() ?: 0)
                        append(';')
                    }
                }
                if (sig != lastSig) {
                    lastSig = sig
                    adapter.submit(list)
                }
                delay(500)
            }
        }

    }

    override fun onDestroyView() {
        job?.cancel()
        job = null
        super.onDestroyView()
    }
}

