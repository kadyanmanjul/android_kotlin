package com.joshtalks.badebhaiya.customViews

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.databinding.ActivityProfileViewTestBinding

class ProfileViewTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileViewTestBinding

    var userName = "Sahil Khan"
    val mimageUrl = "https://www.dmarge.com/wp-content/uploads/2021/01/dwayne-the-rock--480x320.jpg"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_view_test)
        binding.handler = this
//        mimageUrl = "https://www.dmarge.com/wp-content/uploads/2021/01/dwayne-the-rock--480x320.jpg"
//        binding.profileView.userName = userName

    }
}