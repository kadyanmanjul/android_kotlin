package com.joshtalks.joshskills.ui.lesson.conversationRoom.notification

import android.content.Context
import android.media.MediaPlayer
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.R
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.li_conversion_rooms_notification_bar.view.*


class NotificationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var enquiryAction: NotificationViewAction? = null
    var mediaPlayer: MediaPlayer? = null
    var userRequestedUuid: Int? = null
    var isSpeakerInviteNotification: Int? = null
    var state: ConversationRoomNotificationState = ConversationRoomNotificationState.DEFAULT

    interface NotificationViewAction {
        fun onAcceptNotification()
        fun onRejectNotification()
    }

    init {
        mediaPlayer = MediaPlayer.create(context, R.raw.ib_core_sound_new_message)
        View.inflate(context, R.layout.li_conversion_rooms_notification_bar, this)
        this.acceptButton.setOnClickListener {
            this.enquiryAction?.onAcceptNotification()
        }
        this.rejectButton.setOnClickListener {
            this.enquiryAction?.onRejectNotification()
        }
    }

    fun setNotificationState(state: ConversationRoomNotificationState) {
        this.state = state
    }

    fun getNotificationState() : ConversationRoomNotificationState {
        return this.state
    }

    fun setUserUuid(id: Int?) {
        this.userRequestedUuid = id
    }

    fun getUserUuid() :Int?{
        return this.userRequestedUuid
    }

    fun setAcceptButtonText(text: String) {
        this.acceptButton.text = text
    }

    fun loadAnimationSlideUp() {
        notification_bar.pivotY = notification_bar.layoutParams.height.div(2).toFloat()
        notification_bar.animate().scaleY(0.0F).duration = 500
        this.state= ConversationRoomNotificationState.DEFAULT

    }

    fun loadAnimationSlideDown() {
        notification_bar.pivotY = notification_bar.layoutParams.height.div(2).toFloat()
        notification_bar.animate().scaleY(1.0F).duration = 500
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

    fun showActionLayout() {
        this.action_layout.visibility = View.VISIBLE
    }

    fun setBackgroundColor(isPositive: Boolean) {
        when (isPositive) {
            true -> this.notification_bar.setBackgroundResource(R.color.notification_green_color)
            false -> this.notification_bar.setBackgroundResource(R.color.notification_red_color)
        }
    }

    fun setNotificationViewEnquiryAction(action: NotificationViewAction) {
        this.enquiryAction = action
    }

    fun startSound() {
        /*mediaPlayer = MediaPlayer.create(context, R.raw.ib_core_sound_new_message)
        mediaPlayer?.start()*/
    }

    fun endSound() {
        mediaPlayer?.pause()
    }

    fun destroyMediaPlayer() {
        mediaPlayer = null
    }
    @Parcelize
    enum class ConversationRoomNotificationState(val type: Int):Parcelable {
        DEFAULT(0),
        HAND_RAISED(1),
        YOUR_HAND_RAISED(2),
        JOIN_AS_SPEAKER(3),
        MOVED_TO_SPEAKER(4),
        MAX_LIMIT_REACHED(5),
        NO_INTERNET_AVAILABLE(6),
        API_ERROR(7)
    }
}