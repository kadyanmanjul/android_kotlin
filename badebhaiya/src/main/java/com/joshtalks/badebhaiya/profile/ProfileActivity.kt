package com.joshtalks.badebhaiya.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.ActivityProfileBinding

class ProfileActivity: AppCompatActivity() {

    private val binding by lazy<ActivityProfileBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_profile)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
}