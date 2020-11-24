package com.joshtalks.joshskills.ui.day_wise_course.lesson

import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.ui.view_holders.BaseCell
import com.joshtalks.joshskills.ui.view_holders.BaseChatViewHolder
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.lang.ref.WeakReference

@Layout(R.layout.layout_lesson_complete_item)
class LessonCompleteViewHolder(val message: LessonModel) :
    BaseCell() {

    @View(R.id.lesson_name_tv)
    lateinit var lessonNameTv: AppCompatTextView


    @View(R.id.root_view)
    lateinit var rootView: FrameLayout

    @Resolve
    fun onViewInflated() {
        message.let { lessonModel ->
            lessonNameTv.text = getAppContext().getString(
                R.string.lesson_name,
                lessonModel.lessonNo,
                lessonModel.lessonName
            )
        }
    }
}
