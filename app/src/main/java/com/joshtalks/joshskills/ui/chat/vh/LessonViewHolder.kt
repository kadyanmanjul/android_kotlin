package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.assessment.view.Stub

class LessonViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private val rootView: FrameLayout = view.findViewById(R.id.root_view_fl)
    private var lessonCompleteStub: Stub<LessonCompleteView> =
        Stub(view.findViewById(R.id.lesson_complete_stub))
    private var lessonInProgressStub: Stub<LessonInProgressView> =
        Stub(view.findViewById(R.id.lesson_progress_stub))

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        setupUI(message.lesson!!)
    }

    private fun setupUI(lesson: LessonModel) {
        if (lesson.status == LESSON_STATUS.CO) {
            lessonCompleteStub.resolved().let {
                lessonCompleteStub.get().setup(lesson)
            }
        } else {
            lessonInProgressStub.resolved().let {
                lessonInProgressStub.get().setup(lesson)
            }
        }
    }

    override fun unBind() {

    }
}