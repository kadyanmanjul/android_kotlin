package com.joshtalks.joshskills.ui.view_holders

import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.mindorks.placeholderview.annotations.*
import com.vanniktech.emoji.Utils

@Layout(R.layout.inbox_row_layout)
class InboxViewHolder(var inboxEntity: InboxEntity) : BaseCell() {

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


    @JvmField
    var drawablePadding: Float = 2f;

    var context = AppObjectController.joshApplication


    @Resolve
    fun onResolved() {
        profile_image.setImageResource(R.mipmap.ic_launcher)
        tvName.text = inboxEntity.course_name


        inboxEntity.type?.let {
            if (BASE_MESSAGE_TYPE.Q == it) {
                showRecentAsPerView(it)
            } else {
                showRecentAsPerView(it)
            }
        }

        inboxEntity.created?.let {
            tv_last_message_time.text = com.joshtalks.joshskills.core.Utils.getMessageTime(it)

        }
    }


    private fun showRecentAsPerView(baseMessageType: BASE_MESSAGE_TYPE) {

        tv_last_message.compoundDrawablePadding = getDrawablePadding()


        if (BASE_MESSAGE_TYPE.TX == baseMessageType) {
            inboxEntity.text?.let { text ->
                tv_last_message.text = text
            }

        } else if (BASE_MESSAGE_TYPE.IM == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_camera,
                0,
                0,
                0
            );
            tv_last_message.setCompoundDrawablePadding(Utils.dpToPx(context, drawablePadding))
            tv_last_message.text = "Photo"


        } else if (BASE_MESSAGE_TYPE.AU == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_audio,
                0,
                0,
                0
            );
            tv_last_message.setCompoundDrawablePadding(Utils.dpToPx(context, drawablePadding))
            tv_last_message.text = "Audio"


        } else if (BASE_MESSAGE_TYPE.VI == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_video,
                0,
                0,
                0
            );
            tv_last_message.setCompoundDrawablePadding(Utils.dpToPx(context, drawablePadding))
            tv_last_message.text = "Video"

        }
        else if (BASE_MESSAGE_TYPE.PD == baseMessageType) {
            tv_last_message.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_inbox_pdf,
                0,
                0,
                0
            );
            tv_last_message.setCompoundDrawablePadding(Utils.dpToPx(context, drawablePadding))
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
        RxBus.getDefault().send(inboxEntity)
    }


}