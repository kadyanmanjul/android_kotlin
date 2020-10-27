package com.joshtalks.joshskills.ui.day_wise_course.lesson

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference

@Layout(R.layout.layout_lesson_item)
class LessonViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?,
    private val onItemClick: ((lessonChats: ArrayList<ChatModel>, lessonId: Int) -> Unit)? = null
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {


    @View(R.id.lesson_iv)
    lateinit var imageView: AppCompatImageView

    @View(R.id.lesson_name_tv)
    lateinit var lessonNameTv: AppCompatTextView

    @View(R.id.start_lesson_tv)
    lateinit var startLessonTv: AppCompatTextView

    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        message.lessons?.let { lessonQuestions ->
            lessonQuestions.get(0).question?.lesson?.let { lessonModel ->
                lessonNameTv.text = getAppContext().getString(
                    R.string.lesson_name,
                    lessonModel.lessonNo,
                    lessonModel.lessonName
                )
                Utils.setImage(imageView, lessonModel.varthumbnail)

                rootView.setOnClickListener {
                    onItemClick?.invoke(lessonQuestions, lessonModel.id)
                }
            }
        }

        rootView.setBackgroundResource(
            getViewHolderBGResource(
                previousMessage?.sender,
                message.sender
            )
        )
    }
}