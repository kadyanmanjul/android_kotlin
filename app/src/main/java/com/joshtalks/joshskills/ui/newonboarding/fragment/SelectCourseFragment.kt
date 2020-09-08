package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.content.Intent
import android.content.res.ColorStateList
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
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_GUEST_ENROLLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.FragmentCourseSelectionBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.CourseSelectedEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.ui.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.ui.newonboarding.adapter.CourseSelectionViewPageAdapter
import com.joshtalks.joshskills.ui.newonboarding.viewmodel.OnBoardViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.jetbrains.anko.textColor

class SelectCourseFragment() : Fragment() {

    private lateinit var binding: FragmentCourseSelectionBinding
    private var courseList: ArrayList<InboxEntity>? = null
    private var categoryList: HashMap<Int, ArrayList<CourseExploreModel>> = HashMap()
    private val tabName: MutableList<String> = ArrayList()
    private lateinit var viewModel: OnBoardViewModel
    private var compositeDisposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseList = it.getParcelable(USER_COURSES_LIST)!!
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
        initView()
        subscribeObservers()
    }

    private fun subscribeObservers() {
        viewModel.courseListLiveData.observe(requireActivity(), { response ->

            response.forEach { courseExploreModel ->
                courseExploreModel.isClickable = false
                courseExploreModel.categoryIds?.forEach {
                    var courseList = categoryList[it]
                    if (courseList == null) {
                        courseList = ArrayList()
                    }
                    courseList.add(courseExploreModel)
                    categoryList[it] = courseList
                }
            }



            (requireActivity() as BaseActivity).getVersionData()?.courseCategories?.forEach {
                categoryList.keys.iterator().forEach { category_id ->
                    if (it.id == category_id) {
                        tabName.add(it.name!!)
                        return@forEach
                    }
                }
            }

            AppObjectController.uiHandler.post {
                binding.courseListingRv.adapter =
                    CourseSelectionViewPageAdapter(
                        this@SelectCourseFragment,
                        categoryList
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
            navigateToCourseDetailsScreen(
                AppObjectController.getFirebaseRemoteConfig()
                    .getDouble(FirebaseRemoteConfigKey.SUBSCRIPTION_TEST_ID).toInt()
            )
        }
    }

    private fun navigateToCourseDetailsScreen(testId: Int) {
        CourseDetailsActivity.startCourseDetailsActivity(
            requireActivity(),
            testId,
            requireActivity().javaClass.simpleName,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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

            when ((requireActivity() as BaseActivity).getVersionData()?.version?.name) {
                ONBOARD_VERSIONS.ONBOARDING_V2, ONBOARD_VERSIONS.ONBOARDING_V3 -> {
                    binding.startTrialContainer.visibility = View.GONE
                }
                ONBOARD_VERSIONS.ONBOARDING_V4 -> {
                    setSelectedCourse(0)
                }
                else -> {
                    setSelectedCourse(0)
                }
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
                binding.courses.textColor = ContextCompat.getColor(requireActivity(), R.color.grey)
            } else {
                binding.courses.textColor = ContextCompat.getColor(requireActivity(), R.color.black)
            }
            if (count >= it.minimumNumberOfInterests!!) {
                enabledSubmitButton()
            } else {
                disableSubmitButton()
            }
        }
    }

    private fun enabledSubmitButton() {
        binding.btnStartCourse.isEnabled = true
        binding.btnStartCourse.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                R.color.button_primary_color
            )
        )
    }

    private fun disableSubmitButton() {
        binding.btnStartCourse.isEnabled = false
        binding.btnStartCourse.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                requireContext(),
                R.color.transparent_black
            )
        )
    }

    fun registerCourses() {
        val testIds = ArrayList<Int>()
        viewModel.getCourseList()?.let {
            it.forEach { course ->
                if (course.isClickable) {
                    testIds.add(course.id!!)
                }
            }
        }
        if (testIds.isNullOrEmpty()) {
            return
        }
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
                    if (it.isAlreadyEnrolled && it.id != null) {
                        navigateToCourseDetailsScreen(it.id)
                    } else {
                        var count = 0
                        viewModel.getCourseList()?.let {
                            it.forEach { course ->
                                if (course.isClickable)
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
        const val USER_COURSES_LIST = "user_courses_list"

        @JvmStatic
        fun newInstance() =
            SelectCourseFragment()
    }
}