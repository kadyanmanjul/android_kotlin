package com.joshtalks.badebhaiya.launcher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.workers.WorkManagerAdmin
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.signup.SignUpActivity.Companion.REDIRECT_TO_ENTER_NAME
import com.joshtalks.badebhaiya.signup.SignUpActivity.Companion.REDIRECT_TO_PROFILE_ACTIVITY
import io.branch.referral.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        initApp()
    }

    override fun onStart() {
        super.onStart()
        initBranch()
    }


    private fun initApp() {
        WorkManager.getInstance(applicationContext).cancelAllWork()
        WorkManagerAdmin.appStartWorker()
    }

    private fun initBranch() {
        Branch.sessionBuilder(this@LauncherActivity).withCallback { referringParams, error ->
            if (error == null) {
                Log.d(
                    "LauncherActivity.kt",
                    "YASH => onInitFinished: $referringParams"
                )
                referringParams?.let {
                    startActivityForState(
                        if (it.has("user_id"))
                            it.getString("user_id")
                        else null
                    )
                }
            } else {
                Log.e("BRANCH SDK", error.message)
                startActivityForState()
            }
        }.withData(this.intent.data).init()
    }

    private fun startActivityForState(viewUserId: String? = null) {
        val intent: Intent = when {
            User.getInstance().userId.isNotBlank() -> {
                if (User.getInstance().firstName.isNullOrEmpty()) {
                    SignUpActivity.start(this, REDIRECT_TO_ENTER_NAME)
                    return
                }
                if (viewUserId == null) {
                    Intent(this@LauncherActivity, FeedActivity::class.java)
                } else {
                    ProfileActivity.getIntent(this@LauncherActivity, viewUserId)
                }
            }
            else -> SignUpActivity.getIntent(
                this@LauncherActivity,
//                REDIRECT_TO_PROFILE_ACTIVITY,
                viewUserId
            )
        }
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            startActivity(intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }
}
