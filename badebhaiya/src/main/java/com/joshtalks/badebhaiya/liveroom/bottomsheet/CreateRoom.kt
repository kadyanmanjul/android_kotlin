package com.joshtalks.badebhaiya.liveroom.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.BottomSheetCreateRoomBinding
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse

class CreateRoom : BottomSheetDialogFragment() {
    interface CreateRoomCallback {
        fun onRoomCreated(conversationRoomResponse: ConversationRoomResponse, topic: String)
        fun onError(error: String)
    }

    private var callback: CreateRoomCallback? = null
    private lateinit var binding: BottomSheetCreateRoomBinding
    private val viewModel by activityViewModels<FeedViewModel>()

    companion object {
        fun newInstance(): CreateRoom {
            return CreateRoom()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater, R.layout.bottom_sheet_create_room, container, false
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.callback = callback
        return binding.root
    }

    fun addCallback(callback: CreateRoomCallback) {
        this.callback = callback
    }

}