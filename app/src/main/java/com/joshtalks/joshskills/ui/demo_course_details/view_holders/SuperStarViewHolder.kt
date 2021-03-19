package com.joshtalks.joshskills.ui.demo_course_details.view_holders

import android.content.Context
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.SuperStarResponse
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseDetailsBaseCell
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator


@Layout(R.layout.layout_superstart_view)
class SuperStarViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var superStarResponse: SuperStarResponse,
    private val context: Context = AppObjectController.joshApplication,
    private val fragmentManager: FragmentManager

) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.student_rv)
    lateinit var recyclerView: RecyclerView

    @com.mindorks.placeholderview.annotations.View(R.id.worm_dots_indicator)
    lateinit var pageIndicator: ScrollingPagerIndicator


    @Resolve
    fun onResolved() {
        title.text = superStarResponse.title
        initRv()
        setFeedbackRV()
    }

    private fun initRv() {
        /*viewPager.adapter =
            superStarResponse.feedback_list?.let { SuperstarAdapter(fragmentManager, it) }
        pageIndicator.setViewPager(viewPager)
        viewPager.measure(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )*/

    }

    private fun setFeedbackRV() {
        if (recyclerView.adapter == null) {
            recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
                adapter = superStarResponse.feedback_list?.let { StudentFeedbackListAdapter(it) }
            }
        }
        pageIndicator.attachToRecyclerView(recyclerView)

    }
}
