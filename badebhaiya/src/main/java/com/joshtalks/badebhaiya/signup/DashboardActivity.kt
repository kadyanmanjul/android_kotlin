package com.joshtalks.badebhaiya.signup

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.showToast

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
    }

    fun click_listener(){

        showToast("clicked")

    }
}