package com.joshtalks.joshskills.ui.view_holders

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
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
class InboxViewHolder(
    private var inboxEntity: InboxEntity,
    private val totalItem: Int,
    private val indexPos: Int
) :
    BaseCell() {

    @View(R.id.root_view)
    lateinit var rootView: ViewGroup

    @View(R.id.profile_image)
    lateinit var profileImage: ImageView

    @View(R.id.tv_name)
    lateinit var tvName: AppCompatTextView

    @View(R.id.tv_last_message)
    lateinit var tvLastReceivedMessage: AppCompatTextView


    @View(R.id.tv_last_message_time)
    lateinit var tvLastReceivedMessageTime: AppCompatTextView


    @View(R.id.tv_last_message_status)
    lateinit var tvLastMessageStatus: AppCompatImageView

    @View(R.id.horizontal_line)
    lateinit var hLine: android.view.View


    @JvmField
    var drawablePadding: Float = 2f

    var context = AppObjectController.joshApplication


    @Resolve
    fun onResolved() {
        profileImage.setImageResource(R.drawable.ic_josh_course)
        tvName.text = inboxEntity.course_name

        inboxEntity.course_icon?.let {
            setImageInImageView(profileImage, it)
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
            tvLastReceivedMessageTime.text =
                com.joshtalks.joshskills.core.Utils.getMessageTime(inboxEntity.created!!)
        } else {
            tvLastReceivedMessage.text = getAppContext().getString(R.string.click_to_start_course)
        }
        inboxEntity.created?.let {
        }
        if (inboxEntity.chat_id.isNullOrEmpty()) {
            tvLastReceivedMessageTime.setCompoundDrawablesWithIntrinsicBounds(
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

    @SuppressLint("SetTextI18n")
    private fun showRecentAsPerView(baseMessageType: BASE_MESSAGE_TYPE) {
        tvLastReceivedMessage.compoundDrawablePadding = getDrawablePadding()
        when {
            BASE_MESSAGE_TYPE.TX == baseMessageType -> {
                inboxEntity.qText?.let { text ->
                    tvLastReceivedMessage.text =
                        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }
                inboxEntity.text?.let { text ->
                    tvLastReceivedMessage.text =
                        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                }

            }
            BASE_MESSAGE_TYPE.IM == baseMessageType -> {
                tvLastReceivedMessage.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_inbox_camera,
                    0,
                    0,
                    0
                )
                tvLastReceivedMessage.compoundDrawablePadding =
                    Utils.dpToPx(context, drawablePadding)
                tvLastReceivedMessage.text = "Photo"


            }
            BASE_MESSAGE_TYPE.AU == baseMessageType -> {
                tvLastReceivedMessage.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_inbox_audio,
                    0,
                    0,
                    0
                )
                tvLastReceivedMessage.compoundDrawablePadding =
                    Utils.dpToPx(context, drawablePadding)
                tvLastReceivedMessage.text = "Audio"


            }
            BASE_MESSAGE_TYPE.VI == baseMessageType -> {
                tvLastReceivedMessage.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_inbox_video,
                    0,
                    0,
                    0
                )
                tvLastReceivedMessage.compoundDrawablePadding =
                    Utils.dpToPx(context, drawablePadding)
                tvLastReceivedMessage.text = "Video"

            }
            BASE_MESSAGE_TYPE.PD == baseMessageType -> {
                tvLastReceivedMessage.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_inbox_pdf,
                    0,
                    0,
                    0
                )
                tvLastReceivedMessage.compoundDrawablePadding =
                    Utils.dpToPx(context, drawablePadding)
                tvLastReceivedMessage.text = "Pdf"

            }
            BASE_MESSAGE_TYPE.UNLOCK == baseMessageType -> {
                tvLastReceivedMessage.text = "You can unlock your next class"

            }
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
                    tvLastMessageStatus.setImageResource(resource)
                }
            }
        }

    }

    @Click(R.id.root_view)
    fun onClick() {
        AppAnalytics.create(AnalyticsEvent.COURSE_ENGAGEMENT.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(
                AnalyticsEvent.CONVERSATION_ID.NAME,
                inboxEntity.conversation_id
            )
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .addParam(AnalyticsEvent.COURSE_ID.NAME, inboxEntity.courseId)
            .addParam(
                AnalyticsEvent.COURSE_DURATION.NAME,
                inboxEntity.duration?.toString() ?: EMPTY
            )
            .push()
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }
}