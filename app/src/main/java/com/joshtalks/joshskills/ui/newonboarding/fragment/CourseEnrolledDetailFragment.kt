package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SUBSCRIPTION_TEST_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentCourseEnrolledDetailBinding
import com.joshtalks.joshskills.repository.server.onboarding.CourseContent
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.newonboarding.adapter.CourseEnrolledDetailAdapter
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel

class CourseEnrolledDetailFragment : Fragment() {
    lateinit var binding: FragmentCourseEnrolledDetailBinding
    var headingIds: ArrayList<Int> = ArrayList()
    var adapter: CourseEnrolledDetailAdapter = CourseEnrolledDetailAdapter()
    private val viewModel: OnBoardViewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            OnBoardViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            headingIds = it.getIntegerArrayList(HEADING_IDS) as ArrayList<Int>
        }
        subscribeObserver()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_course_enrolled_detail,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        viewModel.getEnrolledCoursesDetails(headingIds)
    }

    private fun subscribeObserver() {
        viewModel.courseEnrolledDetailLiveData.observe(requireActivity(), Observer { data ->
            data?.let { courseEnrolledResponse ->
                adapter.setContent(courseEnrolledResponse.contentList as ArrayList<CourseContent>)
                binding.desc.text = courseEnrolledResponse.text
                binding.title.text = courseEnrolledResponse.description
            }
        })

        viewModel.isEnrolled.observe(requireActivity(), Observer { data ->
            if (data) {
                moveToInboxScreen()
            }
        })
    }

    private fun initView() {
        binding.viewPagerText.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.viewPagerText.isUserInputEnabled = true
        adapter = CourseEnrolledDetailAdapter()
        binding.viewPagerText.adapter = adapter
        binding.viewPagerText.offscreenPageLimit = 10
        binding.wormDotsIndicator.setViewPager2(binding.viewPagerText)

        binding.viewPagerText.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            // override desired callback functions
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                logEventOnPageScrolled(position)
            }
        })

        binding.btnBuy.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_V5_BUY_ACCESS_PASS.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("version", VersionResponse.getInstance().version?.name.toString())
                .addParam("no of courses enrolling", headingIds.size)
                .push()
            navigateToCourseDetailsScreen(
                PrefManager.getIntValue(SUBSCRIPTION_TEST_ID),
                false,
                null
            )
        }
        binding.btnStart.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_V5_START_COURSE_FREE.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("version", VersionResponse.getInstance().version?.name.toString())
                .addParam("no of courses enrolling", headingIds.size)
                .push()
            viewModel.courseEnrolledDetailLiveData.value?.testIds?.let { testIds ->
                viewModel.enrollMentorAgainstTest(
                    testIds
                )
            }
        }
        binding.helpBtn.setOnClickListener {
            (requireActivity() as BaseActivity).openHelpActivity()
        }
    }

    private fun logEventOnPageScrolled(position :Int) {
        AppAnalytics.create(AnalyticsEvent.COURSE_SUGGEST_SCROLL.name)
            .addBasicParam()
            .addUserDetails()
            .addParam("version", VersionResponse.getInstance().version?.name.toString())
            .addParam("no of courses enrolling", headingIds.size)
            .addParam("page position",position)
            .push()
    }

    private fun navigateToCourseDetailsScreen(
        testId: Int,
        haveCourses: Boolean = false,
        whatsappLink: String? = null
    ) {
        CourseDetailsActivity.startCourseDetailsActivity(
            activity = requireActivity(),
            testId = testId,
            startedFrom = TAG,
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            isFromFreeTrial = haveCourses,
            whatsappUrl = whatsappLink,
            buySubscription = true
        )
    }

    private fun moveToInboxScreen() {
        startActivity((requireActivity() as BaseActivity).getInboxActivityIntent(true))
        requireActivity().finish()
    }


    companion object {
        const val TAG = "CourseEnrolledDetailFragment"
        const val HEADING_IDS = "heading_ids"

        @JvmStatic
        fun newInstance(headingIds: ArrayList<Int>) =
            CourseEnrolledDetailFragment()
                .apply {
                    arguments = Bundle().apply {
                        putIntegerArrayList(HEADING_IDS, headingIds)
                    }
                }
    }
}
