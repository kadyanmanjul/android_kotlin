package com.joshtalks.badebhaiya.feed

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.liveroom.bottomsheet.CreateRoom
import com.joshtalks.badebhaiya.repository.model.ConversationRoomResponse
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.showToast

class FeedActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun getInstance() = FeedActivity()
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }

    private val binding by lazy<ActivityFeedBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_feed)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getRooms()
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
    }

    fun openCreateRoomDialog() {
        CreateRoom.newInstance().also {
            it.show(supportFragmentManager, "createRoom")
            it.addCallback(object : CreateRoom.CreateRoomCallback {
                override fun onRoomCreated(conversationRoomResponse: ConversationRoomResponse) {
                    // TODO: 01/04/2022 - @kadyanmanjul start conversation room here
                    it.dismiss()
                }

                override fun onError(error: String) {
                    showToast(error)
                    it.dismiss()
                }
            })
        }
    }
}
