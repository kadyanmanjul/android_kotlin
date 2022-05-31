package com.joshtalks.joshskills.ui.voip.new_arch.ui.views.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.TopicImageViewpagerItemBinding

class TopicImageAdapter(val imageList:ArrayList<String> ,val context:Context):RecyclerView.Adapter<TopicImageAdapter.TopicImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val topicImageBinding =TopicImageViewpagerItemBinding.inflate(inflater, parent, false)
        return TopicImageViewHolder(topicImageBinding)
    }

    override fun onBindViewHolder(holder: TopicImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int {
       return imageList.size
    }

    inner class TopicImageViewHolder(val binding: TopicImageViewpagerItemBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(imageUrl: String) {
            Glide.with(context)
                .load(imageUrl)
                .error(R.drawable.ic_img_loading_error)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(p0: GlideException?, p1: Any?, p2: Target<Drawable>?, p3: Boolean): Boolean {
                        binding.progress.visibility = View.GONE
                        return false
                    }
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.progress.visibility = View.GONE
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(binding.topicImage)
        }
    }
}