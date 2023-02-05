package com.joshtalks.joshskills.premium.ui.course_details.viewholder

import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import android.widget.TextView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.premium.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.VERSION
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.repository.server.course_detail.CardType
import com.joshtalks.joshskills.premium.repository.server.course_detail.Feedback
import com.joshtalks.joshskills.premium.ui.video_player.VideoPlayerActivity
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import de.hdodenhof.circleimageview.CircleImageView


@Layout(R.layout.layout_listitem_story)
class StudentFeedbackCard(
    private var feedback: Feedback,
    private val context: Context = AppObjectController.joshApplication,
    private val testId: Int,
    private val coursePrice: String,
    private val courseName: String
) : CourseDetailsBaseCell(CardType.OTHER_INFO, 0) {

    @com.mindorks.placeholderview.annotations.View(R.id.name)
    lateinit var name: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.frameLayout)
    lateinit var frameLayout: FrameLayout

    @com.mindorks.placeholderview.annotations.View(R.id.profession)
    lateinit var profession: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.image_circle)
    lateinit var circleImage: CircleImageView

    @Resolve
    fun onResolved() {
        name.text = feedback.name
        profession.text = feedback.shortDescription
        feedback.thumbnailUrl?.let {
            setDefaultImageView(circleImage, feedback.thumbnailUrl!!)
        }
    }

    @Click(R.id.image_circle)
    fun onClick() {
        logAnalyticsEvent(feedback.name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            frameLayout.background.setTint(context.resources.getColor(R.color.dark_grey, null))
        }
        feedback.videoUrl?.let {
            VideoPlayerActivity.startVideoActivity(
                context,
                feedback.name,
                null,
                feedback.videoUrl
            )
        }

    }

    fun logAnalyticsEvent(name: String) {
        MixPanelTracker.publishEvent(MixPanelEvent.COURSE_MEET_STUDENTS)
            .addParam(ParamKeys.TEST_ID,testId)
            .addParam(ParamKeys.COURSE_NAME,courseName)
            .addParam(ParamKeys.COURSE_PRICE,coursePrice)
            .addParam(ParamKeys.STUDENT_NAME,name)
            .addParam(ParamKeys.COURSE_ID,
                PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
            .push()
        AppAnalytics.create(AnalyticsEvent.MEET_STUDENT_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.USER_NAME.NAME, name)
            .addParam(VERSION, PrefManager.getStringValue(VERSION))
            .push()
    }
}