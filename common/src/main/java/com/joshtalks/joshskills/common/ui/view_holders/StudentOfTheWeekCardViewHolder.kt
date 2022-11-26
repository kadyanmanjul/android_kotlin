package com.joshtalks.joshskills.common.ui.view_holders

import android.graphics.Typeface
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenUserProfile
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.ref.WeakReference
import java.util.Locale


class StudentOfTheWeekCardViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    
    lateinit var userPic: CircleImageView

    
    lateinit var awardImage: AppCompatImageView

    
    lateinit var studentName: AppCompatTextView

    
    lateinit var totalPoints: AppCompatTextView

    
    lateinit var userText: AppCompatTextView

    
    lateinit var dateText: AppCompatTextView

    
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

    
    fun checkExamDetails() {
        message.awardMentorModel?.mentorId?.let {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it))
        }
    }
}