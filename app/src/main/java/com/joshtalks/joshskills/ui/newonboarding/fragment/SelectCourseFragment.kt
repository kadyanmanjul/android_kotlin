package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentCourseSelectionBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CourseSelectedEventBus
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.newonboarding.adapter.CourseSelectionViewPageAdapter
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.jetbrains.anko.textColor

class SelectCourseFragment : Fragment() {

    private lateinit var binding: FragmentCourseSelectionBinding
    private var haveCourses = false
    private var version = EMPTY
    private var tabName: MutableList<String> = ArrayList()
    private lateinit var viewModel: OnBoardViewModel
    private var compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            haveCourses = it.getBoolean(HAVE_COURSES, false) || PrefManager.getBoolValue(
                IS_TRIAL_ENDED, false
            )
        }
        viewModel = ViewModelProvider(requireActivity()).get(OnBoardViewModel::class.java)
        viewModel.getCourses()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_course_selection, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        version =
            (requireActivity() as BaseActivity).getVersionData()?.version?.name?.type.toString()
        initView()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.courseListLiveData.observe(requireActivity(), { response ->
            val categoryList: HashMap<Int, ArrayList<CourseExploreModel>> = HashMap()

            response.forEach { courseExploreModel ->
                courseExploreModel.categoryIds?.forEach {
                    var courseList = categoryList[it]
                    if (courseList == null) {
                        courseList = ArrayList()
                    }
                    courseList.add(courseExploreModel)
                    categoryList[it] = courseList
                }
            }
            if (categoryList.size == 0) {
                binding.noCourseLayout.visibility = View.VISIBLE
                binding.startTrialContainer.visibility = View.GONE
                return@observe
            }
            val courseMapByCategoryName: HashMap<String, ArrayList<CourseExploreModel>> = HashMap()

            (requireActivity() as BaseActivity).getVersionData()?.courseCategories?.forEach {
                categoryList.keys.forEach innerLoop@{ category_id ->
                    if (it.id == category_id) {
                        courseMapByCategoryName[it.name!!] = categoryList[category_id]!!
                        tabName.add(it.name!!)
                        return@innerLoop
                    }
                }
            }

            AppObjectController.uiHandler.post {
                binding.courseListingRv.adapter =
                    CourseSelectionViewPageAdapter(
                        this@SelectCourseFragment, tabName,
                        courseMapByCategoryName
                    )
                initViewPagerTab()
            }
        })

        viewModel.apiCallStatusLiveData.observe(requireActivity(), { response ->
            AppObjectController.uiHandler.post {
                when (response) {
                    ApiCallStatus.SUCCESS -> {
                        binding.progressBar.visibility = View.GONE
                        showMoveToInboxScreen()
                    }
                    ApiCallStatus.START -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        })

        binding.upgrade.setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_UPGRADE_CLICKED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam("is_already-enrolled", PrefManager.getBoolValue(IS_GUEST_ENROLLED))
                .addParam("version", version)
                .push()
            PaymentSummaryActivity.startPaymentSummaryActivity(
                requireActivity(), AppObjectController.getFirebaseRemoteConfig()
                    .getDouble(FirebaseRemoteConfigKey.SUBSCRIPTION_TEST_ID).toInt().toString()
            )
        }
    }

    private fun navigateToCourseDetailsScreen(testId: Int, haveCourses: Boolean = false) {
        CourseDetailsActivity.startCourseDetailsActivity(
            activity = requireActivity(),
            testId = testId,
            startedFrom = requireActivity().javaClass.simpleName,
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            isFromFreeTrial = haveCourses
        )
    }

    private fun showMoveToInboxScreen() {
        startActivity((activity as BaseActivity).getInboxActivityIntent())
        requireActivity().finish()
    }

    private fun initViewPagerTab() {
        TabLayoutMediator(
            binding.tabLayout, binding.courseListingRv
        ) { tab, position ->
            tab.text = tabName[position]
        }.attach()
        val tabs = binding.tabLayout.getChildAt(0) as ViewGroup
        val layoutParam: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        for (i in 0 until tabs.childCount) {
            val tab = tabs.getChildAt(i)
            tab.layoutParams = layoutParam
            val layoutParams = tab.layoutParams as LinearLayout.LayoutParams

            layoutParams.weight = 0f
            layoutParams.marginEnd = Utils.dpToPx(4)
            layoutParams.marginStart = Utils.dpToPx(4)
            tab.layoutParams = layoutParams
            binding.tabLayout.requestLayout()
        }

        binding.courseListingRv.offscreenPageLimit = 1
    }

    private fun initView() {
        if (PrefManager.getBoolValue(IS_GUEST_ENROLLED, false)) {
            binding.titleTv.text = getString(R.string.explorer_courses)

            if (PrefManager.getBoolValue(
                    IS_TRIAL_ENDED,
                    false
                ) || (requireActivity() as BaseActivity).getVersionData()?.version?.name == ONBOARD_VERSIONS.ONBOARDING_V3 ||
                PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED)
            ) {
                binding.startTrialContainer.visibility = View.GONE

            } else {
                setSelectedCourse(0)
            }
        } else {
            binding.titleTv.text = getString(R.string.select_courses)
            setSelectedCourse(0)
        }

        binding.toolbar.inflateMenu(R.menu.logout_menu)

        val item: MenuItem = binding.toolbar.menu.findItem(R.id.menu_logout)
        item.title = getString(R.string.help)

        binding.toolbar.setOnMenuItemClickListener {
            if (it?.itemId == R.id.menu_logout) {
                (requireActivity() as BaseActivity).openHelpActivity()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun setSelectedCourse(count: Int) {
        val string = SpannableStringBuilder(
            getString(
                R.string.course_selected,
                count.toString()
            )
        )
        binding.courses.text = string
        (requireActivity() as BaseActivity).getVersionData()?.let {
            if (count == 0) {
                binding.courses.textColor =
                    ContextCompat.getColor(requireActivity(), R.color.light_grey)
            } else {
                binding.courses.textColor = ContextCompat.getColor(requireActivity(), R.color.black)
            }
            if (count >= 1) {
                enabledSubmitButton()
            } else {
                disableSubmitButton()
            }
        }
    }

    private fun enabledSubmitButton() {
        binding.btnStartCourse.isEnabled = true
    }

    private fun disableSubmitButton() {
        binding.btnStartCourse.isEnabled = false
    }

    fun registerCourses() {
        val testIds = ArrayList<Int>()
        viewModel.getCourseList()?.let {
            it.forEach { course ->
                if (course.isSelected) {
                    testIds.add(course.id!!)
                }
            }
        }
        if (testIds.isNullOrEmpty()) {
            return
        }
        AppAnalytics.create(AnalyticsEvent.NEW_ONBOARDING_START_LEARNING.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("With no of course", testIds.size)
            .addParam("is_already-enrolled", PrefManager.getBoolValue(IS_GUEST_ENROLLED))
            .addParam("version", version)
            .push()

        viewModel.enrollMentorAgainstTest(testIds)
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(CourseSelectedEventBus::class.java)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if ((it.isAlreadyEnrolled && it.id != null) || PrefManager.getBoolValue(
                            IS_SUBSCRIPTION_STARTED
                        )
                    ) {
                        it.id?.let { id ->
                            navigateToCourseDetailsScreen(id, true)
                        }
                    } else {
                        var count = 0
                        viewModel.getCourseList()?.let { courseList ->
                            courseList.forEach { course ->
                                if (course.isSelected)
                                    count += 1
                            }
                        }
                        setSelectedCourse(count)

                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    companion object {
        const val TAG = "SelectInterestFragment"
        const val HAVE_COURSES = "have_courses"

        @JvmStatic
        fun newInstance(haveCourses: Boolean = false) =
            SelectCourseFragment()
                .apply {
                    arguments = Bundle().apply {
                        putBoolean(HAVE_COURSES, haveCourses)
                    }
                }
    }
}
