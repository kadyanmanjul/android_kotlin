package com.joshtalks.joshskills.ui.view_holders

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView
import java.lang.ref.WeakReference

@Layout(R.layout.layout_student_card)
class StudentCardViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?,
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {

    @View(R.id.tv_student_of)
    lateinit var studentOfDash: AppCompatTextView

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

    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    private var userName: String="Josh Skills"

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        val textList = message.text?.split("$")
        textList?.forEachIndexed { index, text ->
            when (index) {
                0 -> {
                    studentName.text = text
                    userName=text
                }
                1 -> {
                    totalPoints.text = text
                }
                2 -> {
                    userText.text = text
                }
                else -> {

                }
            }
        }

        when (message.question?.chatType) {
            CHAT_TYPE.SOTD-> {
                studentOfDash.text = "STUDENT OF THE DAY"
            }
            CHAT_TYPE.SOTW -> {
                studentOfDash.text ="STUDENT OF THE WEEK"
            }
            CHAT_TYPE.SOTM -> {
                studentOfDash.text ="STUDENT OF THE MONTH"
            }
            CHAT_TYPE.SOTY -> {
                studentOfDash.text ="STUDENT OF THE YEAR"
            }
            else -> {

            }
        }
        userPic.post {
            userPic.setUserImageOrInitials(message.url, userName)
        }
        message.question?.imageList?.get(0)?.let {
            awardImage.setImage(it.imageUrl, AppObjectController.joshApplication)
        }
    }
}
