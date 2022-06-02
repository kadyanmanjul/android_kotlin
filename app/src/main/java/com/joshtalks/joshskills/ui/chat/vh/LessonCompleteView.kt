package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.IS_A2_C1_RETENTION_ENABLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.eventbus.LessonItemClickEventBus

class LessonCompleteView : FrameLayout {
    private lateinit var lessonNameTvCompleted: AppCompatTextView
    private lateinit var rootView: FrameLayout
    private var lessonModel: LessonModel? = null
    private lateinit var roomStatus: ImageView
    private lateinit var translationStatus: ImageView

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
        translationStatus = findViewById(R.id.view12)
        rootView = findViewById(R.id.root_view_completed)
        rootView.setOnClickListener {
            lessonModel?.let {
                RxBus2.publish(LessonItemClickEventBus(it.id, it.isNewGrammar, true))
            }
        }
    }

    fun setup(message: LessonModel, isConversationRoomActive: Boolean) {
        this.lessonModel = message
        lessonNameTvCompleted.text =
            context.getString(R.string.lesson_name, message.lessonNo, message.lessonName)
        roomStatus.isVisible = isConversationRoomActive
        translationStatus.isVisible =
            message.isNewGrammar && PrefManager.hasKey(IS_A2_C1_RETENTION_ENABLED) &&
                    PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED)
    }

}