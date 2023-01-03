package com.joshtalks.joshskills.explore.course_details.viewholder

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.OfferCardEvent
import com.joshtalks.joshskills.explore.course_details.adapters.CourseOverviewAdapter
import com.joshtalks.joshskills.explore.course_details.models.CardType
import com.joshtalks.joshskills.explore.course_details.models.CourseOverviewData
import com.joshtalks.joshskills.explore.course_details.models.OverviewMediaType
import com.joshtalks.joshskills.explore.course_details.models.RecyclerViewCarouselItemDecorator
import com.joshtalks.joshskills.explore.databinding.CourseOverviewViewHolderBinding

class CourseOverviewViewHolder(
    val item: CourseOverviewViewHolderBinding,
    val testId: Int
) : DetailsBaseViewHolder(item) {

    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            CourseOverviewData::class.java
        )
        item.txtCourseName.text = data.courseName
        item.txtTeacherName.text = data.teacherName
        item.txtViewers.text = data.viewerText
        item.txtDescription.text = data.shortDescription
        item.txtRating.text = String.format("%.1f", data.rating)
        item.ratingBar.rating = data.rating.toFloat()

        val isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT)

        if (testId == 10 || testId == 122 || !isCourseBought || PrefManager.getBoolValue(IS_SUBSCRIPTION_STARTED))
            item.cardOffer.visibility = View.GONE

        setCourseStats(data)
        setCarouselView(data)

        item.cardOffer.setOnClickListener {
            RxBus2.publish(OfferCardEvent(testId))
        }

        item.ratingView.setOnClickListener {
            RxBus2.publish(CardType.REVIEWS)
        }
    }

    private fun setCourseStats(data: CourseOverviewData) {
        val statsList = data.media.filter { it.type == OverviewMediaType.ICON }.sortedBy { it.sortOrder }
        if (statsList.isNotEmpty()) {
            item.icon1.visibility = View.VISIBLE
            item.captionIcon1.visibility = View.VISIBLE
            statsList[0].url?.run {
                setImage(this, item.icon1)
            }
            item.captionIcon1.text = statsList[0].text
        }
        if (statsList.size > 1) {
            item.icon2.visibility = View.VISIBLE
            item.captionIcon2.visibility = View.VISIBLE
            statsList[1].url?.run {
                setImage(this, item.icon2)
            }
            item.captionIcon2.text = statsList[1].text
        }
        if (statsList.size > 2) {
            item.icon3.visibility = View.VISIBLE
            item.captionIcon3.visibility = View.VISIBLE
            statsList[2].url?.run {
                setImage(this, item.icon3)
            }
            item.captionIcon3.text = statsList[2].text
        }
        if (statsList.size > 3) {
            item.icon4.visibility = View.VISIBLE
            item.captionIcon4.visibility = View.VISIBLE
            statsList[3].url?.run {
                setImage(this, item.icon4)
            }
            item.captionIcon4.text = statsList[3].text
        }
    }

    private fun setImage(url: String, imageView: ImageView) {
        Glide.with(getAppContext())
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .fitCenter()
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(imageView)
    }

    private fun setCarouselView(data: CourseOverviewData) {
        val linearLayoutManager = LinearLayoutManager(
            getAppContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        linearLayoutManager.isSmoothScrollbarEnabled = true
        item.carouselRecyclerView.setHasFixedSize(true)
        item.carouselRecyclerView.layoutManager = linearLayoutManager

        if (item.carouselRecyclerView.itemDecorationCount < 1) {
            val cardWidthPixels = (getAppContext().resources.displayMetrics.widthPixels * 0.90f).toInt()
            val cardHintPercent = 0.01f
            item.carouselRecyclerView.addItemDecoration(
                RecyclerViewCarouselItemDecorator(
                    getAppContext(),
                    cardWidthPixels,
                    cardHintPercent
                )
            )
        }

        item.carouselRecyclerView.itemAnimator = null
        item.carouselRecyclerView.adapter =
            CourseOverviewAdapter(
                data.media
                    .filter { it.type == OverviewMediaType.IMAGE || it.type == OverviewMediaType.VIDEO }
                    .sortedBy { it.sortOrder }
            )
    }
}
