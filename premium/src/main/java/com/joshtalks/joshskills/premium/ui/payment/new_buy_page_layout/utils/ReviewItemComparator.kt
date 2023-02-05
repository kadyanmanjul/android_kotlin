package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.utils

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.ReviewItem

object ReviewItemComparator: DiffUtil.ItemCallback<ReviewItem>() {
    override fun areItemsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean {
        return oldItem.createdDate == newItem.createdDate &&
                oldItem.description == newItem.description &&
                oldItem.userName == newItem.userName &&
                oldItem.rating == newItem.rating
    }
}