package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bekawestberg.loopinglayout.library.LoopingLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentOnBoardIntroBinding
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.newonboarding.adapter.OnBoardingIntroTextAdapter
import com.joshtalks.joshskills.ui.newonboarding.viewholder.CarouselImageViewHolder
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity

class OnBoardIntroFragment : Fragment() {
    var handler: Handler? = null
    var width: Int = 0
    private var delay = 100L
    lateinit var binding: FragmentOnBoardIntroBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_on_board_intro, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        VersionResponse.getInstance().image?.run {
            val mLayoutManager =
                LoopingLayoutManager(requireContext(), LoopingLayoutManager.HORIZONTAL, false)
            binding.recyclerView.builder
                .setHasFixedSize(true)
                .setLayoutManager(mLayoutManager)
            binding.recyclerView.addView(CarouselImageViewHolder(this))
            startImageScrolling()
            binding.recyclerView.setOnTouchListener { v, event ->
                if (event.action == ACTION_DOWN) {
                    handler?.removeCallbacksAndMessages(null)
                } else if (event.action == ACTION_UP) {
                    startImageScrolling()
                }
                return@setOnTouchListener false
            }

        }
    }

    private fun initView() {
        if (VersionResponse.getInstance().hasVersion()) {
            // Set buttonText
            if (VersionResponse.getInstance().version!!.name == ONBOARD_VERSIONS.ONBOARDING_V2)
                binding.startBtn.text = getString(R.string.start_your_7_day_trial)
            else
                binding.startBtn.text = getString(R.string.get_started)

            //Set Cover Image
            //Set up text viewpager
            VersionResponse.getInstance().content?.let {
                binding.viewPagerText.adapter = OnBoardingIntroTextAdapter(
                    requireActivity().supportFragmentManager,
                    it
                )
                binding.wormDotsIndicator.setViewPager(binding.viewPagerText)
            }
        }

        binding.startBtn.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_GET_STARTED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("version", VersionResponse.getInstance().version?.name.toString())
                .push()
            moveToNextScreen()
        }

        if ((requireActivity() as BaseActivity).isGuestUser()) {
            binding.alreadySubscribed.setOnClickListener {
                AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_ALREADY_USER.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam("version", VersionResponse.getInstance().version?.name.toString())
                    .push()

                val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
                    putExtra(FLOW_FROM, "NewOnBoardFlow journey")
                }
                startActivity(intent)
                requireActivity().finish()
            }
        } else binding.alreadySubscribed.visibility = View.GONE
    }

    private fun moveToNextScreen() {
        if (VersionResponse.getInstance().hasVersion()) {
            when (VersionResponse.getInstance().version!!.name) {
                ONBOARD_VERSIONS.ONBOARDING_V1, ONBOARD_VERSIONS.ONBOARDING_V7 -> {
                    return
                }
                ONBOARD_VERSIONS.ONBOARDING_V2 -> {
                    (requireActivity() as BaseActivity).replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseFragment.newInstance(),
                        SelectCourseFragment.TAG
                    )
                }
                ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4 -> {
                    (requireActivity() as BaseActivity).replaceFragment(
                        R.id.onboarding_container,
                        SelectInterestFragment.newInstance(),
                        SelectInterestFragment.TAG
                    )
                }
                ONBOARD_VERSIONS.ONBOARDING_V5 -> {
                    (requireActivity() as BaseActivity).replaceFragment(
                        R.id.onboarding_container,
                        SelectCourseHeadingFragment.newInstance(),
                        SelectCourseHeadingFragment.TAG
                    )
                }
            }
        }
    }

    private fun startImageScrolling() {
        handler = Handler()
        handler?.postDelayed(object : Runnable {
            override fun run() {
                binding.recyclerView.smoothScrollBy(20, 0)
                handler?.postDelayed(this, delay)
            }
        }, delay)
        /* handler?.postDelayed({ *//* Create an Intent that will start the MainActivity. *//*
            if (width == 0) {
                width = binding.scrollingIv.width
            } else {
                if (scrollingPosition + binding.scrollView.width >= width) {
                    scrollingPosition = 0
                    binding.scrollView.scrollTo(
                        0,
                        0
                    )
                }
                scrollingPosition += 5
                binding.scrollView.post {
                    binding.scrollView.smoothScrollTo(
                        scrollingPosition,
                        0
                    )
                }
            }
            startImageScrolling()
        }, 20)*/
    }

    companion object {
        const val TAG = "OnBoardingIntroFragment"

        @JvmStatic
        fun newInstance() =
            OnBoardIntroFragment()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
    }
}
