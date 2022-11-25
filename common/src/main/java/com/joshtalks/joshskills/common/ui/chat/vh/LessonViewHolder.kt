package com.joshtalks.joshskills.common.ui.chat.vh

import android.content.res.ColorStateList
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.common.repository.local.entity.LessonModel
import com.joshtalks.joshskills.common.ui.assessment.view.Stub

class LessonViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)

    private var lessonCompleteStub: Stub<LessonCompleteView> =
        Stub(view.findViewById(R.id.lesson_complete_stub))
    private var lessonInProgressStub: Stub<LessonInProgressView> =
        Stub(view.findViewById(R.id.lesson_progress_stub))

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        if (null != message.sender) {
            setViewHolderBGRound(message, previousMessage, rootSubView)
        }
        message.lesson?.let {
            setupUI(it)
        }
    }

    private fun setupUI(lesson: LessonModel) {
        val isLessonCompleted = lesson.status == LESSON_STATUS.CO

        if (isLessonCompleted) {
            lessonInProgressStub.get().visibility = View.GONE
            lessonCompleteStub.resolved().let {
                lessonCompleteStub.get().visibility = View.VISIBLE
                lessonCompleteStub.get().setup(lesson)
            }
        } else {
            lessonCompleteStub.get().visibility = View.GONE
            lessonInProgressStub.resolved().let {
                lessonInProgressStub.get().visibility = View.VISIBLE
                lessonInProgressStub.get().setup(lesson)
            }
        }
    }

    override fun unBind() {

    }
}