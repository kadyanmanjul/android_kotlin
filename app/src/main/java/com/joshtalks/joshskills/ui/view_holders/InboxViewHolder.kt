package com.joshtalks.joshskills.ui.view_holders

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.YYYY_MM_DD
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.util.*

@Layout(R.layout.inbox_row_layout)
class InboxViewHolder(
    private var inboxEntity: InboxEntity,
    private val totalItem: Int,
    private val indexPos: Int
) : BaseCell() {

    @View(R.id.root_view)
    lateinit var rootView: ViewGroup

    @View(R.id.profile_image)
    lateinit var profileImage: ImageView

    @View(R.id.tv_name)
    lateinit var tvName: AppCompatTextView

    @View(R.id.tv_last_message_time)
    lateinit var tvLastReceivedMessageTime: AppCompatTextView

    @View(R.id.horizontal_line)
    lateinit var hLine: android.view.View

    @View(R.id.iv_tick)
    lateinit var ivTick: AppCompatImageView

    @View(R.id.course_progress_bar)
    lateinit var courseProgressBar: ProgressBar


    @JvmField
    var drawablePadding: Float = 2f

    var context = AppObjectController.joshApplication

    @Resolve
    fun onResolved() {
        profileImage.setImageResource(R.drawable.ic_josh_course)
        tvName.text = inboxEntity.course_name
        courseProgressBar.progress = 0
        inboxEntity.course_icon?.let {
            setImageInImageView(profileImage, it)
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

        inboxEntity.created?.run {
            tvLastReceivedMessageTime.text = Utils.getMessageTime(this)
            if (inboxEntity.batchStarted.isNotEmpty()) {
                val lastDownloadDate = YYYY_MM_DD.format(this).toLowerCase(Locale.getDefault())
                val diff =
                    Utils.dateDifferenceInDays(
                        inboxEntity.batchStarted,
                        lastDownloadDate,
                        YYYY_MM_DD
                    )
                        .toInt()
                courseProgressBar.progress = diff
                inboxEntity.duration?.run {
                    courseProgressBar.max = this

                    if (diff >= this) {
                        ivTick.setBackgroundResource(R.drawable.ic_course_in_complete_bg)
                    } else {
                        ivTick.setBackgroundResource(R.drawable.ic_course_complete_bg)
                    }
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
                inboxEntity.duration.toString()
            )
            .push()
        RxBus2.publish(OpenCourseEventBus(inboxEntity))
    }
}