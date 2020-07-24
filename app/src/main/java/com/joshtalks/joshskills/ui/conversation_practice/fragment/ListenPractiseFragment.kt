package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel
import kotlinx.android.synthetic.main.fragment_listen_practise.recycler_view
import java.util.*

class ListenPractiseFragment private constructor() : Fragment() {

    private lateinit var listen: List<ListenModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listen = it.getParcelableArrayList(ARG_AUDIO_LIST) ?: ArrayList()

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_listen_practise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
    }

    private fun initRV() {
        recycler_view.layoutManager = LinearLayoutManager(context)

    }

    companion object {
        const val ARG_AUDIO_LIST = "audio-list"

        @JvmStatic
        fun newInstance(listen: List<ListenModel>) =
            ListenPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_AUDIO_LIST, ArrayList(listen))
                }
            }
    }

}