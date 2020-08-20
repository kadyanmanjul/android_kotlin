package com.joshtalks.joshskills.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.databinding.FragmentCourseListingBinding
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.vanniktech.emoji.Utils
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator

class CourseListingFragment private constructor() : Fragment() {
    private lateinit var binding: FragmentCourseListingBinding
    private var courseList: ArrayList<CourseExploreModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            courseList =
                it.getParcelableArrayList<CourseExploreModel>(ARG_COURSE_LIST_OBJ) as ArrayList<CourseExploreModel>
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_course_listing,
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

        val filterList = courseList.filter { it.cardType.ordinal == ExploreCardType.NORMAL.ordinal }
        val adapter = CourseExploreAdapter(filterList)
        binding.recyclerView.adapter = adapter
        addOfferCard()
    }

    private fun addOfferCard() {
        val filterList = courseList.filter { it.cardType.ordinal != ExploreCardType.NORMAL.ordinal }
        if (filterList.isNullOrEmpty().not()) {
            filterList.forEach {
                binding.offerCardView.bind(it)
            }
        }
    }

    companion object {
        private const val ARG_COURSE_LIST_OBJ = "course-list-obj"

        @JvmStatic
        fun newInstance(conversationPractiseModel: List<CourseExploreModel>) =
            CourseListingFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(
                        ARG_COURSE_LIST_OBJ,
                        ArrayList(conversationPractiseModel)
                    )
                }
            }
    }
}