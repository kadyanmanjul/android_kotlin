package com.joshtalks.badebhaiya

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class TemporaryFeedActivity: AppCompatActivity() {

    companion object {
        fun openFeedActivity(context: Context) {
            Intent(context, TemporaryFeedActivity::class.java).run {
                context.startActivity(this)
            }
        }
    }
}