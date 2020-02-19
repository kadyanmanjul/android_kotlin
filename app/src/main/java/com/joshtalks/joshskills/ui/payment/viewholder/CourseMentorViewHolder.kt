package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.course_detail.CourseMentor
import com.joshtalks.joshskills.ui.view_holders.BaseCell
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView

@Layout(R.layout.course_mentor_layout)
class CourseMentorViewHolder(
    private val courseMentor: CourseMentor,
    val context: Context = AppObjectController.joshApplication
) : BaseCell() {

    @View(R.id.iv_mentor)
    lateinit var ivMentor: CircleImageView

    @View(R.id.tv_name)
    lateinit var name: JoshTextView


    @View(R.id.tv_where)
    lateinit var tvWhere: JoshTextView

    @View(R.id.tv_qualification_details)
    lateinit var qualificationDetail: JoshTextView


    @Resolve
    fun onViewInflated() {
        setDefaultImageView(ivMentor, courseMentor.imageUrl)
        name.text = courseMentor.name
        tvWhere.text = courseMentor.from
        qualificationDetail.text = courseMentor.qualificationDetails

    }
}