package com.joshtalks.badebhaiya.feed

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.ActivityFeedBinding
import com.joshtalks.badebhaiya.repository.model.User

class FeedActivity : AppCompatActivity() {

    companion object {
        @JvmStatic
        fun getInstance() = FeedActivity()
    }

    private val binding by lazy<ActivityFeedBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_feed)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[FeedViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.lifecycleOwner = this
        viewModel.getRooms(User.getInstance().userId)
        addObserver()
    }

    private fun addObserver() {

    }
}
