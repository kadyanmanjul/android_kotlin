package com.joshtalks.joshskills.conversationRoom.notification

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.R
import kotlinx.android.synthetic.main.li_conversion_rooms_notification_bar.view.acceptButton
import kotlinx.android.synthetic.main.li_conversion_rooms_notification_bar.view.action_layout
import kotlinx.android.synthetic.main.li_conversion_rooms_notification_bar.view.heading
import kotlinx.android.synthetic.main.li_conversion_rooms_notification_bar.view.notification_bar
import kotlinx.android.synthetic.main.li_conversion_rooms_notification_bar.view.rejectButton

class NotificationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    var enquiryAction: NotificationViewAction? = null

    interface NotificationViewAction {
        fun onAcceptNotification()
        fun onRejectNotification()
    }

    init {
        View.inflate(context, R.layout.li_conversion_rooms_notification_bar, this)
        this.acceptButton.setOnClickListener {
            this.enquiryAction?.onAcceptNotification()
        }
        this.rejectButton.setOnClickListener {
            this.enquiryAction?.onRejectNotification()
        }
    }

    fun setAcceptButtonText(text: String) {
        this.acceptButton.text = text
    }

    fun setRejectButtonText(text: String) {
        this.rejectButton.text = text
    }

    fun setHeading(text: String) {
        this.heading.text = text
    }

    fun hideActionLayout() {
        this.action_layout.visibility = View.GONE
    }

    fun showActionLayout(){
        this.action_layout.visibility = View.VISIBLE
    }

    fun setBackgroundColor(isPositive: Boolean){
        when(isPositive){
            true -> this.notification_bar.setBackgroundResource(R.color.notification_green_color)
            false -> this.notification_bar.setBackgroundResource(R.color.notification_red_color)
        }
    }

    fun setNotificationViewEnquiryAction(action: NotificationViewAction) {
        this.enquiryAction = action
    }
}