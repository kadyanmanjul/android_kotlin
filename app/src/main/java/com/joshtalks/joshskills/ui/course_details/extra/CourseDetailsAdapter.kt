package com.joshtalks.joshskills.ui.course_details.extra

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.*
import com.joshtalks.joshskills.repository.server.course_detail.Card
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.ui.course_details.viewholder.*

class CourseDetailsAdapter(val activity: Activity, val testId: Int, val data: List<Card>) :
    RecyclerView.Adapter<DetailsBaseViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsBaseViewHolder {
        return when (viewType) {
            COURSE_OVERVIEW -> {
                val view = setViewHolder<CourseOverviewViewHolderBinding>(parent, R.layout.course_overview_view_holder)
                CourseOverviewViewHolder(view, testId)
            }
            LONG_DESCRIPTION -> {
                val view = setViewHolder<LayoutLongDescriptionCardViewHolderBinding>(
                    parent,
                    R.layout.layout_long_description_card_view_holder
                )
                LongDescriptionViewHolder(view)
            }
            TEACHER_DETAILS -> {
                val view = setViewHolder<TeacherDetailsViewHolderBinding>(parent, R.layout.teacher_details_view_holder)
                TeacherDetailsViewHolder(view)
            }
            SYLLABUS -> {
                val view = setViewHolder<LayoutSyllabusViewBinding>(parent, R.layout.layout_syllabus_view)
                SyllabusViewHolder(view)
            }
            GUIDELINES -> {
                val view = setViewHolder<GuidelineViewHolderBinding>(parent, R.layout.guideline_view_holder)
                GuidelineViewHolder(view, activity)
            }
            DEMO_LESSON -> {
                val view = setViewHolder<DemoLessonViewHolderBinding>(parent, R.layout.demo_lesson_view_holder)
                DemoLessonViewHolder(view)
            }
            REVIEWS -> {
                val view = setViewHolder<ReviewAndRatingLayoutBinding>(parent, R.layout.review_and_rating_layout)
                ReviewRatingViewHolder(view)
            }
            LOCATION_STATS -> {
                val view = setViewHolder<LayoutLocationStatsViewHolderBinding>(parent, R.layout.layout_location_stats_view_holder)
                LocationStatViewHolder(view, activity)
            }
            STUDENT_FEEDBACK -> {
                val view =
                    setViewHolder<LayoutStudentFeedbackViewholderBinding>(parent, R.layout.layout_student_feedback_viewholder)
                StudentFeedbackViewHolder(view, testId)
            }
            FAQ -> {
                val view = setViewHolder<LayoutExpandableViewHolderBinding>(parent, R.layout.layout_expandable_view_holder)
                MasterFaqViewHolder(view, testId)
            }
            ABOUT_JOSH -> {
                val view = setViewHolder<LayoutAboutJoshViewHolderBinding>(parent, R.layout.layout_about_josh_view_holder)
                AboutJoshViewHolder(view)
            }
            OTHER_INFO -> {
                val view = setViewHolder<OtherInfoViewHolderBinding>(parent, R.layout.other_info_view_holder)
                OtherInfoViewHolder(view)
            }
            else -> {
                val view = setViewHolder<OtherInfoViewHolderBinding>(parent, R.layout.other_info_view_holder)
                OtherInfoViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (data[position].cardType) {
            CardType.COURSE_OVERVIEW -> COURSE_OVERVIEW
            CardType.LONG_DESCRIPTION -> LONG_DESCRIPTION
            CardType.TEACHER_DETAILS -> TEACHER_DETAILS
            CardType.SYLLABUS -> SYLLABUS
            CardType.GUIDELINES -> GUIDELINES
            CardType.DEMO_LESSON -> DEMO_LESSON
            CardType.REVIEWS -> REVIEWS
            CardType.LOCATION_STATS -> LOCATION_STATS
            CardType.STUDENT_FEEDBACK -> STUDENT_FEEDBACK
            CardType.FAQ -> FAQ
            CardType.ABOUT_JOSH -> ABOUT_JOSH
            CardType.OTHER_INFO -> OTHER_INFO
            else -> -1
        }
    }

    override fun onBindViewHolder(holder: DetailsBaseViewHolder, position: Int) {
        holder.bindData(data[position].sequenceNumber, data[position].data)
    }

    override fun getItemCount() = data.size

    private fun <V : ViewDataBinding> setViewHolder(parent: ViewGroup, layoutId: Int): V {
        return DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            layoutId,
            parent,
            false
        )
    }

    companion object {
        private const val COURSE_OVERVIEW = 0
        private const val LONG_DESCRIPTION = 1
        private const val TEACHER_DETAILS = 2
        private const val SYLLABUS = 3
        private const val GUIDELINES = 4
        private const val DEMO_LESSON = 5
        private const val REVIEWS = 6
        private const val LOCATION_STATS = 7
        private const val STUDENT_FEEDBACK = 8
        private const val FAQ = 9
        private const val ABOUT_JOSH = 10
        private const val OTHER_INFO = 11
    }
}