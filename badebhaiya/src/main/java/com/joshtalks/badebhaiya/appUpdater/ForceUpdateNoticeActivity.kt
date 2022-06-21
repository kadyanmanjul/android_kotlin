package com.joshtalks.badebhaiya.appUpdater

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ForceUpdateNoticeActivity: ComponentActivity() {

    @Inject
    lateinit var appUpdater: JoshAppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ForceUpdateNoticeScreen(
                onDownloadClick = {
                    finish()
                    appUpdater.onDownloadClick()
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

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }
}