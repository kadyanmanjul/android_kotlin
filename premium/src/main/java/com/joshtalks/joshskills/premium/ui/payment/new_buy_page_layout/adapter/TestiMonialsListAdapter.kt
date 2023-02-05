package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.databinding.ItemTestimonalVideoBinding
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.TestimonialVideo
import com.joshtalks.joshskills.premium.ui.special_practice.utils.CLICK_ON_TESTIMONIALS_VIDEO
import com.joshtalks.joshskills.premium.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class TestiMonialsListAdapter(var videoList: List<TestimonialVideo>? = listOf()) : RecyclerView.Adapter<TestiMonialsListAdapter.TestiMonialsViewHolder>() {
    var itemClick: ((TestimonialVideo, Int, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestiMonialsViewHolder {
        val binding  = ItemTestimonalVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TestiMonialsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TestiMonialsViewHolder, position: Int) {
        holder.setData(videoList?.get(position), position)
    }

    override fun getItemCount(): Int = videoList?.size ?:0

    fun addVideoList(members: List<TestimonialVideo>?) {
        videoList = members
        Log.e("sagar", "addVideoList: $videoList", )
        notifyDataSetChanged()
    }

    fun setListener(function: ((TestimonialVideo, Int, Int) -> Unit)?) {
        itemClick = function
    }

    inner class TestiMonialsViewHolder(private val binding: ItemTestimonalVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(testimonialVideo: TestimonialVideo?,position: Int) {
            Log.e("sagar", "addVideoList: $testimonialVideo", )
            with(binding) {
                setImageView(testimonialVideo?.thumbnailUrl ?: EMPTY, this.imageView)
                this.playIcon.setOnClickListener {
                    if (testimonialVideo != null)
                        itemClick?.invoke(testimonialVideo, CLICK_ON_TESTIMONIALS_VIDEO, position)
                }
            }
        }

        private fun setImageView(url: String, imageView: ImageView) {
            val multi = MultiTransformation(
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
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .apply(RequestOptions.bitmapTransform(multi))
                .into(imageView)
        }
    }
}