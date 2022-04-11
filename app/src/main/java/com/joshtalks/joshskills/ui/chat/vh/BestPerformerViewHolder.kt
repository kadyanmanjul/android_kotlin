package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.ui.userprofile.models.Award
import java.util.*

class BestPerformerViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private val studentOfDash: AppCompatTextView = view.findViewById(R.id.tv_student_of)
    private val userPic: AppCompatImageView = view.findViewById(R.id.user_pic)
    private val awardImage: AppCompatImageView = view.findViewById(R.id.iv_award)
    private val studentName: AppCompatTextView = view.findViewById(R.id.student_name)
    private val totalPoints: AppCompatTextView = view.findViewById(R.id.total_points)
    private val userText: AppCompatTextView = view.findViewById(R.id.user_text)
    private val rootView: FrameLayout = view.findViewById(R.id.root_view_fl)
    private var message: ChatModel? = null

    init {
        rootView.also {
            it.setOnClickListener {
                message?.awardMentorModel?.mentorId?.let {
                    RxBus2.publish(OpenUserProfile(it))
                }
            }
        }
    }

    override fun bind(message: ChatModel, previousSender: ChatModel?) {
        this.message = message
        message.awardMentorModel?.let { awardMentorModel ->

            val resp = StringBuilder()
            awardMentorModel.performerName?.split(" ")?.forEach {
                resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                    .append(" ")
            }
            studentName.text = resp
            totalPoints.text = awardMentorModel.totalPointsText
            userText.text = awardMentorModel.description
            studentOfDash.text = awardMentorModel.awardText
            userPic.post {
                userPic.setUserImageOrInitials(
                    awardMentorModel.performerPhotoUrl,
                    awardMentorModel.performerName?.capitalize(Locale.getDefault()).toString(),
                    dpToPx = 28,
                    isRound = true
                )
            }
            awardMentorModel.awardImageUrl?.let {
                awardImage.setImage(it, AppObjectController.joshApplication)
            }
            awardImage.setOnClickListener {
                RxBus2.publish(
                    AwardItemClickedEventBus(
                        Award(
                            awardMentorModel.id,
                            awardMentorModel.awardText,
                            0,
                            null,
                            null,
                            awardMentorModel.awardDescription,
                            true,
                            true,
                            0,
                            null,
                            null
                        )
                    )
                )
            }
        }
    }

    override fun unBind() {

    }
}
