package com.joshtalks.joshskills.ui.chat.vh

import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.GONE
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.extension.setImageViewWRPH
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.LessonItemClickEventBus

//@Layout(R.layout.layout_lesson_item)
class LessonViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    val imageView: AppCompatImageView = view.findViewById(R.id.lesson_iv)
    val lessonNameTv: AppCompatTextView = view.findViewById(R.id.lesson_name_tv)
    val lessonNameTvCompleted: AppCompatTextView = view.findViewById(R.id.lesson_name_tv__completed)
    val startLessonTv: AppCompatTextView = view.findViewById(R.id.start_lesson_tv)
    val continueLessonTv: AppCompatTextView = view.findViewById(R.id.continue_lesson_tv)
    val rootViewUncompleted: FrameLayout = view.findViewById(R.id.root_view)
    val rootView: FrameLayout = view.findViewById(R.id.root_view_fl)
    val rootViewCompleted: FrameLayout = view.findViewById(R.id.root_view_completed)
    val grammarStatus: ImageView = view.findViewById(R.id.view1)
    val vocabStatus: ImageView = view.findViewById(R.id.view2)
    val readingStatus: ImageView = view.findViewById(R.id.view3)
    val speakingStatus: ImageView = view.findViewById(R.id.view4)
    private var message: ChatModel? = null

    init {
        rootViewUncompleted.also { it ->
            it.setOnClickListener {
                message?.lesson?.let {
                    RxBus2.publish(LessonItemClickEventBus(it.id))
                }
            }
        }
        rootViewUncompleted.also { it ->
            it.setOnClickListener {
                message?.lesson?.let {
                    RxBus2.publish(LessonItemClickEventBus(it.id))
                }
            }
        }

    }

    private val drawableAttempted: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_lesson_green_tick,
            null
        )
    }
    val drawableUnattempted: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_lesson_disabled_tick,
            null
        )
    }

    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        this.message = message
        grammarStatus.visibility = GONE
        vocabStatus.visibility = GONE
        readingStatus.visibility = GONE
        speakingStatus.visibility = GONE
        startLessonTv.visibility = GONE
        continueLessonTv.visibility = GONE
        rootViewCompleted.visibility = GONE
        rootViewUncompleted.visibility = GONE

        lessonNameTvCompleted.text = EMPTY
        lessonNameTv.text = EMPTY
        grammarStatus.setImageDrawable(null)
        vocabStatus.setImageDrawable(null)
        readingStatus.setImageDrawable(null)
        speakingStatus.setImageDrawable(null)
        message.lesson?.let { lesson ->
            if (lesson.status != LESSON_STATUS.CO) {
                rootViewUncompleted.visibility = View.VISIBLE
                rootViewCompleted.visibility = GONE

                if (lesson.status == LESSON_STATUS.AT) {
                    startLessonTv.visibility = GONE
                    continueLessonTv.visibility = View.VISIBLE

                    grammarStatus.visibility = View.VISIBLE
                    vocabStatus.visibility = View.VISIBLE
                    readingStatus.visibility = View.VISIBLE
                    speakingStatus.visibility = View.VISIBLE

                    if (lesson.grammarStatus == LESSON_STATUS.CO) {
                        grammarStatus.setImageDrawable(drawableAttempted)
                    } else {
                        grammarStatus.setImageDrawable(drawableUnattempted)
                    }
                    if (lesson.vocabStatus == LESSON_STATUS.CO) {
                        vocabStatus.setImageDrawable(drawableAttempted)
                    } else {
                        vocabStatus.setImageDrawable(drawableUnattempted)
                    }
                    if (lesson.readingStatus == LESSON_STATUS.CO) {
                        readingStatus.setImageDrawable(drawableAttempted)
                    } else {
                        readingStatus.setImageDrawable(drawableUnattempted)
                    }
                    if (lesson.speakingStatus == LESSON_STATUS.CO) {
                        speakingStatus.setImageDrawable(drawableAttempted)
                    } else {
                        speakingStatus.setImageDrawable(drawableUnattempted)
                    }
                } else {
                    grammarStatus.visibility = GONE
                    vocabStatus.visibility = GONE
                    readingStatus.visibility = GONE
                    speakingStatus.visibility = GONE
                    startLessonTv.visibility = View.VISIBLE
                    continueLessonTv.visibility = GONE
                }

                lessonNameTv.text = getAppContext().getString(
                    R.string.lesson_name,
                    lesson.lessonNo,
                    lesson.lessonName
                )
                imageView.setImageViewWRPH(lesson.thumbnailUrl)
                setViewHolderBG(previousMessage?.sender, message.sender!!, rootViewUncompleted)
            } else {
                rootViewCompleted.visibility = View.VISIBLE
                rootViewUncompleted.visibility = GONE
                message.lesson?.let { lessonModel ->
                    lessonNameTvCompleted.text = getAppContext().getString(
                        R.string.lesson_name,
                        lessonModel.lessonNo,
                        lessonModel.lessonName
                    )
                }
            }
        }
    }

    override fun unBind() {

    }
}