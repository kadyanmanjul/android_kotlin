package com.joshtalks.joshskills.ui.day_wise_course.lesson

import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
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
    private val onItemClick: ((lessonId: Int, lessonInterval: Int, chatId: String) -> Unit)? = null
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {


    @View(R.id.lesson_iv)
    lateinit var imageView: AppCompatImageView

    @View(R.id.lesson_name_tv)
    lateinit var lessonNameTv: AppCompatTextView

    @View(R.id.lesson_name_tv__completed)
    lateinit var lessonNameTvCompleted: AppCompatTextView

    @View(R.id.start_lesson_tv)
    lateinit var startLessonTv: AppCompatTextView

    @View(R.id.root_view)
    lateinit var rootViewUncompleted: FrameLayout

    @View(R.id.root_view_fl)
    lateinit var rootView: FrameLayout

    @View(R.id.root_view_completed)
    lateinit var rootViewCompleted: FrameLayout

    @View(R.id.view1)
    lateinit var grammarStatus: ImageView

    @View(R.id.view2)
    lateinit var vocabStatus: ImageView

    @View(R.id.view3)
    lateinit var readingStatus: ImageView

    @View(R.id.view4)
    lateinit var speakingStatus: ImageView

    override fun getRoot(): FrameLayout {
        return rootView
    }

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        if (message.question?.lesson?.status != LESSON_STATUS.CO) {
            rootViewUncompleted.visibility = android.view.View.VISIBLE
            rootViewCompleted.visibility = android.view.View.GONE

            if (message.question?.lesson?.status == LESSON_STATUS.AT) {
                startLessonTv.text = getAppContext().getString(R.string.continue_lesson)
            } else {
                startLessonTv.text = getAppContext().getString(R.string.start_lesson)
            }

            message.lessons?.let { lessonModel ->
                lessonNameTv.text = getAppContext().getString(
                    R.string.lesson_name,
                    lessonModel.lessonNo,
                    lessonModel.lessonName
                )
                Utils.setImage(imageView, lessonModel.varthumbnail)

            }
            rootViewUncompleted.setOnClickListener {
                message.question?.lesson?.let {
                    onItemClick?.invoke(
                        it.id, message.question?.lesson?.interval ?: -1,
                        message.chatId
                    )
                }
            }

            rootViewUncompleted.setBackgroundResource(
                getViewHolderBGResource(
                    previousMessage?.sender,
                    message.sender
                )
            )
        } else {
            rootViewCompleted.visibility = android.view.View.VISIBLE
            rootViewUncompleted.visibility = android.view.View.GONE
            message.question?.lesson?.let { lessonModel ->
                lessonNameTvCompleted.text = getAppContext().getString(
                    R.string.lesson_name,
                    lessonModel.lessonNo,
                    lessonModel.lessonName
                )
            }
            rootViewCompleted.setOnClickListener {
                message.lessons?.let {
                    onItemClick?.invoke(it.id, message.lessons?.interval ?: -1, message.chatId)
                }
            }
        }

        message.question?.lesson?.let {
            if (it.grammarStatus == LESSON_STATUS.CO) {
                grammarStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.green_right_answer
                    )
                )
            } else {
                grammarStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.grey
                    )
                )
            }
            if (it.vocabStatus == LESSON_STATUS.CO) {
                vocabStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.green_right_answer
                    )
                )
            } else {
                vocabStatus.drawable.setTint(ContextCompat.getColor(getAppContext(), R.color.grey))
            }
            if (it.readingStatus == LESSON_STATUS.CO) {
                readingStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.green_right_answer
                    )
                )
            } else {
                readingStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.grey
                    )
                )
            }
            if (it.speakingStatus == LESSON_STATUS.CO) {
                speakingStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.green_right_answer
                    )
                )
            } else {
                speakingStatus.drawable.setTint(
                    ContextCompat.getColor(
                        getAppContext(),
                        R.color.grey
                    )
                )
            }
        }
    }
}