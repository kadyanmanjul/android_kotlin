package com.joshtalks.joshskills.ui.inbox.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.YYYY_MM_DD
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.InboxItemLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import timber.log.Timber
import java.util.*

class InboxAdapter(private var lifecycleProvider: LifecycleOwner) :
    RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {
    private var items: ArrayList<InboxEntity> = arrayListOf()
    var drawablePadding: Float = 2f
    var progressBarStatus: Boolean = true

    init {
        progressBarStatus = AppObjectController.getFirebaseRemoteConfig()
            .getBoolean(FirebaseRemoteConfigKey.INBOX_SCREEN_COURSE_PROGRESS)
    }

    fun getAppContext() = AppObjectController.joshApplication

    private fun getDrawablePadding() = Utils.dpToPx(getAppContext(), 4f)

    fun addItems(newList: List<InboxEntity>) {
        if (newList.isEmpty()) {
            return
        }
        val diffCallback = InboxDiffCallback(items, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InboxViewHolder {
        val binding =
            InboxItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.apply {
            lifecycleOwner = lifecycleProvider
        }
        return InboxViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        holder.bind(items[position], position)
    }


    inner class InboxViewHolder(val binding: InboxItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(inboxEntity: InboxEntity, indexPos: Int) {
            with(binding) {
                obj = inboxEntity
                tvLastMessageTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tvName.text = inboxEntity.course_name
                courseProgressBar.progress = 0
                tvLastMessage
                //   profileImage.setInboxImageView(inboxEntity.course_icon)
                if (inboxEntity.chat_id.isNullOrEmpty()) {
                    tvLastMessageTime.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_unread,
                        0,
                        0,
                        0
                    )
                }

                if ((itemCount - 1) == indexPos) {
                    horizontalLine.visibility = android.view.View.GONE
                }
                if (progressBarStatus) {
                    courseProgressBar.visibility = android.view.View.VISIBLE
                    tvLastMessage.visibility = android.view.View.GONE
                    if (inboxEntity.batchStarted.isNullOrEmpty().not()) {
                        val todayDate = YYYY_MM_DD.format(Date()).toLowerCase(Locale.getDefault())
                        val diff =
                            Utils.dateDifferenceInDays(
                                inboxEntity.batchStarted!!,
                                todayDate,
                                YYYY_MM_DD
                            )
                                .toInt()

                        inboxEntity.duration?.run {
                            courseProgressBar.max = this * 100
                            setUpProgressAnimate(diff * 100)

                            if (diff >= this) {
                                ivTick.setBackgroundResource(R.drawable.ic_course_in_complete_bg)
                                courseProgressBar.progressTintList = ColorStateList.valueOf(
                                    ContextCompat.getColor(getAppContext(), R.color.text_light_ac)
                                )
                            } else {
                                ivTick.setBackgroundResource(R.drawable.ic_course_complete_bg)
                            }
                        }

                    } else {
                        Timber.d("Batch Created not found")
                    }
                } else {
                    courseProgressBar.visibility = android.view.View.GONE
                    tvLastMessage.visibility = android.view.View.VISIBLE
                    inboxEntity.type?.let {
                        if (BASE_MESSAGE_TYPE.Q == it || BASE_MESSAGE_TYPE.AR == it) {
                            inboxEntity.material_type?.let { messageType ->
                                showRecentAsPerView(inboxEntity, messageType)
                            }
                        } else {
                            showRecentAsPerView(inboxEntity, it)
                        }
                    }
                }
                inboxEntity.created?.run {
                    binding.tvLastMessageTime.text = Utils.getMessageTime(this)
                }
                rootView.setOnClickListener {
                    onClickView(inboxEntity)
                }
            }
        }

        private fun setUpProgressAnimate(diff: Int) {
            val animation: ObjectAnimator =
                ObjectAnimator.ofInt(binding.courseProgressBar, "progress", 0, diff)
            animation.startDelay = 0
            animation.duration = 700
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.start()
        }

        @SuppressLint("SetTextI18n")
        private fun showRecentAsPerView(
            inboxEntity: InboxEntity,
            baseMessageType: BASE_MESSAGE_TYPE
        ) {
            binding.tvLastMessage.compoundDrawablePadding = getDrawablePadding()
            when {
                BASE_MESSAGE_TYPE.TX == baseMessageType -> {
                    inboxEntity.qText?.let { text ->
                        binding.tvLastMessage.text =
                            HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }
                    inboxEntity.text?.let { text ->
                        binding.tvLastMessage.text =
                            HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                    }

                }
                BASE_MESSAGE_TYPE.IM == baseMessageType -> {
                    binding.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_inbox_camera,
                        0,
                        0,
                        0
                    )
                    binding.tvLastMessage.compoundDrawablePadding =
                        Utils.dpToPx(getAppContext(), drawablePadding)
                    binding.tvLastMessage.text = "Photo"
                }
                BASE_MESSAGE_TYPE.AU == baseMessageType -> {
                    binding.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_inbox_audio,
                        0,
                        0,
                        0
                    )
                    binding.tvLastMessage.compoundDrawablePadding =
                        Utils.dpToPx(getAppContext(), drawablePadding)
                    binding.tvLastMessage.text = "Audio"


                }
                BASE_MESSAGE_TYPE.VI == baseMessageType -> {
                    binding.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_inbox_video,
                        0,
                        0,
                        0
                    )
                    binding.tvLastMessage.compoundDrawablePadding =
                        Utils.dpToPx(getAppContext(), drawablePadding)
                    binding.tvLastMessage.text = "Video"

                }
                BASE_MESSAGE_TYPE.PD == baseMessageType -> {
                    binding.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_inbox_pdf,
                        0,
                        0,
                        0
                    )
                    binding.tvLastMessage.compoundDrawablePadding =
                        Utils.dpToPx(getAppContext(), drawablePadding)
                    binding.tvLastMessage.text = "Pdf"
                }
            }
        }

        private fun onClickView(inboxEntity: InboxEntity) {
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
}





