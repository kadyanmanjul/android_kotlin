package com.joshtalks.joshskills.ui.view_holders

import android.widget.TextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Position
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.util.*
import android.R.attr.button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LEFT_OF
import android.widget.RelativeLayout.ALIGN_PARENT_RIGHT
import com.joshtalks.joshskills.core.AppObjectController
import android.R.attr.gravity
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference


@Layout(R.layout.chat_text_message_holder)
class TextViewHolder(activityRef: WeakReference<FragmentActivity>, message: ChatModel) :
    BaseChatViewHolder(activityRef, message) {

    @View(R.id.parent_layout)
    lateinit var parent_layout: android.view.View

    @View(R.id.root_sub_view)
    lateinit var root_sub_view: FrameLayout


    @View(R.id.text_message_body)
    lateinit var text_message_body: TextView


    @View(R.id.text_message_time)
    lateinit var text_message_time: AppCompatTextView


    @View(R.id.root_view)
    lateinit var root_view: FrameLayout


    @View(R.id.message_view)
    lateinit var message_view: FrameLayout


    @Resolve
    fun onResolved() {
        message.sender?.let {
            updateView(it, root_view, root_sub_view, message_view)
        }
        text_message_time.text = Utils.messageTimeConversion(message.created)
        updateTime(text_message_time)
        if (message.question != null) {
            message.question?.qText.let {
                text_message_body.text = it

            }

        } else {
            text_message_body.text = message.text

        }
    }
}