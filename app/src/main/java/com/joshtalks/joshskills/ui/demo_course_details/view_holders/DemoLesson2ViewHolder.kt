package com.joshtalks.joshskills.ui.demo_course_details.view_holders

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenDemoLessonEventBus
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.DemoLesson2Response
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseDetailsBaseCell
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.layout_start_demo_card_view)
class DemoLesson2ViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var demoLesson2Response: DemoLesson2Response,
    private val context: Context = AppObjectController.joshApplication,
    private var isCompleted: Boolean
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.start_btn)
    lateinit var startBtn: MaterialTextView

    @com.mindorks.placeholderview.annotations.View(R.id.completed_btn)
    lateinit var completedBtn: MaterialTextView

    @Resolve
    fun onResolved() {
        if (isCompleted) {
            startBtn.visibility = View.GONE
            completedBtn.visibility = View.VISIBLE
            title.text = demoLesson2Response.title.toString()
        } else {
            startBtn.visibility = View.VISIBLE
            completedBtn.visibility = View.GONE
            startBtn.text = demoLesson2Response.button_text.toString()
            title.text = demoLesson2Response.title.toString()
        }
    }

    @Click(R.id.start_btn)
    fun onClick() {
        RxBus2.publish(OpenDemoLessonEventBus(demoLesson2Response?.lessonId))
    }

    public fun changeTextToCompleted(isLessonCompleted: Boolean) {
        isCompleted = isLessonCompleted
    }
}
