package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.ItemRatingAndReviewBinding
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.RatingAndReviewsList

class RatingAndReviewsAdapter(var amountList: List<RatingAndReviewsList>? = listOf()) :
    RecyclerView.Adapter<RatingAndReviewsAdapter.RatingAndReviewViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RatingAndReviewsAdapter.RatingAndReviewViewHolder {
        val binding = ItemRatingAndReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RatingAndReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RatingAndReviewsAdapter.RatingAndReviewViewHolder, position: Int) {
        holder.setData(amountList?.get(position))
    }

    override fun getItemCount(): Int = amountList?.size ?: 0

    fun addRatingList(members: List<RatingAndReviewsList>?) {
        amountList = members
        notifyDataSetChanged()
    }

    inner class RatingAndReviewViewHolder(private val binding: ItemRatingAndReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(name: RatingAndReviewsList?) {
            Log.e("sagar", "setData: ${name}")
            with(binding) {
                this.tvName.text = name?.userName
                this.courseRating.rating = name?.rating ?: 0.0f
                this.ratingDate.text = name?.createdDate
                this.ratingDesc.text = name?.description
                this.profileImage.setUserImageOrInitials(null, name?.userName ?: EMPTY, isRound = true)
            }
        }
    }
}