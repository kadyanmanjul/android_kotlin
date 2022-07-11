package com.joshtalks.badebhaiya.showCallRequests

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.joshtalks.badebhaiya.composeTheme.JoshBadeBhaiyaTheme
import com.joshtalks.badebhaiya.utils.open

class CallRequestsListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JoshBadeBhaiyaTheme {
                CallRequestsListScreen()
            }
        }
    }

    companion object {
        fun open(context: Context){
            Intent(context, CallRequestsListActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }

}