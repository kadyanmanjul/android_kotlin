package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.IconMarginSpan
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent
import com.joshtalks.joshskills.repository.local.eventbus.VideoShowEvent
import com.joshtalks.joshskills.repository.server.course_detail.CARD_STATE
import com.joshtalks.joshskills.repository.server.course_detail.CourseDetailsResponse
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import io.github.inflationx.calligraphy3.TypefaceUtils
import jp.wasabeef.glide.transformations.RoundedCornersTransformation


const val MIN_LINES = 4

@Layout(R.layout.course_details_data_view_holder)
class CourseDetailDataViewHeader(
    private var courseDetails: CourseDetailsResponse,
    private val context: Context = AppObjectController.joshApplication
) {

    @com.mindorks.placeholderview.annotations.View(R.id.tv_detail)
    lateinit var detailsTV: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text_title)
    lateinit var titleTV: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text_read_more)
    lateinit var readMoreTV: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.video_container_fl)
    lateinit var videoContainerView: View

    @com.mindorks.placeholderview.annotations.View(R.id.btn_play_view)
    lateinit var btnPlayView: View

    @com.mindorks.placeholderview.annotations.View(R.id.iv_expand)
    lateinit var expandIV: AppCompatImageView


    @com.mindorks.placeholderview.annotations.View(R.id.multi_line_ll)
    lateinit var multiLineLL: LinearLayout


    @com.mindorks.placeholderview.annotations.View(R.id.image_view)
    lateinit var placeholderImageView: AppCompatImageView
    private val typefaceSpan = TypefaceUtils.load(
        context.assets,
        "fonts/Roboto-Regular.ttf"
    )


    var expand = false

    @Resolve
    fun onResolved() {
        detailsTV.maxLines = Integer.MAX_VALUE
        if (courseDetails.title.isNullOrEmpty().not()) {
            titleTV.text = courseDetails.title
            titleTV.visibility = View.VISIBLE
        }
        if (courseDetails.description.isNullOrEmpty().not()) {
            if (courseDetails.cardState == CARD_STATE.ML) {
                multiLineLL.visibility = View.VISIBLE
                val stringList = courseDetails.description!!.split("~")
                stringList.forEach {
                    multiLineLL.addView(getTextView(it))
                }
            } else {
                detailsTV.text =
                    HtmlCompat.fromHtml(
                        courseDetails.description!!,
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                detailsTV.visibility = View.VISIBLE
                detailsTV.setLineSpacing(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        2.0f,
                        context.resources.displayMetrics
                    ), 1.0f
                )
            }
        }
        when (courseDetails.cardState) {
            CARD_STATE.NM -> {

            }
            CARD_STATE.CO -> {
                expandIV.visibility = View.VISIBLE
                detailsTV.visibility = View.GONE
                expandIV.setImageResource(R.drawable.ic_expand)
            }
            CARD_STATE.UCO -> {
                expand = true
                expandIV.visibility = View.VISIBLE
                expandIV.setImageResource(R.drawable.ic_remove_expand)
                detailsTV.visibility = View.VISIBLE


            }
            CARD_STATE.SM -> {
                readMoreTV.visibility = View.VISIBLE
                detailsTV.maxLines = MIN_LINES
            }
        }


        if (courseDetails.type == BASE_MESSAGE_TYPE.TX) {
            videoContainerView.visibility = View.GONE

        } else {
            videoContainerView.visibility = View.VISIBLE
            if (courseDetails.type == BASE_MESSAGE_TYPE.IM) {
                btnPlayView.visibility = View.GONE
                setImageView(courseDetails.url)
            } else if (courseDetails.type == BASE_MESSAGE_TYPE.VI) {
                btnPlayView.visibility = View.VISIBLE
                setImageView(courseDetails.thumbnail)
            }
        }

    }

    private fun setImageView(url: String) {
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )

        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .apply(RequestOptions.bitmapTransform(multi))
            .into(placeholderImageView)
    }

    @Click(R.id.text_read_more)
    fun onClick() {
        if (detailsTV.maxLines == MIN_LINES) {
            detailsTV.maxLines = Integer.MAX_VALUE
            readMoreTV.text = context.getString(R.string.show_less)
        } else {
            detailsTV.maxLines = MIN_LINES
            readMoreTV.text = context.getString(R.string.show_more)

        }

    }


    @Click(R.id.video_container_fl)
    fun openVideo() {
        if (courseDetails.type == BASE_MESSAGE_TYPE.IM) {
            RxBus2.publish(ImageShowEvent(null, courseDetails.url, null))
        } else if (courseDetails.type == BASE_MESSAGE_TYPE.VI) {
            RxBus2.publish(VideoShowEvent(courseDetails.url))
        }
    }

    @Click(R.id.iv_expand)
    fun onClickCollapseView() {
        expand = if (expand) {
            expandIV.setImageResource(R.drawable.ic_expand)
            detailsTV.visibility = View.GONE
            AppAnalytics.create(AnalyticsEvent.COURSE_DATA_EXPANDED.NAME)
                .addUserDetails()
                .addParam(AnalyticsEvent.COURSE_NAME.NAME, courseDetails.title?.toString() ?: EMPTY)
                .addParam(AnalyticsEvent.MEDIA_TYPE.NAME, courseDetails.type.toString())
                .push()
            false
        } else {
            detailsTV.visibility = View.VISIBLE
            expandIV.setImageResource(R.drawable.ic_remove_expand)
            AppAnalytics.create(AnalyticsEvent.COURSE_DATA_CONTRACTED.NAME)
                .addUserDetails()
                .addParam(AnalyticsEvent.COURSE_NAME.NAME, courseDetails.title?.toString() ?: EMPTY)
                .addParam(AnalyticsEvent.MEDIA_TYPE.NAME, courseDetails.type.toString())
                .push()
            true
        }
    }

    private fun getTextView(text: String): TextView {
        val textView = TextView(context)
        textView.setTextColor(ContextCompat.getColor(context, R.color.gray_48))
        textView.typeface = typefaceSpan
        val spanString = SpannableString(text)
        spanString.setSpan(
            IconMarginSpan(
                Utils.getBitmapFromVectorDrawable(context, R.drawable.ic_small_tick),
                22
            ), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spanString
        textView.setPadding(0, 2, 0, 2)
        textView.setLineSpacing(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2.0f,
                context.resources.displayMetrics
            ), 1.0f
        )
        return textView

    }


}