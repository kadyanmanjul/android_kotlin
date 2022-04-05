package com.joshtalks.badebhaiya.launcher

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.workers.WorkManagerAdmin
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.signup.fragments.SignUpEnterPhoneFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initApp()
        setContentView(R.layout.activity_launcher)
    }

    private fun initApp() {
        lifecycleScope.launch(Dispatchers.IO) {
            WorkManager.getInstance(applicationContext).cancelAllWork()
            WorkManagerAdmin.appInitWorker()
            //Branch.getInstance().resetUserSession()
            delay(1000)
            startActivity(getIntentForState())
        }
    }

    fun getIntentForState(): Intent {
        val intent: Intent = when {
            User.getInstance().userId.isNotBlank() -> {
                Intent(this@LauncherActivity, FeedActivity::class.java)
            }
            else -> Intent(this@LauncherActivity, SignUpActivity::class.java)
        }
        return intent.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
