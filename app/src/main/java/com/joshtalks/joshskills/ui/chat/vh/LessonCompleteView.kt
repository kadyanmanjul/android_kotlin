package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.eventbus.LessonItemClickEventBus

class LessonCompleteView : FrameLayout {
    private lateinit var lessonNameTvCompleted: AppCompatTextView
    private lateinit var rootView: FrameLayout
    private var lessonModel: LessonModel? = null
    private lateinit var roomStatus: ImageView

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
        View.inflate(context, R.layout.lesson_complete_view, this)
        lessonNameTvCompleted = findViewById(R.id.lesson_name_tv__completed)
        roomStatus = findViewById(R.id.view15)
        rootView = findViewById(R.id.root_view_completed)
        rootView.setOnClickListener {
            lessonModel?.let {
                RxBus2.publish(LessonItemClickEventBus(it.id,it.isNewGrammar,true))
            }
        }
    }

    fun setup(message: LessonModel, isConversationRoomActive: Boolean) {
        this.lessonModel = message
        lessonNameTvCompleted.text =
            context.getString(R.string.lesson_name, message.lessonNo, message.lessonName)
        roomStatus.visibility = if (isConversationRoomActive) View.VISIBLE else View.INVISIBLE
    }

}