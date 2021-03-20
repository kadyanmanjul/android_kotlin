package com.joshtalks.joshskills.ui.introduction

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
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.DEMO_LESSON_NUMBER
import com.joshtalks.joshskills.core.DEMO_LESSON_TOPIC_ID
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.INTRODUCTION_IS_CONTINUE_CLICKED
import com.joshtalks.joshskills.core.INTRODUCTION_LAST_POSITION
import com.joshtalks.joshskills.core.INTRODUCTION_START_NOW_CLICKED
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.IntroLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.eventbus.StartDemoSpeakingLessonEventBus
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntroductionActivity : AppCompatActivity() {

    private lateinit var binding: IntroLayoutBinding
    private val compositeDisposable = CompositeDisposable()
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(this).get(LessonViewModel::class.java)
    }
    private var lastPosition: Int = 0

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
            DataBindingUtil.setContentView(this, R.layout.intro_layout)
        binding.lifecycleOwner = this
        binding.handler = this
        val adapter = IntroAdapter(
            supportFragmentManager, this.lifecycle
        )
        binding.fragmentContainer.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        binding.lessonViewpager.adapter = adapter
        binding.lessonViewpager.requestTransparentRegion(binding.lessonViewpager)
        binding.wormDotsIndicator.attachToPager(binding.lessonViewpager)
        lastPosition = PrefManager.getIntValue(INTRODUCTION_LAST_POSITION)
        binding.lessonViewpager.currentItem = lastPosition
        binding.lessonViewpager.setPageTransformer(MarginPageTransformer(Utils.dpToPx(40)))

        binding.lessonViewpager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                PrefManager.put(INTRODUCTION_LAST_POSITION, position)
                if (position >= 3) {
                    binding.continueBtn.visibility = View.VISIBLE
                    binding.nextBtn.visibility = View.GONE
                } else {
                    binding.continueBtn.visibility = View.GONE
                    binding.nextBtn.visibility = View.VISIBLE
                }
            }
        })
        binding.continueBtn.setOnClickListener {
            PrefManager.put(INTRODUCTION_IS_CONTINUE_CLICKED, true)
            showPaymentProcessingFragment()
        }
        binding.nextBtn.setOnClickListener {
            binding.lessonViewpager.currentItem = binding.lessonViewpager.currentItem.plus(1)
        }
        addObservers()
        if (PrefManager.getBoolValue(
                INTRODUCTION_IS_CONTINUE_CLICKED,
                defValue = false
            ) || PrefManager.getBoolValue(INTRODUCTION_START_NOW_CLICKED)
        )
            showPaymentProcessingFragment()
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
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(this@IntroductionActivity)
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

    private fun subscribeRxBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(StartDemoSpeakingLessonEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
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
                })

    }

    private fun insertDemoLessoninDb(lessonModel: LessonModel) {
        CoroutineScope(Dispatchers.IO).launch {
            lessonModel.chatId = ""
            AppObjectController.appDatabase.lessonDao().insertSingleItem(lessonModel)
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeRxBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    private fun showPaymentProcessingFragment() {
        binding.container.visibility = View.GONE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_container,
                ReadyForDemoClassFragment.newInstance(),
                "Ready For Demo Class Fragment"
            )
            .commitAllowingStateLoss()
    }

    private fun startDemoSpeakingActivity(topicId: String) {
        binding.container.visibility = View.GONE
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

    enum class Content(val text: String, val drawable: Int) {
        FIRST(
            "रोज़ाना इंग्लिश बोलो और " +
                    "अपना माहौल खुद बनाओ", R.drawable.intro_page1
        ),
        SECOND(
            "नए शब्द सीखो और उनका एस्टमाल करके तरक़्क़ी करो",
            R.drawable.intro_page2
        ),
        THIRD(
            "सही कान्सेप्ट्स समझो अपने बड़े भैया Vedant Sir से",
            R.drawable.intro_page3
        ),
        FOURTH(
            "प्रैक्टिस करके अपने माता पिता का नाम रोशन करो और बनो \n" +
                    "Student of the Month",
            R.drawable.intro_page4
        )
    }
}