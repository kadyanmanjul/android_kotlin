package com.joshtalks.joshskills.ui.explore.v2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.RecommendedItemViewHolderV2Binding
import com.joshtalks.joshskills.repository.server.CourseExploreModel

class RecommendedItemAdapter(private var courseList: List<CourseExploreModel>) :
    RecyclerView.Adapter<RecommendedItemAdapter.RecommendItemViewHolder>() {
    private val context = AppObjectController.joshApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecommendedItemViewHolderV2Binding.inflate(inflater, parent, false)
        return RecommendItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return courseList.size
    }

    override fun getItemViewType(position: Int): Int {
        return courseList[position].cardType.ordinal
    }


    override fun onBindViewHolder(holder: RecommendItemViewHolder, position: Int) {
        holder.bind(courseList[position])
    }

    inner class RecommendItemViewHolder(val binding: RecommendedItemViewHolderV2Binding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(courseExploreModel: CourseExploreModel) {
            with(binding) {
                rootView.setOnClickListener {

                }
                Glide.with(context)
                    .load(courseExploreModel.imageUrl)
                    .override(Utils.dpToPx(280), Utils.dpToPx(240))
                    .into(imageView)
            }
        }
    }

}
