package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.assessment.view.Stub

class LessonViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    private var message: ChatModel? = null
    private val rootView: FrameLayout = view.findViewById(R.id.root_view_fl)
    private var lessonCompleteStub: Stub<LessonCompleteView> =
        Stub(view.findViewById(R.id.lesson_complete_stub))
    private var lessonInProgressStub: Stub<LessonInProgressView> =
        Stub(view.findViewById(R.id.lesson_progress_stub))


    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        if (this.message != null && this.message!!.chatId != message.chatId) {
            return
        }
        this.message = message
        setupUI(message.lesson!!)
    }

    private fun setupUI(lesson: LessonModel) {
        if (lesson.status == LESSON_STATUS.CO) {
            if (lessonCompleteStub.resolved().not()) {
                lessonCompleteStub.get().setup(lesson)
            }
        } else {
            if (lessonInProgressStub.resolved().not()) {
                lessonInProgressStub.get().setup(lesson)
            }
        }
    }

    override fun unBind() {

    }
}