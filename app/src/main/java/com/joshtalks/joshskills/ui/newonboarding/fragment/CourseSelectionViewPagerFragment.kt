package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.IS_TRIAL_ENDED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.FragmentViewpagerCourseSelectionBinding
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.onboarding.ONBOARD_VERSIONS
import com.joshtalks.joshskills.ui.newonboarding.adapter.CourseSelectionAdapter
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator

class CourseSelectionViewPagerFragment : Fragment() {
    private lateinit var binding: FragmentViewpagerCourseSelectionBinding
    private lateinit var adapter: CourseSelectionAdapter
    private var courseList: ArrayList<CourseExploreModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseList =
                it.getParcelableArrayList<CourseExploreModel>(ARG_COURSE_LIST_OBJ) as ArrayList<CourseExploreModel>
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_viewpager_course_selection,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
    }

    private fun initRV() {
        binding.recyclerView.itemAnimator?.apply {
            addDuration = 2000
            changeDuration = 2000
        }
        binding.recyclerView.itemAnimator = SlideInUpAnimator(OvershootInterpolator(2f))
        binding.recyclerView.layoutManager = SmoothLinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(Utils.dpToPx(requireContext(), 6f))
        )
        var isSecondFlow = false
        (requireActivity() as BaseActivity).getVersionData()?.let {
            if (it.version!!.name == ONBOARD_VERSIONS.ONBOARDING_V3 ||
                PrefManager.getBoolValue(IS_TRIAL_ENDED, false)
            ) {
                isSecondFlow = true
            }
        }
        adapter = CourseSelectionAdapter(courseList, isSecondFlow)
        binding.recyclerView.adapter = adapter

    }

    companion object {
        private const val ARG_COURSE_LIST_OBJ = "course-select-list-obj"

        @JvmStatic
        fun newInstance(conversationPractiseModel: List<CourseExploreModel>) =
            CourseSelectionViewPagerFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(
                        ARG_COURSE_LIST_OBJ,
                        ArrayList(conversationPractiseModel)
                    )
                }
            }
    }
}