package com.joshtalks.joshskills.ui.view_holders

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.YYYY_MM_DD
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.eventbus.OpenCourseEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import jp.wasabeef.glide.transformations.CropTransformation
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import timber.log.Timber
import java.util.Date
import java.util.Locale

@Layout(R.layout.inbox_row_layout)
class InboxViewHolder(
    private var inboxEntity: InboxEntity,
    private val totalItem: Int,
    private val indexPos: Int
) {
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

    @View(R.id.tv_last_message)
    lateinit var tvLastReceivedMessage: AppCompatTextView


    @JvmField
    var drawablePadding: Float = 2f

    @JvmField
    var progressBarStatus: Boolean = true

    init {
        progressBarStatus = AppObjectController.getFirebaseRemoteConfig()
            .getBoolean(FirebaseRemoteConfigKey.INBOX_SCREEN_COURSE_PROGRESS)
    }

    fun getAppContext() = AppObjectController.joshApplication

    private fun getDrawablePadding() = Utils.dpToPx(getAppContext(), 4f)

    @Resolve
    fun onResolved() {
        tvName.text = inboxEntity.course_name
        courseProgressBar.progress = 0
        if (inboxEntity.course_icon.isNullOrEmpty()) {
            profileImage.setImageResource(R.drawable.ic_josh_course)
        } else {
            inboxEntity.course_icon?.let {
                profileImage.setInboxImageView(it)
            }
        }
        if (inboxEntity.chat_id.isNullOrEmpty()) {
            tvLastReceivedMessageTime.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_unread,
                0,
                0,
                0
            )
        }

        if ((totalItem - 1) == indexPos) {
            hLine.visibility = android.view.View.GONE
        }
        if (progressBarStatus) {
            courseProgressBar.visibility = android.view.View.VISIBLE
            tvLastReceivedMessage.visibility = android.view.View.GONE
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
            tvLastReceivedMessage.visibility = android.view.View.VISIBLE
            inboxEntity.type?.let {
                if (BASE_MESSAGE_TYPE.Q == it || BASE_MESSAGE_TYPE.AR == it) {
                    inboxEntity.material_type?.let { messageType ->
                        showRecentAsPerView(messageType)
                    }
                } else {
                    showRecentAsPerView(it)
                }
            }
        }
        inboxEntity.created?.run {
            tvLastReceivedMessageTime.text = Utils.getMessageTime(this)
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
                    Utils.dpToPx(getAppContext(), drawablePadding)
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
                    Utils.dpToPx(getAppContext(), drawablePadding)
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
                    Utils.dpToPx(getAppContext(), drawablePadding)
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
                    Utils.dpToPx(getAppContext(), drawablePadding)
                tvLastReceivedMessage.text = "Pdf"

            }
        }
    }

    private fun setUpProgressAnimate(diff: Int) {
        val animation: ObjectAnimator =
            ObjectAnimator.ofInt(courseProgressBar, "progress", 0, diff)
        animation.startDelay = 0
        animation.duration = 700
        animation.interpolator = AccelerateDecelerateInterpolator()
        animation.start()
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


    private fun ImageView.setInboxImageView(url: String) {
        val multi = MultiTransformation(
            CropTransformation(
                Utils.dpToPx(240),
                Utils.dpToPx(240),
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
            .apply(RequestOptions.bitmapTransform(multi))
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false

                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            })
            .into(this)
    }
}