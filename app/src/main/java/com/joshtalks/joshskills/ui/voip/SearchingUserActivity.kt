package com.joshtalks.joshskills.ui.voip

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ActivitySearchingUserBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.ui.voip.util.AudioPlayer
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import timber.log.Timber
import java.util.HashMap

const val COURSE_ID = "course_id"
const val TOPIC_ID = "topic_id"
const val TOPIC_NAME = "topic_name"

class SearchingUserActivity : BaseActivity() {
    companion object {
        fun startUserForPractiseOnPhoneActivity(
            activity: Activity,
            courseId: String,
            topicId: Int,
            topicName: String
        ): Intent {
            return Intent(activity, SearchingUserActivity::class.java).apply {
                putExtra(COURSE_ID, courseId)
                putExtra(TOPIC_ID, topicId)
                putExtra(TOPIC_NAME, topicName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    private var courseId: String? = null
    private var topicId: Int? = null
    private var topicName: String? = null
    private var timer: CountDownTimer? = null
    private lateinit var binding: ActivitySearchingUserBinding
    private var mBoundService: WebRtcService? = null
    private var appAnalytics: AppAnalytics? = null
    private var mServiceBound = false
    private val viewModel: VoipCallingViewModel by lazy {
        ViewModelProvider(this).get(VoipCallingViewModel::class.java)
    }
    private var outgoingCallData: HashMap<String, String?> = HashMap()

    private var myConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val myBinder = service as WebRtcService.MyBinder
            mBoundService = myBinder.getService()
            mServiceBound = true
            mBoundService?.addListener(callback)
            addRequesting()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBound = false
        }
    }

    private var callback: WebRtcCallback = object : WebRtcCallback {
        override fun onRinging() {
            Timber.tag("SearchingUserActivity").e("onRinging")
        }

        override fun onConnect() {
            Timber.tag("SearchingUserActivity").e("onConnect")
            WebRtcActivity.startOutgoingCallActivity(this@SearchingUserActivity, outgoingCallData)
            this@SearchingUserActivity.finish()
        }

        override fun onDisconnect(callId: String?) {
            Timber.tag("SearchingUserActivity").e("onDisconnect")
        }

        override fun onCallDisconnect(id: String?) {
            Timber.tag("SearchingUserActivity").e("onCallDisconnect")
            stopCalling()
        }

        override fun onCallReject(id: String?) {
            Timber.tag("SearchingUserActivity").e("onCallReject")
            outgoingCallData.clear()
            initApiForSearchUser()
        }

        override fun onSelfDisconnect(id: String?) {
            Timber.tag("SearchingUserActivity").e("onSelfDisconnect")
        }

        override fun onIncomingCallHangup(id: String?) {
            Timber.tag("SearchingUserActivity").e("onIncomingCallHangup")
        }
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
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_searching_user)
        binding.lifecycleOwner = this
        binding.handler = this
        courseId = intent.getStringExtra(COURSE_ID)
        topicId = intent.getIntExtra(TOPIC_ID, -1)
        topicName = intent.getStringExtra(TOPIC_NAME)
        appAnalytics = AppAnalytics.create(AnalyticsEvent.OPEN_CALL_SEARCH_SCREEN_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_ID.NAME, courseId)
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, "Conversation list")
        initView()
        addObserver()
    }

    private fun initView() {
        binding.progressBar.max = 100
        binding.progressBar.progress = 0
    }

    private fun addObserver() {
        viewModel.voipDetailsLiveData.observe(this, {
            if (it != null) {
                AppAnalytics.create(AnalyticsEvent.FIND_USER_FOR_P2P_CALL.NAME)
                    .addUserDetails()
                    .addParam(AnalyticsEvent.PLIVO_ID.NAME, it.plivoUserName)
                    .push()
                AudioPlayer.getInstance().playProgressTone()
                WebRtcService.startOutgoingCall(getMapForOutgoing(it))
            }
        })
        viewModel.apiCallStatusLiveData.observe(this, {
            if (ApiCallStatus.RETRY == it) {
                AppObjectController.uiHandler.postDelayed({
                    initApiForSearchUser()

                }, 1000)
            } else if (ApiCallStatus.FAILED_PERMANENT == it) {
                showToast("We did not find any user, please retry")
                stopCalling()
            }
        })
    }

    private fun addRequesting() {
        if (PermissionUtils.isCallingPermissionEnabled(this)) {
            requestForSearchUser()
            return
        }

        PermissionUtils.callingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            requestForSearchUser()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(this@SearchingUserActivity)
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }


    private fun startProgressBarCountDown() {
        binding.progressBar.max = 100
        binding.progressBar.progress = 0
        timer = object : CountDownTimer(5000, 500) {
            override fun onTick(millisUntilFinished: Long) {
                val diff = binding.progressBar.progress + 10
                fillProgressBar(diff)
            }

            override fun onFinish() {
                startProgressBarCountDown()
            }
        }
        timer?.start()
    }

    private fun fillProgressBar(diff: Int) {
        val animation: ObjectAnimator =
            ObjectAnimator.ofInt(
                binding.progressBar,
                "progress",
                binding.progressBar.progress,
                diff
            )
        animation.startDelay = 0
        animation.duration = 250
        animation.interpolator = AccelerateDecelerateInterpolator()
        animation.start()
    }

    private fun requestForSearchUser() {
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        appAnalytics?.addParam(AnalyticsEvent.SEARCH_USER_FOR_VOIP.NAME, courseId)
        startProgressBarCountDown()
        initApiForSearchUser()
    }

    private fun initApiForSearchUser() {
        courseId?.let {
            viewModel.getUserForTalk(it, topicId)
        }
    }

    fun stopCalling() {
        mBoundService?.endCall()
        AppAnalytics.create(AnalyticsEvent.STOP_USER_FOR_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        timer?.cancel()
        this.finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        appAnalytics?.addParam(AnalyticsEvent.BACK_PRESSED.NAME, true)
        appAnalytics?.push()
    }

    override fun onBackPressed() {
        stopCalling()
    }

    override fun onStart() {
        super.onStart()
        bindService(
            Intent(this, WebRtcService::class.java),
            myConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        AudioPlayer.getInstance().stopProgressTone()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
        unbindService(myConnection)
    }

    private fun getMapForOutgoing(
        voipCallDetailModel: VoipCallDetailModel?
    ): HashMap<String, String?> {
        if (outgoingCallData.isEmpty()) {
            voipCallDetailModel?.topic = topicId?.toString()
            voipCallDetailModel?.topicName = topicName
            voipCallDetailModel?.callieName = getCallieName()
            //   voipCallDetailModel?.plivoUserName = "514c55d8d96a40cb90b2ed193151019617540338749"
            outgoingCallData = LinkedHashMap()
            outgoingCallData.apply {
                put("X-PH-MOBILEUUID", voipCallDetailModel?.mobileUUID)
                put("X-PH-Destination", voipCallDetailModel?.plivoUserName)
                put("X-PH-TOPIC", topicId?.toString())
                put("X-PH-TOPICNAME", topicName)
                put("X-PH-CALLERNAME", getCallieName())
                put("X-PH-CALLIENAME", voipCallDetailModel?.name)
                put("X-PH-IMAGE_URL", voipCallDetailModel?.profilePic)
                put("X-PH-LOCALITY", voipCallDetailModel?.locality)
            }
        }
        return outgoingCallData
    }

    private fun getCallieName(): String {
        val name = Mentor.getInstance().getUser()?.firstName
        if (name.isNullOrEmpty()) {
            return "User"
        }
        return name
    }
}