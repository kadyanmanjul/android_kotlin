package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.assessment.view.Stub

class LessonViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)

    private var lessonCompleteStub: Stub<LessonCompleteView> =
        Stub(view.findViewById(R.id.lesson_complete_stub))
    private var lessonInProgressStub: Stub<LessonInProgressView> =
        Stub(view.findViewById(R.id.lesson_progress_stub))

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        if (null != message.sender) {
            setViewHolderBG(message, previousMessage, rootSubView)
        }
        message.lesson?.let {
            setupUI(it)
        }
    }

    private fun setupUI(lesson: LessonModel) {
        val isLessonCompleted= lesson.status == LESSON_STATUS.CO

        if (isLessonCompleted) {
            lessonInProgressStub.get().visibility = View.GONE
            lessonCompleteStub.resolved().let {
                lessonCompleteStub.get().visibility = View.VISIBLE
                lessonCompleteStub.get().setup(lesson,AppObjectController.getFirebaseRemoteConfig()
                    .getBoolean(FirebaseRemoteConfigKey.IS_CONVERSATION_ROOM_ACTIVE))
            }
        } else {
            lessonCompleteStub.get().visibility = View.GONE
            lessonInProgressStub.resolved().let {
                lessonInProgressStub.get().visibility = View.VISIBLE
                lessonInProgressStub.get().setup(lesson,AppObjectController.getFirebaseRemoteConfig()
                    .getBoolean(FirebaseRemoteConfigKey.IS_CONVERSATION_ROOM_ACTIVE))
            }
        }
    }

    override fun unBind() {

    }
}