package com.joshtalks.joshskills.ui.voip.voip_rating

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ActivityCallRatingDialogBinding
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CallRatingDialogActivity : AppCompatActivity() {
    private var callerName = EMPTY
    private var callDuration = 0
    private var agoraCallId = 0
    private var callerProfileUrl : String? = null
    private var callerMentorId = EMPTY
    private var agoraMentorId = EMPTY

    private lateinit var binding : ActivityCallRatingDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_call_rating_dialog
        )
        binding.lifecycleOwner = this
        initIntentObject()
        CallRatingsFragment.newInstance(
            callerName,
            callDuration,
            agoraCallId,
            callerProfileUrl,
            callerMentorId,
            agoraMentorId
        ).show(supportFragmentManager, "CallRatingsFragment")
    }


    override fun onBackPressed() {
        if(!SearchingUserActivity.backPressMutex.isLocked) {
            CoroutineScope(Dispatchers.Main).launch {
                backPressMutex.withLock {
                    delay(1000)
                }
            }
        } else{
            closeActivity()
            super.onBackPressed()
        }
    }

    fun closeActivity(){
        finishAndRemoveTask()
    }

    private fun initIntentObject() {
        if (intent.hasExtra(CALLER_NAME)) callerName = intent.getStringExtra(CALLER_NAME).toString()
        if (intent.hasExtra(CALL_DURATION)) callDuration = intent.getIntExtra(CALL_DURATION, 0)
        if (intent.hasExtra(AGORA_ID)) agoraCallId = intent.getIntExtra(AGORA_ID, 0)
        if (intent.hasExtra(PROFILE_URL)) callerProfileUrl = intent.getStringExtra(PROFILE_URL).toString()
        if (intent.hasExtra(CALLER_MENTOR_ID)) callerMentorId = intent.getStringExtra(CALLER_MENTOR_ID).toString()
        if (intent.hasExtra(AGORA_MENTOR_ID)) agoraMentorId = intent.getStringExtra(AGORA_MENTOR_ID).toString()
    }

    companion object {
        const val CALLER_NAME = "caller_name"
        const val CALL_DURATION = "call_duration"
        const val AGORA_ID = "agora_id"
        const val PROFILE_URL = "profile_url"
        const val CALLER_MENTOR_ID = "caller_mentor_id"
        const val AGORA_MENTOR_ID = "agora_mentor_id"
        var backPressMutex = Mutex()

        fun startCallRatingDialogActivity(activity: Activity,
                                          callerName: String,
                                          callDuration: Int,
                                          agoraCallId: Int,
                                          callerProfileUrl: String?,
                                          callerMentorId : String,
                                          agoraMentorId : String) {
            val intent = Intent(activity, CallRatingDialogActivity::class.java).apply {
                putExtra(CALLER_NAME, callerName)
                putExtra(CALL_DURATION, callDuration)
                putExtra(AGORA_ID, agoraCallId)
                putExtra(PROFILE_URL, callerProfileUrl)
                putExtra(CALLER_MENTOR_ID, callerMentorId)
                putExtra(AGORA_MENTOR_ID, agoraMentorId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
            activity.overridePendingTransition(R.anim.slide_up_dialog, R.anim.slide_out_top)
        }
    }
}