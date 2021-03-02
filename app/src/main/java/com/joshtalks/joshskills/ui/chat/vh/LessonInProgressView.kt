package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.extension.setImageViewWRPH
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.eventbus.LessonItemClickEventBus

class LessonInProgressView : FrameLayout {
    private lateinit var rootView: FrameLayout
    private var lessonModel: LessonModel? = null

    private lateinit var imageView: AppCompatImageView
    private lateinit var lessonNameTv: AppCompatTextView
    private lateinit var startLessonTv: AppCompatTextView
    private lateinit var continueLessonTv: AppCompatTextView
    private lateinit var grammarStatus: ImageView
    private lateinit var vocabStatus: ImageView
    private lateinit var readingStatus: ImageView
    private lateinit var speakingStatus: ImageView

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


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.lesson_progress_view, this)
        rootView = findViewById(R.id.root_view)
        rootView.setOnClickListener {
            lessonModel?.let {
                RxBus2.publish(LessonItemClickEventBus(it.id))
            }
        }
        imageView = findViewById(R.id.lesson_iv)
        lessonNameTv = findViewById(R.id.lesson_name_tv)
        startLessonTv = findViewById(R.id.start_lesson_tv)
        continueLessonTv = findViewById(R.id.continue_lesson_tv)
        grammarStatus = findViewById(R.id.view1)
        vocabStatus = findViewById(R.id.view2)
        readingStatus = findViewById(R.id.view3)
        speakingStatus = findViewById(R.id.view4)
    }

    fun setup(lesson: LessonModel) {
        this.lessonModel = lesson
        lessonNameTv.text = context.getString(
            R.string.lesson_name,
            lesson.lessonNo,
            lesson.lessonName
        )
        imageView.setImageViewWRPH(lesson.thumbnailUrl)
        setupUI(lesson)
    }

    private fun setupUI(lesson: LessonModel) {
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
    }

}