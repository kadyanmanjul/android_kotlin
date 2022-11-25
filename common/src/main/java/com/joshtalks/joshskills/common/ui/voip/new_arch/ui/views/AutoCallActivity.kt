package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views

import android.content.Intent
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.constants.*
import com.joshtalks.joshskills.common.databinding.ActivityAutoCallBinding
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.viewmodels.AutoCallViewModel
import com.joshtalks.joshskills.common.voip.constant.CALL_NOW
import com.joshtalks.joshskills.common.voip.constant.Category
import com.joshtalks.joshskills.common.voip.constant.STOP_WAITING
import com.joshtalks.joshskills.common.voip.voipanalytics.CallAnalytics
import com.joshtalks.joshskills.common.voip.voipanalytics.EventName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class AutoCallActivity : com.joshtalks.joshskills.common.base.BaseActivity() {
    private val binding by lazy<ActivityAutoCallBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_auto_call)
    }

    val vm by lazy {
        ViewModelProvider(this)[AutoCallViewModel::class.java]
    }

    lateinit var animationJob : Job

    override fun initViewBinding() {
        binding.vm = vm
        binding.executePendingBindings()
    }

    override fun onCreated() {
        animationJob = lifecycleScope.launchWhenStarted {
            withContext(Dispatchers.IO) {
                try {
                    val data = vm.setUIState()
                    if (data?.waitTime == null)
                        finish()
                    else
                        animate(data.waitTime)
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun initViewState() {
        event.observe(this) {
            when(it.what) {
                CALL_NOW -> callNow()
                STOP_WAITING -> {
                    CallAnalytics.addAnalytics(
                        event = EventName.AUTO_CONNECT_SKIP_PRESSED,
                        agoraCallId = "",
                        agoraMentorId = com.joshtalks.joshskills.voip.data.local.PrefManager.getLocalUserAgoraId().toString()
                    )
                    finish()
                }
            }
        }
    }

    private fun callNow() {
        CallAnalytics.addAnalytics(
            event = EventName.AUTO_CONNECT_CALL_NOW_PRESSED,
            agoraCallId = "",
            agoraMentorId = com.joshtalks.joshskills.voip.data.local.PrefManager.getLocalUserAgoraId().toString()
        )
        if(::animationJob.isInitialized)
            animationJob.cancel()
        val callIntent = Intent(this, VoiceCallActivity::class.java)
        callIntent.apply {
            putExtra(INTENT_DATA_COURSE_ID, "151")
            putExtra(INTENT_DATA_TOPIC_ID, "10")
            putExtra(
                STARTING_POINT,
                FROM_ACTIVITY
            )
            putExtra(INTENT_DATA_CALL_CATEGORY, Category.PEER_TO_PEER.ordinal)
        }
        startActivity(callIntent)
        finish()
    }

    private suspend fun animate(timeoutSecond : Long = 10) {
        var value = 0
        var timerText = timeoutSecond
        while(true) {
            value += 5
            withContext(Dispatchers.Main) {
                binding.progressBar.progress = value
                binding.timerTv.text = timerText.toString()
            }
            if(value >= 100) {
                value = 0
                timerText -= 1
            }
            if(timerText <= 0) {
                callNow()
                break
            }
            delay(50)
        }
    }
}