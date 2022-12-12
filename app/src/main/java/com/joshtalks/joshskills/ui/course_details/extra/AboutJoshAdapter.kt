package com.joshtalks.joshskills.ui.course_details.extra

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.LayoutAboutJoshCardBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ImageShowEvent
import com.joshtalks.joshskills.repository.server.course_detail.Detail
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

class AboutJoshAdapter(val details: List<Detail>) : RecyclerView.Adapter<AboutJoshAdapter.AboutJoshCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AboutJoshCardViewHolder {
        val binding = LayoutAboutJoshCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AboutJoshCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AboutJoshCardViewHolder, position: Int) {
        holder.bind(details[position])

        holder.item.image.setOnClickListener {
            RxBus2.publish(ImageShowEvent(details[position].imageUrl, details[position].imageUrl))
        }
    }

    override fun getItemCount() = details.size

    class AboutJoshCardViewHolder(val item: LayoutAboutJoshCardBinding) : RecyclerView.ViewHolder(item.root) {
        fun bind(detail: Detail) {
            item.title.text = detail.title
            item.description.text = detail.description
            setImageView(detail.imageUrl)
        }

        private fun setImageView(url: String) {
            val multi = MultiTransformation(
                RoundedCornersTransformation(
                    Utils.dpToPx(4),
                    0,
                    RoundedCornersTransformation.CornerType.ALL
                )
            )
            Glide.with(itemView)
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.bitmapTransform(multi))
                .into(item.image)
        }
    }
}