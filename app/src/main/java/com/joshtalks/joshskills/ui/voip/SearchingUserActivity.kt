package com.joshtalks.joshskills.ui.voip

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.interfaces.OnDismissWithDialog
import com.joshtalks.joshskills.databinding.ActivitySearchingUserBinding
import com.joshtalks.joshskills.ui.voip.extra.CallingStartMediatorDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

const val COURSE_ID = "course_id"

class SearchingUserActivity : BaseActivity(), OnDismissWithDialog {

    private var courseId: String? = null
    private var timer: CountDownTimer? = null
    private lateinit var binding: ActivitySearchingUserBinding
    private var canTimerComplete = false
    private val viewModel: VoipCallingViewModel by lazy {
        ViewModelProvider(this).get(VoipCallingViewModel::class.java)
    }
    private var appAnalytics: AppAnalytics?=null


    companion object {
        fun startUserForPractiseOnPhoneActivity(activity: Activity, courseId: String) {
            Intent(activity, SearchingUserActivity::class.java).apply {
                putExtra(COURSE_ID, courseId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }.run {
                activity.startActivity(this)
            }
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
        appAnalytics=AppAnalytics.create(AnalyticsEvent.OPEN_CALL_SEARCH_SCREEN_VOIP.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_ID.NAME,courseId)
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, "Conversation list")
        initView()
        addObserver()
        addRequesting()

    }

    private fun initView() {
        binding.progressBar.max = 100
        binding.progressBar.progress = 0
    }

    private fun addObserver() {
        viewModel.voipDetailsLiveData.observe(this, {
            if (it == null) {
                timer?.cancel()
                fillProgressBar(0)
            }
            if (it != null && canTimerComplete) {
                startMediatorDialog()
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
                if (viewModel.voipDetailsLiveData.value != null) {
                    fillProgressBar(100)
                    startMediatorDialog()
                } else {
                    canTimerComplete = true
                }
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
        appAnalytics?.addParam(AnalyticsEvent.SEARCH_USER_FOR_VOIP.NAME,courseId)
        courseId?.let {
            startProgressBarCountDown()
            viewModel.getUserForTalk(it)
        }
    }

    fun stopCalling() {
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

    override fun onSuccessDismiss() {
        if (viewModel.voipDetailsLiveData.value == null) {
            addRequesting()
        } else {
            viewModel.voipDetailsLiveData.value?.let {
                AppAnalytics.create(AnalyticsEvent.START_CALL_USER_FOR_VOIP.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .push()
                viewModel.voipDetailsLiveData.postValue(null)
                WebRtcActivity.startVoipActivity(this, it)
            }
        }
    }

    override fun onCancel() {
        this.finish()
    }

    override fun onDismiss() {
        viewModel.voipDetailsLiveData.postValue(null)
        AppObjectController.uiHandler.postDelayed({
            requestForSearchUser()
        }, 500)

    }

    private fun startMediatorDialog() {
        if (isFinishing.not()) {
            CallingStartMediatorDialog.newInstance()
                .show(supportFragmentManager, "calling_start_mediator_dialog")
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        binding.progressBar.progress = 0
        AppObjectController.uiHandler.postDelayed({
            addRequesting()
        }, 2000)
    }

}
