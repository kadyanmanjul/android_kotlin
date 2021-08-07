package com.joshtalks.joshskills.ui.voip.voip_rating

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.LESSON_COMPLETE_SNACKBAR_TEXT_STRING
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.core.textDrawableBitmap
import com.joshtalks.joshskills.databinding.VoipCallFeedbackViewBinding
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import java.util.HashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

const val ARG_CALLER_IMAGE = "caller_image_url"
const val ARG_CALLER_NAME = "caller_name"
const val ARG_CHANNEL_NAME = "channel_name"
const val ARG_CALL_TIME = "call_time"
const val ARG_YOUR_NAME = "your_name"
const val ARG_YOUR_AGORA_ID = "your_agora_id"
const val ARG_DIM_BACKGROUND = "dim_bg"

class VoipCallFeedbackActivity : BaseActivity() {

    private lateinit var binding: VoipCallFeedbackViewBinding
    private var channelName: String = EMPTY
    private var yourAgoraId: Int = -1
    private var pointsString: String = EMPTY
    private var dimBg = false
    private var callerName: String = EMPTY
    private var yourName: String = EMPTY

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(applicationContext, R.color.white)
        dimBg = savedInstanceState?.getBoolean(ARG_DIM_BACKGROUND) ?: false

        binding = DataBindingUtil.setContentView(this, R.layout.voip_call_feedback_view)
        binding.lifecycleOwner = this
        binding.handler = this
        initFun(intent)
    }

    override fun onBackPressed() {
        submitFeedback("BACK")
        super.onBackPressed()
    }

    private fun addObserver() {
        practiceViewModel.pointsSnackBarText.observe(
            this,
            {
                if (it.pointsList.isNullOrEmpty().not()) {
                    showSnackBar(
                        binding.rootContainer,
                        Snackbar.LENGTH_LONG,
                        it.pointsList!!.get(0)
                    )
                    PrefManager.put(LESSON_COMPLETE_SNACKBAR_TEXT_STRING,it.pointsList!!.last(),false)

                }
            }
        )
    }

    fun initFun(arguments: Intent) {
        val msz = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOIP_FEEDBACK_MESSAGE_NEW)

        arguments?.let {
            channelName = it.getStringExtra(ARG_CHANNEL_NAME) ?: EMPTY
            yourAgoraId = it.getIntExtra(ARG_YOUR_AGORA_ID, 0)
            callerName = it.getStringExtra(ARG_CALLER_NAME) ?: EMPTY
            yourName = it.getStringExtra(ARG_YOUR_NAME) ?: EMPTY
            binding.txtMessage.text = msz.replaceFirst("#", callerName)

            binding.cImage.setImageResource(R.drawable.ic_call_placeholder)
            val image = it.getStringExtra(ARG_CALLER_IMAGE)
            if (image.isNullOrEmpty()) {
                binding.cImage.setImageBitmap(
                    callerName.textDrawableBitmap(
                        width = 96,
                        height = 96
                    )
                )
            } else {
                binding.cImage.setRoundImage(image)
            }

            val mTime = StringBuilder()
            val callTime = it.getLongExtra(ARG_CALL_TIME, 0L)
            val second: Int = (callTime / 1000 % 60).toInt()
            val minute: Int = (callTime / (1000 * 60) % 60).toInt()
            if (minute > 0) {
                mTime.append(minute).append(getMinuteString(minute))
            }
            if (second > 0) {
                mTime.append(second).append(getSecondString(second))
            }
            binding.txtSpoke.text = getString(R.string.spoke_for_minute, mTime.toString())
            binding.txtBottom.text = getString(R.string.block_user_hint, callerName, callerName)

            addObserver()
            practiceViewModel.getPointsForVocabAndReading(null, channelName = channelName)
        }
    }

    private fun getMinuteString(min: Int): String {
        if (min > 1) {
            return " minutes "
        }
        return " minute "
    }

    private fun getSecondString(sec: Int): String {
        if (sec > 1) {
            return " seconds "
        }
        return " second "
    }

    fun submitFeedback(response: String) {
        lifecycleScope.launch {
            withTimeout(550) {
                try {
                    val requestParams: HashMap<String, String> = HashMap()
                    requestParams["channel_name"] = channelName
                    requestParams["agora_mentor_id"] = yourAgoraId.toString()
                    requestParams["response"] = response
                    AppObjectController.p2pNetworkService.p2pCallFeedbackV2(requestParams)
                    WorkManagerAdmin.syncFavoriteCaller()
                    delay(250)
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
                when (response) {
                    "YES" -> {
                        showToast("$callerName is now added to your Favorite Practice Partners.")
                    }
                    "NO" -> {
                        showToast("$callerName is now added to your Blocklist.")
                    }
                    "MAYBE" -> {
                        showToast("Thank you for submitting the feedback.")
                    }
                }
                finishAndRemoveTask()
            }
        }
    }

    companion object {

        fun startPtoPFeedbackActivity(
            channelName: String?,
            callTime: Long,
            callerName: String?,
            callerImage: String?,
            yourName: String?,
            yourAgoraId: Int?,
            dimBg: Boolean = false,
            activity: Activity,
            flags: Array<Int> = arrayOf()
        ) {

            Intent(activity, VoipCallFeedbackActivity::class.java).apply {
                putExtra(ARG_CHANNEL_NAME, channelName)
                putExtra(ARG_CALL_TIME, callTime)
                putExtra(ARG_CALLER_NAME, callerName)
                putExtra(ARG_CALLER_IMAGE, callerImage)
                putExtra(ARG_YOUR_NAME, yourName)
                putExtra(ARG_YOUR_AGORA_ID, yourAgoraId ?: -1)
                putExtra(ARG_DIM_BACKGROUND, dimBg)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }
}
