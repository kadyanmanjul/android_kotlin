package com.joshtalks.badebhaiya.launcher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.appUpdater.JoshAppUpdater
import com.joshtalks.badebhaiya.core.workers.WorkManagerAdmin
import com.joshtalks.badebhaiya.customViews.ProfileViewTestActivity
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.signup.SignUpActivity.Companion.REDIRECT_TO_ENTER_NAME
import com.joshtalks.badebhaiya.utils.SingleDataManager
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.collectStateFlow
import com.userexperior.UserExperior
import dagger.hilt.android.AndroidEntryPoint
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.Defines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class LauncherActivity : AppCompatActivity(), Branch.BranchReferralInitListener {

    @Inject
    lateinit var appUpdater: JoshAppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        appUpdater.checkAndUpdate(this)

        if (!User.getInstance().isGuestUser)
            BBRepository().lastLogin()

        UserExperior.startRecording(getApplicationContext(), BuildConfig.USER_EXPERIOR_API_KEY)

        //if(Settings.Global.getInt(getContentResolver(), Settings.Global.AUTO_TIME) == 1)
        //     {
        // Enabled
        //showToast("Auto Time Enabled")
        SingleDataManager.pendingPilotAction = null
        SingleDataManager.pendingPilotEventData = null
        initApp()

//             }
//             else
//             {
//                 // Disabed
//                 showToast("Auto Time Disabled")
//                 finish()
//             }
        //setAutoTimeEnabled(boolean enabled)
        collectStateFlow(appUpdater.isUpdateAvailable) { updateAvailable ->
            if (!updateAvailable) {
                appUpdater.flushResources()
//                initBranch()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Utils.isInternetAvailable()) {
            WorkManagerAdmin.requiredTaskAfterLoginComplete()
            Branch.sessionBuilder(this)
                .withCallback(this)
                .withData(this.intent?.data)
                .init()
        } else {
            startActivityForState()
        }

    }

    override fun onResume() {
        super.onResume()
        appUpdater.checkIfUpdating()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        appUpdater.onResult(requestCode, resultCode)
    }


    private fun initApp() {
        WorkManager.getInstance(applicationContext).cancelAllWork()
        if (User.getInstance().isLoggedIn())
            WorkManagerAdmin.appStartWorker()
    }

    private fun initBranch() {
        /*Branch.sessionBuilder(this@LauncherActivity).withCallback { referringParams, error ->

        }.withData(this.intent.data).init()*/
    }

    private fun startActivityForState(viewUserId: String? = null, request_dialog: Boolean?=false, room_id:Int?=null) {
        Log.i("CHECKGUEST", "startActivityForState: $viewUserId ------$room_id")
        val intent: Intent = when {
            !User.getInstance().isGuestUser -> {
                if (User.getInstance().firstName.isNullOrEmpty()) {
                    SignUpActivity.start(this, REDIRECT_TO_ENTER_NAME)
                    overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
                    return
                }
//                if (viewUserId == null) {
//                    Log.i("YASHENDRA", "startActivityForState:  Feed")
//                    Intent(this@LauncherActivity, FeedActivity::class.java)
//                } else {
//                    openProfile(viewUserId)
//                    Log.i("YASHENDRA", "startActivityForState:  profile")
//                    ProfileActivity.getIntent(this@LauncherActivity, viewUserId, true)
//                }
                val intent = Intent(this@LauncherActivity, FeedActivity::class.java)
//                val intent = Intent(this@LauncherActivity, ProfileViewTestActivity::class.java)
                intent.putExtra("userId", viewUserId)
                intent.putExtra("request_dialog",request_dialog)
                intent.putExtra("room_id", room_id)
                intent
            }
            else -> {
                // User is not logged in.

                if (viewUserId != null || room_id!=null) {
                    Log.i("CHECKGUEST", "startActivityForState: checkpint 1")
                    // came by deeplink.. redirect to profile
                    val intent = Intent(this@LauncherActivity, FeedActivity::class.java)
//                    val intent = Intent(this@LauncherActivity, ProfileViewTestActivity::class.java)
                    intent.putExtra("userId", viewUserId)
                    intent.putExtra("profile_deeplink", true)
                    intent.putExtra("request_dialog",request_dialog)
                    intent.putExtra("room_id", room_id)
                    intent

                } else {
                    SignUpActivity.getIntent(
                        this@LauncherActivity,
//                REDIRECT_TO_PROFILE_ACTIVITY,
                        viewUserId
                    )
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            delay(1000)
            startActivity(intent.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            overridePendingTransition(R.anim.fade_in,R.anim.fade_out)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        if (intent.hasExtra(Defines.IntentKeys.ForceNewBranchSession.key) && intent.getBooleanExtra(
                Defines.IntentKeys.ForceNewBranchSession.key,
                false
            )
        ) {
            Branch.sessionBuilder(this).withCallback(this@LauncherActivity).reInit()
        }
    }

    override fun onInitFinished(referringParams: JSONObject?, error: BranchError?) {
        Log.d("YASHENDRA", "initBranch: $referringParams")
        if (error == null) {
            Log.d(
                "LauncherActivity.kt",
                "YASH => onInitFinished: $referringParams"
            )
            referringParams?.let {
                Log.d("CHECKGUEST", "branch json data => ${it}")

                if(it.has("is_recorded_room")) {//TODO:-identification for type of deeplink
                     }

                startActivityForState(
                    if (it.has("user_id"))
                        it.getString("user_id")
                    else null,
                    if (it.has("request_dialog"))
                        it.getBoolean("request_dialog")
                    else null,
                    if (it.has("recorded_room_id")) {
                        it.get("recorded_room_id").toString().toInt()
                    }
                    else null
                )

//                    startActivityForState(
//                        "cb91868f-2e8c-4c09-8003-2bd480534d7e"
//                    )
            }
        } else {
            Log.e("BRANCH SDK", error.message)
            startActivityForState()
        }
    }
}
