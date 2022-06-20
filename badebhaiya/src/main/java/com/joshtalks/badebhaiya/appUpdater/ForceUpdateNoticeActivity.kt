package com.joshtalks.badebhaiya.appUpdater

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ForceUpdateNoticeActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForceUpdateNoticeScreen(
                onDownloadClick = {
                    finish()
                },
                onExitClick = {
                    finishAffinity()
                }
            )
        }
    }

    companion object {
        fun launch(context: Context){
            Intent(context, ForceUpdateNoticeActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }
}