package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.common.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.core.custom_ui.JoshRatingBar
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.CourseOverviewData
import com.joshtalks.joshskills.common.repository.server.course_detail.OverviewMediaType
import com.joshtalks.joshskills.common.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve



class CourseOverviewViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private val data: CourseOverviewData,
    private val context: Context = AppObjectController.joshApplication,
    private val testId: Int,
    private val coursePrice: String,
    private val courseName: String
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var txtCourseName: JoshTextView

    
    lateinit var txtTeacherName: JoshTextView

    
    lateinit var txtViewers: JoshTextView

    
    lateinit var txtDescription: JoshTextView

    
    lateinit var imgTopIcon: AppCompatImageView

    
    lateinit var txtRating: JoshTextView

    
    lateinit var ratingBar: JoshRatingBar

    
    lateinit var statsIcon1: AppCompatImageView

    
    lateinit var statsCaption1: JoshTextView

    
    lateinit var statsIcon2: AppCompatImageView

    
    lateinit var statsCaption2: JoshTextView

    
    lateinit var statsIcon3: AppCompatImageView

    
    lateinit var statsCaption3: JoshTextView

    
    lateinit var statsIcon4: AppCompatImageView

    
    lateinit var statsCaption4: JoshTextView

    
    lateinit var carouselRecyclerView: PlaceHolderView

    @Resolve
    fun onResolved() {
        txtCourseName.text = data.courseName
        txtTeacherName.text = data.teacherName
        txtViewers.text = data.viewerText
        txtDescription.text = data.shortDescription
        txtRating.text = String.format("%.1f", data.rating)
        ratingBar.rating = data.rating.toFloat()
        if (data.topIconUrl.isNullOrBlank().not()) {
            setImage(data.topIconUrl!!, imgTopIcon)
            imgTopIcon.visibility = View.VISIBLE
        }
        setCourseStats()
        setCarouselView()
    }

    private fun setCourseStats() {
        val statsList =
            data.media.filter { it.type == OverviewMediaType.ICON }.sortedBy { it.sortOrder }
        if (statsList.isNotEmpty()) {
            statsIcon1.visibility = View.VISIBLE
            statsCaption1.visibility = View.VISIBLE
            statsList[0].url?.run {
                setImage(this, statsIcon1)
            }
            statsCaption1.text = statsList[0].text
        }
        if (statsList.size > 1) {
            statsIcon2.visibility = View.VISIBLE
            statsCaption2.visibility = View.VISIBLE
            statsList[1].url?.run {
                setImage(this, statsIcon2)
            }
            statsCaption2.text = statsList[1].text
        }
        if (statsList.size > 2) {
            statsIcon3.visibility = View.VISIBLE
            statsCaption3.visibility = View.VISIBLE
            statsList[2].url?.run {
                setImage(this, statsIcon3)
            }
            statsCaption3.text = statsList[2].text
        }
        if (statsList.size > 3) {
            statsIcon4.visibility = View.VISIBLE
            statsCaption4.visibility = View.VISIBLE
            statsList[3].url?.run {
                setImage(this, statsIcon4)
            }
            statsCaption4.text = statsList[3].text
        }
    }

    private fun setImage(url: String, imageView: ImageView) {
        Glide.with(context)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .fitCenter()
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(imageView)
    }

    private fun setCarouselView() {
        val linearLayoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        carouselRecyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)

        if (carouselRecyclerView.itemDecorationCount < 1) {
            val cardWidthPixels = (context.resources.displayMetrics.widthPixels * 0.90f).toInt()
            val cardHintPercent = 0.01f
            carouselRecyclerView.addItemDecoration(
                RecyclerViewCarouselItemDecorator(
                    context,
                    cardWidthPixels,
                    cardHintPercent
                )
            )
        }

        carouselRecyclerView.itemAnimator = null
        data.media.filter { it.type == OverviewMediaType.IMAGE || it.type == OverviewMediaType.VIDEO }
            .sortedBy { it.sortOrder }
            .forEach {
                carouselRecyclerView.addView(
                    CourseOverviewMediaViewHolder(
                        it
                    )
                )
            }
    }

    
    fun onClickRatingView() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(CardType.REVIEWS)

        MixPanelTracker.publishEvent(MixPanelEvent.COURSE_VIEW_RATING)
            .addParam(ParamKeys.TEST_ID,testId)
            .addParam(ParamKeys.COURSE_NAME,courseName)
            .addParam(ParamKeys.COURSE_PRICE,coursePrice)
            .addParam(ParamKeys.COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
            .push()
    }
}
