package com.joshtalks.joshskills.ui.voip.voip_rating

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.LESSON_COMPLETE_SNACKBAR_TEXT_STRING
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.textDrawableBitmap
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.databinding.VoipCallFeedbackViewBinding
import com.joshtalks.joshskills.repository.local.model.KFactor
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.ui.voip.SHOW_FPP_DIALOG
import com.joshtalks.joshskills.ui.voip.share_call.ShareWithFriendsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.Response

const val ARG_CALLER_IMAGE = "caller_image_url"
const val ARG_CALLER_NAME = "caller_name"
const val ARG_CHANNEL_NAME = "channel_name"
const val ARG_CALL_TIME = "call_time"
const val ARG_YOUR_NAME = "your_name"
const val ARG_YOUR_AGORA_ID = "your_agora_id"
const val ARG_DIM_BACKGROUND = "dim_bg"
const val ARG_CALLER_ID = "caller_id"
const val ARG_CURRENT_ID= "current_id"
const val SHARE_SCREEN_MINUTES_THRESHOLD = "SHARE_SCREEN_MINUTES_THRESHOLD"

class VoipCallFeedbackActivity : BaseActivity(){

    private lateinit var binding: VoipCallFeedbackViewBinding
    private var channelName: String = EMPTY
    private var yourAgoraId: Int = -1
    private var pointsString: String = EMPTY
    private var dimBg = false
    private var callerName: String = EMPTY
    private var yourName: String = EMPTY
    private var callerId:Int = -1
    private var currentId:Int= -1
    private var minute = 0
    private var callerImage: String = EMPTY
    private var fppDialogFlag:String?=null
    private var p2pCallShareControl: Boolean = false

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
        if(intent!=null){
            initFun(intent)
        }else{
            closeActivity()
        }
        initABTest()

    }

    private fun initABTest(){
        practiceViewModel.getCampaignData(CampaignKeys.P2P_IMAGE_SHARING.name)
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
                    PrefManager.put(
                        LESSON_COMPLETE_SNACKBAR_TEXT_STRING,
                        it.pointsList!!.last(),
                        false
                    )

                }
            }
        )

        practiceViewModel.abTestCampaignliveData.observe(this){abTestCampaignData->
            abTestCampaignData?.let {map->
                p2pCallShareControl=(map.variantKey == VariantKeys.P2P_IS_ENABLED.name) && map.variableMap?.isEnabled == true
            }
        }

    }

    fun initFun(arguments: Intent) {
        val msz = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOIP_FEEDBACK_MESSAGE_NEW)

        arguments?.let {
            callerId=it.getIntExtra(ARG_CALLER_ID,-1)
            currentId=it.getIntExtra(ARG_CURRENT_ID,-1)
            channelName = it.getStringExtra(ARG_CHANNEL_NAME) ?: EMPTY
            yourAgoraId = it.getIntExtra(ARG_YOUR_AGORA_ID, 0)
            callerName = it.getStringExtra(ARG_CALLER_NAME) ?: EMPTY
            yourName = it.getStringExtra(ARG_YOUR_NAME) ?: EMPTY
            fppDialogFlag = it.getStringExtra(SHOW_FPP_DIALOG)
            binding.txtMessage.text = msz.replaceFirst("#", callerName)

            Log.e("Sagar", "initFun: $fppDialogFlag", )
            if (fppDialogFlag=="true"){
                binding.rootView.visibility  = View.VISIBLE
            }else{
                binding.rootView.visibility = View.GONE
            }

            binding.cImage.setImageResource(R.drawable.ic_call_placeholder)
            val image = it.getStringExtra(ARG_CALLER_IMAGE)
            callerImage = image?: EMPTY
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
            minute = (callTime / (1000 * 60) % 60).toInt()
            val totalSecond:Int=((minute*60)+second)

            if(totalSecond < 120 && PrefManager.getBoolValue(IS_COURSE_BOUGHT) ){
                showReportDialog("REPORT"){
                    closeActivity()
                }
            }
            if(totalSecond >1200 ){
                submitFeedback("20_min_call")
            }

            if (minute > 0) {
                mTime.append(minute).append(getMinuteString(minute))

                practiceViewModel.postGoal("SIV_GT_2MIN")
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

    private fun showReportDialog(type:String,function: ()->Unit) {
        ReportDialogFragment.newInstance(callerId,currentId, type,channelName,function = function,fppDialogFlag)
            .show(supportFragmentManager, "ReportDialogFragment")

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
                    val apiResponse =
                        AppObjectController.p2pNetworkService.p2pCallFeedbackV2(requestParams)
                    if(p2pCallShareControl) startShareActivity(apiResponse)
                    WorkManagerAdmin.syncFavoriteCaller()
                    delay(250)
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
                when (response) {
                    "YES" -> {
                        //showToast("$callerName is now added to your Favorite Practice Partners.")
                        closeActivity()


                    }
                    "NO" -> {
                        showReportDialog("BLOCK"){
                            closeActivity()
                        }

                        //showToast("$callerName is now added to your Blocklist.")
                    }
                    "MAYBE" -> {
                        //showToast("Thank you for submitting the feedback.")
                        closeActivity()

                    }
                    "CLOSED"-> {
                        closeActivity()
                    }
                    }
                }
        }
    }

     fun closeActivity(){
        finishAndRemoveTask()
    }

    private fun startShareActivity(apiResponse: Response<KFactor>) {
        if (apiResponse.isSuccessful &&
            apiResponse.code() in 201..203 && apiResponse.body()!!.duration_filter) {
            val body = apiResponse.body()!!

            val cState: String?
            val cCity: String?
            val rState: String?
            val rCity: String?

            if (yourAgoraId == body.caller.agora_mentor_id) {
                cState = body.caller.state
                cCity = body.caller.city
                rState = body.receiver.state
                rCity = body.receiver.city
            } else {
                cState = body.receiver.state
                cCity = body.receiver.city
                rState = body.caller.state
                rCity = body.caller.city
            }
            ShareWithFriendsActivity.startShareWithFriendsActivity(
                activity = this@VoipCallFeedbackActivity,
                receiverName = callerName,
                receiverImage = callerImage,
                minutesTalked = minute,
                callerState = cState,
                callerCity = cCity,
                receiverState = rState,
                receiverCity = rCity,
            )
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
            flags: Array<Int> = arrayOf(),
            callerId:Int,
            currentUserId:Int,
            fppDialogFlag:String
        ) {
            Log.e("Sagar", "callStatusNetworkApi: sagar $fppDialogFlag")

            Intent(activity, VoipCallFeedbackActivity::class.java).apply {
                putExtra(ARG_CHANNEL_NAME, channelName)
                putExtra(ARG_CALL_TIME, callTime)
                putExtra(ARG_CALLER_NAME, callerName)
                putExtra(ARG_CALLER_IMAGE, callerImage)
                putExtra(ARG_YOUR_NAME, yourName)
                putExtra(ARG_YOUR_AGORA_ID, yourAgoraId ?: -1)
                putExtra(ARG_DIM_BACKGROUND, dimBg)
                putExtra(ARG_CALLER_ID, callerId)
                putExtra(ARG_CURRENT_ID, currentUserId)
                putExtra(SHOW_FPP_DIALOG,fppDialogFlag)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }
}
