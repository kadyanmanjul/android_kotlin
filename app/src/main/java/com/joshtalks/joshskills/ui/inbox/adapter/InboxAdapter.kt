package com.joshtalks.joshskills.ui.inbox.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_FREE_TRIAL
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.YYYY_MM_DD
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.interfaces.OnOpenCourseListener
import com.joshtalks.joshskills.databinding.InboxItemLayoutBinding
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import timber.log.Timber

class InboxAdapter(
    private var lifecycleProvider: LifecycleOwner,
    private val openCourseListener: OnOpenCourseListener
) :
    RecyclerView.Adapter<InboxAdapter.InboxViewHolder>() {
    private var items: ArrayList<InboxEntity> = arrayListOf()
    var drawablePadding: Float = 2f
    var progressBarStatus: Boolean = true

    init {
        progressBarStatus = AppObjectController.getFirebaseRemoteConfig()
            .getBoolean(FirebaseRemoteConfigKey.INBOX_SCREEN_COURSE_PROGRESS)
        setHasStableIds(true)
    }

    fun getAppContext() = AppObjectController.joshApplication

    private fun getDrawablePadding() = Utils.dpToPx(getAppContext(), 4f)

    fun addItems(newList: List<InboxEntity>) {
        if (newList.isEmpty()) {
            return
        }
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
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

    override fun getItemId(position: Int): Long {
        return items[position].courseId.toLong()
    }

    inner class InboxViewHolder(val binding: InboxItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var countdownTimerBack: CountdownTimerBack? = null

        fun bind(inboxEntity: InboxEntity, indexPos: Int) {
            with(binding) {
                obj = inboxEntity
                tvLastMessageTime.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                tvName.text = inboxEntity.course_name
                courseProgressBar.progress = 0
                horizontalLine.visibility = android.view.View.VISIBLE
                imageUrl(binding.profileImage, inboxEntity.course_icon)
                if (PrefManager.getBoolValue(IS_FREE_TRIAL) && inboxEntity.created == null && inboxEntity.isCapsuleCourse) {
                    unseenMsgCount.visibility = ViewGroup.VISIBLE
                    unseenMsgCount.text = "3"
                } else {
                    unseenMsgCount.visibility = ViewGroup.GONE
                }
                //   profileImage.setInboxImageView(inboxEntity.course_icon)
                /* if (inboxEntity.chat_id.isNullOrEmpty()) {
                     tvLastMessageTime.setCompoundDrawablesWithIntrinsicBounds(
                         R.drawable.ic_unread,
                         0,
                         0,
                         0
                     )
                 }*/

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

                        inboxEntity.duration.run {
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
                        if (BASE_MESSAGE_TYPE.Q == it) {
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
                if ((itemCount - 1) == bindingAdapterPosition || (itemCount - 1) == layoutPosition) {
                    horizontalLine.visibility = android.view.View.GONE
                }
                freeTrialTimer.visibility = View.INVISIBLE
                if (inboxEntity.isCourseBought) {
                    freeTrialTimer.visibility = View.INVISIBLE
                    tvLastMessage.visibility = View.VISIBLE
                } else if (inboxEntity.isCourseLocked) {
                    freeTrialTimer.visibility = View.VISIBLE
                    tvLastMessage.visibility = View.INVISIBLE
                    freeTrialTimer.text = getAppContext().getString(R.string.free_trial_ended)
                    countdownTimerBack?.stop()
                } else if (inboxEntity.expiryDate != null && inboxEntity.isCourseBought.not()) {
                    freeTrialTimer.visibility = View.VISIBLE
                    tvLastMessage.visibility = View.INVISIBLE
                    if (inboxEntity.expiryDate.time <= System.currentTimeMillis()) {
                        freeTrialTimer.text = getAppContext().getString(R.string.free_trial_ended)
                        countdownTimerBack?.stop()
                    } else {
                        startTimer(
                            (inboxEntity.expiryDate.time - System.currentTimeMillis()).times(1000),
                            freeTrialTimer
                        )
                    }
                } else {
                    freeTrialTimer.visibility = View.INVISIBLE
                    tvLastMessage.visibility = View.VISIBLE
                }
            }
        }

        private fun startTimer(startTimeInMilliSeconds: Long, freeTrialTimer: AppCompatTextView) {
            countdownTimerBack?.stop()
            countdownTimerBack = null
            countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
                override fun onTimerTick(millis: Long) {
                    AppObjectController.uiHandler.post {
                        freeTrialTimer.text = getAppContext().getString(
                            R.string.free_trial_end_in,
                            UtilTime.timeFormatted(millis)
                        )
                    }
                }

                override fun onTimerFinish() {
                    freeTrialTimer.text = getAppContext().getString(R.string.free_trial_ended)
                    countdownTimerBack?.stop()
                }
            }
            countdownTimerBack?.startTimer()
        }

        fun imageUrl(imageView: ImageView, url: String?) {
            if (url.isNullOrEmpty()) {
                imageView.setImageResource(R.drawable.ic_josh_course)
                return
            }

            val multi = MultiTransformation(
                CropTransformation(
                    Utils.dpToPx(48),
                    Utils.dpToPx(48),
                    CropTransformation.CropType.CENTER
                ),
                RoundedCornersTransformation(
                    Utils.dpToPx(ROUND_CORNER),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )
            Glide.with(AppObjectController.joshApplication)
                .load(url)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(
                    RequestOptions.bitmapTransform(multi).apply(
                        RequestOptions().placeholder(R.drawable.ic_josh_course)
                            .error(R.drawable.ic_josh_course)
                    )

                )
                .into(imageView)
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
                BASE_MESSAGE_TYPE.LESSON == baseMessageType -> {
                    binding.tvLastMessage.compoundDrawablePadding =
                        Utils.dpToPx(getAppContext(), drawablePadding)
                    binding.tvLastMessage.text = "Lesson ${inboxEntity.lessonNo}"
                }
                BASE_MESSAGE_TYPE.BEST_PERFORMER == baseMessageType -> {
                    binding.tvLastMessage.compoundDrawablePadding =
                        Utils.dpToPx(getAppContext(), drawablePadding)
                    binding.tvLastMessage.text = " Student of the Day"
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
            openCourseListener.onClick(inboxEntity)
            //   RxBus2.publish(OpenCourseEventBus(inboxEntity))
        }
    }
}
