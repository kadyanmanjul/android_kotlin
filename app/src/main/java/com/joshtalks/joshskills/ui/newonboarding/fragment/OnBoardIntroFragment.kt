package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.ONBOARDING_VERSION_KEY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentOnBoardIntroBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.ui.newonboarding.adapter.OnBoardingIntroTextAdapter
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity

class OnBoardIntroFragment : Fragment() {
    var handler: Handler? = null
    var scrollingPosition = 0
    var width: Int = 0
    lateinit var binding: FragmentOnBoardIntroBinding
    private val viewModel: OnBoardViewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            OnBoardViewModel::class.java
        )
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        subscribeObserver()
    }

    private fun subscribeObserver() {
        viewModel.isGuestUserCreated.observe(requireActivity(), Observer { isGuestUserCreated ->
            if (isGuestUserCreated) {
                (requireActivity() as BaseActivity).apply {
                    when (getVersionData()?.version!!.name) {
                        ONBOARD_VERSIONS.ONBOARDING_V1 -> {
                            return@apply
                        }
                        ONBOARD_VERSIONS.ONBOARDING_V2 -> {
                            replaceFragment(
                                R.id.onboarding_container,
                                SelectCourseFragment.newInstance(),
                                SelectCourseFragment.TAG
                            )
                        }
                        ONBOARD_VERSIONS.ONBOARDING_V3, ONBOARD_VERSIONS.ONBOARDING_V4 -> {
                            replaceFragment(
                                R.id.onboarding_container,
                                SelectInterestFragment.newInstance(),
                                SelectInterestFragment.TAG
                            )

                        }
                    }
                }
            }
        })
    }

    private fun initView() {
        (requireActivity() as BaseActivity).getVersionData()?.let { versionData ->
            // Set buttonText
            if (versionData.version!!.name == ONBOARD_VERSIONS.ONBOARDING_V2)
                binding.startBtn.text = getString(R.string.start_your_7_day_trial)
            else
                binding.startBtn.text = getString(R.string.get_started)

            //Set Cover Image
            Utils.setImage(binding.scrollingIv, versionData.image)
            startImageScrolling()
            //Set up text viewpager
            versionData.content?.let {
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
                .push()
            viewModel.createGuestUser()
        }

        if ((requireActivity() as BaseActivity).isGuestUser() || Mentor.getInstance().hasId()
                .not()
        ) {
            binding.alreadySubscribed.setOnClickListener {
                val versionData=(requireActivity() as BaseActivity).getVersionData()
                versionData?.version?.let {
                    it.name=ONBOARD_VERSIONS.ONBOARDING_V1
                }
                versionData?.let {
                    PrefManager.put(
                        ONBOARDING_VERSION_KEY,
                        AppObjectController.gsonMapper.toJson(versionData)
                    )
                }
                AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_ALREADY_USER.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .push()

                val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
                    putExtra(FLOW_FROM, "NewOnBoardFlow journey")
                }
                startActivity(intent)
                requireActivity().finish()
            }
        } else binding.alreadySubscribed.visibility = View.GONE
    }

    private fun startImageScrolling() {
        handler = Handler()
        handler?.postDelayed({ /* Create an Intent that will start the MainActivity. */
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
        }, 20)
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
