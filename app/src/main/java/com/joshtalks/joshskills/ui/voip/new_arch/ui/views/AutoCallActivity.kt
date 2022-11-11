package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.eval
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ActivityAutoCallBinding
import com.joshtalks.joshskills.databinding.ActivityVoiceCallBinding
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.AutoConnectData
import com.joshtalks.joshskills.ui.voip.new_arch.ui.models.SearchingRule
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.AutoCallViewModel
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel
import com.joshtalks.joshskills.voip.constant.CALL_NOW
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.constant.STOP_WAITING
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

class AutoCallActivity : BaseActivity () {
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
                STOP_WAITING -> finish()
            }
        }
    }

    private fun callNow() {
        if(::animationJob.isInitialized)
            animationJob.cancel()
        val callIntent = Intent(this, VoiceCallActivity::class.java)
        callIntent.apply {
            putExtra(INTENT_DATA_COURSE_ID, "151")
            putExtra(INTENT_DATA_TOPIC_ID, "10")
            putExtra(STARTING_POINT, FROM_ACTIVITY)
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