package com.joshtalks.joshskills.ui.introduction

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.INTRODUCTION_IS_CONTINUE_CLICKED
import com.joshtalks.joshskills.core.INTRODUCTION_LAST_POSITION
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.IntroLayoutBinding
import com.joshtalks.joshskills.repository.server.introduction.DemoOnboardingData
import com.joshtalks.joshskills.ui.lesson.LessonViewModel

class IntroductionActivity : BaseActivity() {

    private lateinit var binding: IntroLayoutBinding
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
        lastPosition = PrefManager.getIntValue(INTRODUCTION_LAST_POSITION)
        binding.continueBtn.setOnClickListener {
            PrefManager.put(INTRODUCTION_IS_CONTINUE_CLICKED, true)
            showPaymentProcessingFragment()
        }
        binding.nextBtn.setOnClickListener {
            binding.lessonViewpager.currentItem = binding.lessonViewpager.currentItem.plus(1)
        }
        addObservers()
        showProgressBar()
        viewModel.getDemoOnBoardingData()
    }

    private fun addObservers() {

        viewModel.demoOnboardingData.observe(this, {
            it?.let { data ->
                initViewPager(data)
            }
        })

        viewModel.apiStatus.observe(this, {
            hideProgressBar()
        })
    }

    private fun initViewPager(data: DemoOnboardingData) {
        val adapter = IntroAdapter(
            data,
            supportFragmentManager, this.lifecycle
        )
        binding.fragmentContainer.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        binding.lessonViewpager.adapter = adapter
        binding.lessonViewpager.requestTransparentRegion(binding.lessonViewpager)
        binding.wormDotsIndicator.attachToPager(binding.lessonViewpager)
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
    }

    private fun showPaymentProcessingFragment() {
        ReadyForDemoClassActivity.startReadyForDemoActivity(
            this,
            arrayOf(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        )
    }
}
