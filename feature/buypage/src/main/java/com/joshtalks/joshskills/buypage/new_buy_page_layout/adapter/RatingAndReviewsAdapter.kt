package com.joshtalks.joshskills.buypage.new_buy_page_layout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.buypage.R
import com.joshtalks.joshskills.buypage.databinding.ItemRatingAndReviewBinding
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.buypage.new_buy_page_layout.model.ReviewItem

class RatingAndReviewsAdapter(diffUtil: DiffUtil.ItemCallback<ReviewItem>) :
    PagingDataAdapter<ReviewItem, RatingAndReviewsAdapter.ReviewViewHolder>(diffUtil) {

    inner class ReviewViewHolder(val item: ItemRatingAndReviewBinding): RecyclerView.ViewHolder(item.root) {
        fun onBind(data: ReviewItem) {
            item.itemData = data
            with(item) {
                this.profileImage.setUserImageOrInitials(null, data.userName, isRound = true)
            }
        }
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        getItem(position)?.let { holder.onBind(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = DataBindingUtil.inflate<ItemRatingAndReviewBinding>(LayoutInflater.from(parent.context), R.layout.item_rating_and_review, parent, false)
        return ReviewViewHolder(view)
    }
}