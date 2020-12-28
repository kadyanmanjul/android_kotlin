package com.joshtalks.joshskills.ui.voip

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.ActivitySearchingUserBinding
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

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
    private var uiHandler: Handler? = null
    private var compositeDisposable = CompositeDisposable()


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
        override fun onConnect(connectId: String) {
            Timber.tag("SearchingUserActivity").e("onConnect")
            outgoingCallData[RTC_CALLER_UID_KEY] = connectId
            WebRtcActivity.startOutgoingCallActivity(this@SearchingUserActivity, outgoingCallData)
            this@SearchingUserActivity.finish()
        }

        override fun switchChannel(data: HashMap<String, String?>) {
            val callActivityIntent =
                Intent(this@SearchingUserActivity, WebRtcActivity::class.java).apply {
                    putExtra(CALL_TYPE, CallType.INCOMING)
                    putExtra(AUTO_PICKUP_CALL, true)
                    putExtra(CALL_USER_OBJ, data)
                }
            startActivity(callActivityIntent)
            this@SearchingUserActivity.finish()
        }

        override fun onNoUserFound() {
            showToast(getString(R.string.did_not_answer_message))
            stopCalling()
            this@SearchingUserActivity.finishAndRemoveTask()
        }

        override fun onChannelJoin() {
            super.onChannelJoin()
            uiHandler?.postDelayed({
                if (isFinishing.not()) {
                    binding.btnAction.visibility = View.VISIBLE
                }
            }, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WebRtcService.initLibrary()
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
        viewModel.apiCallStatusLiveData.observe(this, {
            if (ApiCallStatus.FAILED == it || ApiCallStatus.FAILED_PERMANENT == it) {
                showToast(getString(R.string.did_not_answer_message))
                finishAndRemoveTask()
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
        appAnalytics?.addParam(AnalyticsEvent.SEARCH_USER_FOR_VOIP.NAME, courseId)
        startProgressBarCountDown()
        initApiForSearchUser()
    }

    private fun initApiForSearchUser() {
        courseId?.let {
            viewModel.getUserForTalk(it, topicId, ::callback)
        }
    }

    private fun callback(token: String, channelName: String, uid: Int) {
        ifDidNotFindActiveUser()
        WebRtcService.startOutgoingCall(getMapForOutgoing(token, channelName, uid))
    }

    fun stopCalling() {
        mBoundService?.endCall(apiCall = true)
        AppAnalytics.create(AnalyticsEvent.STOP_USER_FOR_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
        timer?.cancel()
        finishAndRemoveTask()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
            stopCalling()
        }
        return super.onKeyDown(keyCode, event)
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
        uiHandler = Handler(Looper.getMainLooper())
        bindService(
            Intent(this, WebRtcService::class.java),
            myConnection,
            BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        uiHandler?.removeCallbacksAndMessages(null)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        unbindService(myConnection)
    }

    private fun getMapForOutgoing(
        token: String,
        channelName: String,
        uid: Int
    ): HashMap<String, String?> {
        if (outgoingCallData.isEmpty()) {
            outgoingCallData = LinkedHashMap()
            outgoingCallData.apply {
                put(RTC_TOKEN_KEY, token)
                put(RTC_CHANNEL_KEY, channelName)
                put(RTC_UID_KEY, uid.toString())
            }
        }
        return outgoingCallData
    }

    private fun ifDidNotFindActiveUser() {
        compositeDisposable.add(
            Completable.complete()
                .delay(2, TimeUnit.MINUTES)
                .doOnComplete {
                    mBoundService?.isCallNotConnected()?.let {
                        if (it.not()) {
                            WebRtcService.noUserFoundCallDisconnect()
                        }
                    }
                }
                .subscribe())
    }
}