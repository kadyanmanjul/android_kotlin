package com.joshtalks.joshskills.common.ui.lesson.viewholder

import android.graphics.drawable.Drawable
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.common.ui.view_holders.BaseChatViewHolder
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference

class LessonViewHolder(
    activityRef: WeakReference<FragmentActivity>,
    message: ChatModel,
    previousMessage: ChatModel?,
    private val onItemClick: ((lessonId: Int, lessonInterval: Int, chatId: String) -> Unit)? = null
) :
    BaseChatViewHolder(activityRef, message, previousMessage) {


    
    lateinit var imageView: AppCompatImageView

    
    lateinit var lessonNameTv: AppCompatTextView

    
    lateinit var lessonNameTvCompleted: AppCompatTextView

    
    lateinit var startLessonTv: AppCompatTextView

    
    lateinit var continueLessonTv: AppCompatTextView

    
    lateinit var rootViewUncompleted: FrameLayout

    
    lateinit var rootView: FrameLayout

    
    lateinit var rootViewCompleted: FrameLayout

    
    lateinit var grammarStatus: ImageView

    
    lateinit var vocabStatus: ImageView

    
    lateinit var readingStatus: ImageView

    
    lateinit var speakingStatus: ImageView

    override fun getRoot(): FrameLayout {
        return rootView
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

    @Resolve
    override fun onViewInflated() {
        super.onViewInflated()
        if (message.lesson?.status != LESSON_STATUS.CO) {
            rootViewUncompleted.visibility = VISIBLE
            rootViewCompleted.visibility = GONE

            if (message.lesson?.status == LESSON_STATUS.AT) {
                startLessonTv.visibility = GONE
                continueLessonTv.visibility = VISIBLE

                grammarStatus.visibility = VISIBLE
                vocabStatus.visibility = VISIBLE
                readingStatus.visibility = VISIBLE
                speakingStatus.visibility = VISIBLE

                message.lesson?.let {
                    if (it.grammarStatus == LESSON_STATUS.CO) {
                        grammarStatus.setImageDrawable(drawableAttempted)
                    } else {
                        grammarStatus.setImageDrawable(drawableUnattempted)
                    }
                    if (it.vocabStatus == LESSON_STATUS.CO) {
                        vocabStatus.setImageDrawable(drawableAttempted)
                    } else {
                        vocabStatus.setImageDrawable(drawableUnattempted)
                    }
                    if (it.readingStatus == LESSON_STATUS.CO) {
                        readingStatus.setImageDrawable(drawableAttempted)
                    } else {
                        readingStatus.setImageDrawable(drawableUnattempted)
                    }
                    if (it.speakingStatus == LESSON_STATUS.CO) {
                        speakingStatus.setImageDrawable(drawableAttempted)
                    } else {
                        speakingStatus.setImageDrawable(drawableUnattempted)
                    }
                }
            } else {

                grammarStatus.visibility = GONE
                vocabStatus.visibility = GONE
                readingStatus.visibility = GONE
                speakingStatus.visibility = GONE
                startLessonTv.visibility = VISIBLE
                continueLessonTv.visibility = GONE
            }

            message.lesson?.let { lessonModel ->
                lessonNameTv.text = getAppContext().getString(
                    R.string.lesson_name,
                    lessonModel.lessonNo,
                    lessonModel.lessonName
                )
                //  Utils.setImage(imageView, lessonModel.varthumbnail)
                setImageInImageViewV2(imageView, lessonModel.thumbnailUrl)

            }
            rootViewUncompleted.setOnClickListener {
                message.lesson?.let {
                    onItemClick?.invoke(
                        it.id, message.lesson?.interval ?: -1,
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
            rootViewCompleted.visibility = VISIBLE
            rootViewUncompleted.visibility = GONE
            message.lesson?.let { lessonModel ->
                lessonNameTvCompleted.text = getAppContext().getString(
                    R.string.lesson_name,
                    lessonModel.lessonNo,
                    lessonModel.lessonName
                )
            }
            rootViewCompleted.setOnClickListener {
                message.lesson?.let {
                    onItemClick?.invoke(it.id, message.lesson?.interval ?: -1, message.chatId)
                }
            }
        }
    }
}
