package com.joshtalks.joshskills.ui.view_holders

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
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


    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
    }
}