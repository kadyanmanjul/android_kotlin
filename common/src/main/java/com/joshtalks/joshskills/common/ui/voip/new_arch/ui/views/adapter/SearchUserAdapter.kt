package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.databinding.ItemSearchingReasonBinding
import com.joshtalks.joshskills.common.databinding.TopicImageViewpagerItemBinding
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.models.*

class SearchUserAdapter(var items : List<SearchingItem> = listOf(), val context:Context):RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        Log.d("Bhaskar", "onCreateViewHolder :")
        val inflater = LayoutInflater.from(parent.context)
        val itemBinding =ItemSearchingReasonBinding.inflate(inflater, parent, false)
        return SearchUserViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
       return items.size
    }

    inner class SearchUserViewHolder(val binding: ItemSearchingReasonBinding):RecyclerView.ViewHolder(binding.root){
        fun bind(item: SearchingItem) {
            Log.d("Bhaskar", "SearchUserViewHolder : $item")
                Glide.with(context)
                    .load(item.icon)
                    .error(R.drawable.ic_img_loading_error)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(binding.reasonImage)

            binding.tvReasonTitle.text = item.title
            when(item) {
                is Rules -> {
                    binding.tvReasonDesc.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    binding.tvReasonOptional.visibility = View.GONE
                    binding.tvReasonDesc.text = item.content
                }
                is Tips -> {
                    binding.tvReasonDesc.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_bullet, 0, 0, 0)
                    binding.tvReasonOptional.visibility = View.VISIBLE
                    binding.tvReasonDesc.text = item.content.first()
                    binding.tvReasonOptional.text = item.content.last()
                }
            }
        }
    }
}