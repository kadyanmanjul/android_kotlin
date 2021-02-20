package com.joshtalks.joshskills.ui.view_holders

import android.graphics.Typeface
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.ref.WeakReference
import java.util.Locale

@Layout(R.layout.layout_student_of_week_card)
class StudentOfTheWeekCardViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    @View(R.id.user_pic)
    lateinit var userPic: CircleImageView

    @View(R.id.iv_award)
    lateinit var awardImage: AppCompatImageView

    @View(R.id.student_name)
    lateinit var studentName: AppCompatTextView

    @View(R.id.total_points)
    lateinit var totalPoints: AppCompatTextView

    @View(R.id.user_text)
    lateinit var userText: AppCompatTextView

    @View(R.id.date_text)
    lateinit var dateText: AppCompatTextView

    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    private var userName: String = "Josh Skills"

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        message.awardMentorModel?.let { awardMentorModel ->
            val resp = StringBuilder()
            awardMentorModel.performerName?.split(" ")?.forEach {
                resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                    .append(" ")
            }
            studentName.text = resp
            awardMentorModel.totalPointsText?.let {
                totalPoints.text = HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
            }
            userText.text = awardMentorModel.description
            dateText.text = awardMentorModel.dateText

            dateText.setTypeface(dateText.typeface, Typeface.ITALIC)
            userPic.post {
                userPic.setUserImageOrInitials(
                    awardMentorModel.performerPhotoUrl,
                    awardMentorModel.performerName?.capitalize(Locale.getDefault()).toString(),
                    dpToPx = 28
                )
            }
            awardMentorModel.awardImageUrl?.let {
                awardImage.setImage(it, AppObjectController.joshApplication)
            }
        }
    }

    @Click(R.id.root_view_fl)
    fun checkExamDetails() {
        message.awardMentorModel?.mentorId?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
    }
}