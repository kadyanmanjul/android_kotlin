package com.joshtalks.joshskills.ui.course_details.view_holders

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
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.JoshRatingBar
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.CourseOverviewData
import com.joshtalks.joshskills.repository.server.course_detail.OverviewMediaType
import com.joshtalks.joshskills.repository.server.course_detail.RecyclerViewCarouselItemDecorator
import com.mindorks.placeholderview.PlaceHolderView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.course_overview_view_holder)
class CourseOverviewViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private val data: CourseOverviewData,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.txtCourseName)
    lateinit var txtCourseName: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.txtTeacherName)
    lateinit var txtTeacherName: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.txtViewers)
    lateinit var txtViewers: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.txtDescription)
    lateinit var txtDescription: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.img_top_icon)
    lateinit var imgTopIcon: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.txtRating)
    lateinit var txtRating: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.rating_bar)
    lateinit var ratingBar: JoshRatingBar

    @com.mindorks.placeholderview.annotations.View(R.id.icon1)
    lateinit var statsIcon1: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.captionIcon1)
    lateinit var statsCaption1: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.icon2)
    lateinit var statsIcon2: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.captionIcon2)
    lateinit var statsCaption2: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.icon3)
    lateinit var statsIcon3: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.captionIcon3)
    lateinit var statsCaption3: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.icon4)
    lateinit var statsIcon4: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.captionIcon4)
    lateinit var statsCaption4: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.carousel_recycler_view)
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
            setImage(statsList[0].url, statsIcon1)
            statsCaption1.text = statsList[0].text
        }
        if (statsList.size > 1) {
            statsIcon2.visibility = View.VISIBLE
            statsCaption2.visibility = View.VISIBLE
            setImage(statsList[1].url, statsIcon2)
            statsCaption2.text = statsList[1].text
        }
        if (statsList.size > 2) {
            statsIcon3.visibility = View.VISIBLE
            statsCaption3.visibility = View.VISIBLE
            setImage(statsList[2].url, statsIcon3)
            statsCaption3.text = statsList[2].text
        }
        if (statsList.size > 3) {
            statsIcon4.visibility = View.VISIBLE
            statsCaption4.visibility = View.VISIBLE
            setImage(statsList[3].url, statsIcon4)
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

    @Click(R.id.ratingView)
    fun onClickRatingView() {
        RxBus2.publish(CardType.REVIEWS)
    }
}
