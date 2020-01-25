package com.joshtalks.joshskills.ui.view_holders

import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.vanniktech.emoji.Utils

@Layout(R.layout.inbox_row_layout)
class InboxViewHolder(var inboxEntity: InboxEntity, val totalItem: Int, val indexPos: Int) :
    BaseCell() {

    @View(R.id.profile_image)
    lateinit var profile_image: ImageView

    @View(R.id.tv_name)
    lateinit var tvName: AppCompatTextView

    @View(R.id.tv_last_message)
    lateinit var tv_last_message: AppCompatTextView


    @View(R.id.tv_last_message_time)
    lateinit var tv_last_message_time: AppCompatTextView


    @View(R.id.tv_last_message_status)
    lateinit var tv_last_message_status: AppCompatImageView

    @View(R.id.tv_notification)
    lateinit var tv_notification: AppCompatTextView

    @View(R.id.horizontal_line)
    lateinit var hLine: android.view.View


    @JvmField
    var drawablePadding: Float = 2f

    var context = AppObjectController.joshApplication


    @Resolve
    fun onResolved() {
        profile_image.setImageResource(R.drawable.ic_josh_course)
        tvName.text = inboxEntity.course_name

        inboxEntity.course_icon?.let {
            setImageInImageView(profile_image, it)
        }


        inboxEntity.type?.let {
            if (BASE_MESSAGE_TYPE.Q == it || BASE_MESSAGE_TYPE.AR == it) {
                inboxEntity.material_type?.let { messageType ->
                    showRecentAsPerView(messageType)
                }
            } else {
                showRecentAsPerView(it)
            }
        }

        if (inboxEntity.created != null) {
            tv_last_message_time.text =
                com.joshtalks.joshskills.core.Utils.getMessageTime(inboxEntity.created!!)
        } else {
            tv_last_message.text = getAppContext().getString(R.string.click_to_start_course)

        }
        inboxEntity.created?.let {
        }
        if (inboxEntity.chat_id.isNullOrEmpty()) {
            tv_last_message_time.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_unread,
                0,
                0,
                0
            )
        }
        if ((totalItem - 1) == indexPos && AppObjectController.getFirebaseRemoteConfig().getBoolean(
                "course_explore_flag"
            )
        ) {
            hLine.visibility = android.view.View.GONE
        }


    }


    private fun showRecentAsPerView(baseMessageType: BASE_MESSAGE_TYPE) {

        tv_last_message.compoundDrawablePadding = getDrawablePadding()


        if (BASE_MESSAGE_TYPE.TX == baseMessageType) {
            inboxEntity.qText?.let { text ->
                tv_last_message.text = text
            }
            inboxEntity.text?.let { text ->
                tv_last_message.text = text
            }

        } else if (BASE_MESSAGE_TYPE.IM == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_camera,
                0,
                0,
                0
            )
            tv_last_message.compoundDrawablePadding = Utils.dpToPx(context, drawablePadding)
            tv_last_message.text = "Photo"


        } else if (BASE_MESSAGE_TYPE.AU == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_audio,
                0,
                0,
                0
            )
            tv_last_message.compoundDrawablePadding = Utils.dpToPx(context, drawablePadding)
            tv_last_message.text = "Audio"


        } else if (BASE_MESSAGE_TYPE.VI == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_video,
                0,
                0,
                0
            )
            tv_last_message.compoundDrawablePadding = Utils.dpToPx(context, drawablePadding)
            tv_last_message.text = "Video"

        } else if (BASE_MESSAGE_TYPE.PD == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_pdf,
                0,
                0,
                0
            )
            tv_last_message.compoundDrawablePadding = Utils.dpToPx(context, drawablePadding)
            tv_last_message.text = "Pdf"

        }

        if (this.inboxEntity.message_deliver_status != null) {
            inboxEntity.message_deliver_status?.let { messageDeliverStatus ->

                inboxEntity.user?.id.let {

                    var resource = 0
                    if (it.equals(getUserId(), ignoreCase = true)) {
                        resource = when (messageDeliverStatus) {
                            MESSAGE_DELIVER_STATUS.SENT ->
                                R.drawable.ic_sent_message_s_tick
                            MESSAGE_DELIVER_STATUS.SENT_RECEIVED ->
                                R.drawable.ic_sent_message_d_tick
                            else -> R.drawable.ic_sent_message_s_r_tick
                        }
                    }
                    tv_last_message_status.setImageResource(resource)
                }
            }
        }

    }


    @Click(R.id.chat_row_container)
    fun onClick() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }

    @Click(R.id.profile_image)
    fun onClickProfileImage() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }

    @Click(R.id.tv_name)
    fun onClickCourseName() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }

    @Click(R.id.tv_last_message_status)
    fun onClickLastMessageStatus() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }

    @Click(R.id.tv_last_message)
    fun onClickLastMessage() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }

    @Click(R.id.tv_last_message_time)
    fun onClickLastMessageTime() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }

    @Click(R.id.tv_notification)
    fun onClickNotification() {
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }


}