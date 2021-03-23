package com.joshtalks.joshskills.ui.introduction

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.DEMO_LESSON_NUMBER
import com.joshtalks.joshskills.core.DEMO_LESSON_TOPIC_ID
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.INTRODUCTION_START_NOW_CLICKED
import com.joshtalks.joshskills.core.INTRODUCTION_YES_EXCITED_CLICKED
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.ActivityReadyForDemoBinding
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class ReadyForDemoClassActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadyForDemoBinding
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(this).get(LessonViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        window.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.setOnSystemUiVisibilityChangeListener {
                if ((it and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    AppObjectController.uiHandler.postDelayed({
                        decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_IMMERSIVE
                    }, 500)

                }
            }
            statusBarColor = Color.TRANSPARENT
        }
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_ready_for_demo)
        binding.rootView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        binding.lifecycleOwner = this
        binding.handler = this
        addObservers()

    }

    private fun addObservers() {
        viewModel.lessonQuestionsLiveData.observe(this, {
            it.get(0).title
            val spQuestion = it.filter { it.chatType == CHAT_TYPE.SP }.getOrNull(0)
            spQuestion?.topicId?.let {
                PrefManager.put(DEMO_LESSON_TOPIC_ID, it)
                PrefManager.put(DEMO_LESSON_NUMBER, viewModel.demoLessonNoLiveData.value ?: 0)
                addRequesting(it)
            }
        })

        viewModel.apiStatus.observe(this, {
            //hideProgressBar()
        })
    }

    private fun addRequesting(topicId: String) {
        if (PermissionUtils.isDemoCallingPermissionEnabled(this)) {
            requestForSearchUser(topicId)
            return
        }

        PermissionUtils.demoCallingFeaturePermission(
            this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            requestForSearchUser(topicId)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(this@ReadyForDemoClassActivity)
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

    private fun requestForSearchUser(it: String) {
        startDemoSpeakingActivity(it)
        startActivity(
            DemoSearchingUserActivity.startUserForPractiseOnPhoneActivity(
                this,
                courseId = null,
                topicId = it.toInt(),
                topicName = EMPTY
            )
        )
        this.finish()
    }

    companion object {

        fun startReadyForDemoActivity(
            activity: Activity,
            flags: Array<Int> = arrayOf(),
        ) {
            Intent(activity, ReadyForDemoClassActivity::class.java).apply {
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }

    public fun startOnBoading() {
        val intent = Intent(this, SignUpActivity::class.java).apply {
            putExtra(FLOW_FROM, "new demo Onboarding flow")
        }
        startActivity(intent)
        this.finish()
    }

    public fun startDemoSpeakingClass() {
        PrefManager.put(INTRODUCTION_YES_EXCITED_CLICKED, true)
        PrefManager.put(IS_DEMO_P2P, true)
        if (PrefManager.getBoolValue(INTRODUCTION_START_NOW_CLICKED, false)) {
            DemoCourseDetailsActivity.startDemoCourseDetailsActivity(
                activity = this,
                testId = 201,
                startedFrom = "DemoCourseDetailsActivity",
                flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        } else {
            if (PrefManager.getStringValue(DEMO_LESSON_TOPIC_ID).isBlank()) {
                viewModel.getDemoLesson()
            } else {
                startDemoSpeakingActivity(
                    PrefManager.getStringValue(
                        DEMO_LESSON_TOPIC_ID
                    )
                )
            }
        }
    }

    private fun startDemoSpeakingActivity(topicId: String) {
        DemoSpeakingPractiseActivity.startDemoSpeakingActivity(
            this,
            topicId,
            viewModel.demoLessonNoLiveData.value ?: 0,
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
        )

        DemoSearchingUserActivity.startUserForPractiseOnPhoneActivity(
            this,
            courseId = EMPTY,
            topicId = topicId.toInt(),
            topicName = EMPTY
        )
    }

}
