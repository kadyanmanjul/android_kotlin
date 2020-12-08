package com.joshtalks.joshskills.ui.explore.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.ui.explore.CourseExploreAdapter
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kotlinx.android.synthetic.main.fragment_course_listing.offer_card_view
import kotlinx.android.synthetic.main.fragment_course_listing.recycler_view

class CourseListingFragmentV2 : Fragment() {
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
    ): View {
        return inflater.inflate(R.layout.fragment_course_listing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
    }

    private fun initRV() {
        recycler_view.itemAnimator?.apply {
            addDuration = 2000
            changeDuration = 2000
        }
        recycler_view.itemAnimator = SlideInUpAnimator(OvershootInterpolator(2f))
        recycler_view.layoutManager = SmoothLinearLayoutManager(context)
        recycler_view.setHasFixedSize(true)
        recycler_view.addItemDecoration(
            LayoutMarginDecoration(Utils.dpToPx(requireContext(), 6f))
        )

        val filterList = courseList.filter { it.cardType.ordinal == ExploreCardType.NORMAL.ordinal }
        val adapter = CourseExploreAdapter(filterList)
        recycler_view.adapter = adapter
        addOfferCard()
    }

    private fun addOfferCard() {
        val filterList = courseList.filter { it.cardType.ordinal != ExploreCardType.NORMAL.ordinal }
        if (filterList.isNullOrEmpty().not()) {
            filterList.forEach {
                offer_card_view.bind(it)
            }
        }
    }

    companion object {
        private const val ARG_COURSE_LIST_OBJ = "course-list-obj"

        @JvmStatic
        fun newInstance(conversationPractiseModel: List<CourseExploreModel>) =
            CourseListingFragmentV2().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(
                        ARG_COURSE_LIST_OBJ,
                        ArrayList(conversationPractiseModel)
                    )
                }
            }
    }
}