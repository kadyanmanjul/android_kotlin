package com.joshtalks.joshskills.explore.course_details.adapters

import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.github.twocoffeesoneteam.glidetovectoryou.GlideToVectorYou
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.databinding.FaqCategoryItemLayoutBinding
import com.joshtalks.joshskills.common.repository.server.FAQCategory

class MasterFaqAdapter(val listData: List<FAQCategory>) : RecyclerView.Adapter<MasterFaqAdapter.MasterFaqViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MasterFaqViewHolder {
        val binding = FaqCategoryItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MasterFaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MasterFaqViewHolder, position: Int) {
        holder.bind(listData.get(position))
    }

    override fun getItemCount() = listData.size

    class MasterFaqViewHolder(val item: FaqCategoryItemLayoutBinding) :
        RecyclerView.ViewHolder(item.root) {

        fun bind(faqCategory: FAQCategory) {
            item.tvCategoryName.text = faqCategory.categoryName
            if (faqCategory.iconUrl.endsWith(".svg")) {

                val requestBuilder = GlideToVectorYou
                    .init()
                    .with(AppObjectController.joshApplication)
                    .requestBuilder

                requestBuilder.load(faqCategory.iconUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions().centerCrop())
                    .listener(object : RequestListener<PictureDrawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<PictureDrawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: PictureDrawable?,
                            model: Any?,
                            target: Target<PictureDrawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            resource?.let {
                                AppObjectController.uiHandler.post { item.ivCategoryIcon.setImageDrawable(it) }
                            }
                            return false
                        }

                    }).submit()

            } else {
                item.ivCategoryIcon.setImage(faqCategory.iconUrl)
            }
        }
    }
}