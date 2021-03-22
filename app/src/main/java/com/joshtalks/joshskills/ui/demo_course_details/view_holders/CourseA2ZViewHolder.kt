package com.joshtalks.joshskills.ui.demo_course_details.view_holders

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenDemoLessonEventBus
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.CourseAToZResponse
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseDetailsBaseCell
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.layout_course_a_z_view)
class CourseA2ZViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var courseAToZResponse: CourseAToZResponse,
    private val context: Context = AppObjectController.joshApplication,
    private var isCompleted: Boolean

) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.description)
    lateinit var description: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.start_btn)
    lateinit var startBtn: MaterialTextView

    @com.mindorks.placeholderview.annotations.View(R.id.completed_btn)
    lateinit var completedBtn: MaterialTextView

    @Resolve
    fun onResolved() {
        description.text = HtmlCompat.fromHtml(
            courseAToZResponse.description.toString(),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        title.text = courseAToZResponse.title.toString()
        if (isCompleted) {
            startBtn.visibility = View.GONE
            completedBtn.visibility = View.VISIBLE
            title.text = courseAToZResponse.title.toString()
        } else {
            startBtn.visibility = View.VISIBLE
            completedBtn.visibility = View.GONE
            startBtn.text = courseAToZResponse.button_text.toString()
            title.text = courseAToZResponse.title.toString()
        }
    }

    @Click(R.id.start_btn)
    fun onClick() {
        RxBus2.publish(OpenDemoLessonEventBus(courseAToZResponse.lessonId))
    }

    public fun changeTextToCompleted(isLessonCompleted: Boolean) {
        isCompleted = isLessonCompleted
    }
}
