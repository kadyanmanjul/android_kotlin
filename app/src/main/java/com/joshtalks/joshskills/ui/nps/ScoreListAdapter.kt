package com.joshtalks.joshskills.ui.nps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ScoreItemLayoutBinding

class ScoreListAdapter(context: NetPromoterScoreFragment) :
    RecyclerView.Adapter<ScoreListAdapter.ViewHolder>() {
    private val ratingNumbers = Array(11) { i -> i * 1 }
    private var lastSelected = -1
    private var onRatingSelectListener: OnRatingSelectListener = context


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ScoreItemLayoutBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = ratingNumbers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(ratingNumbers[position])

    inner class ViewHolder(val binding: ScoreItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(number: Int) {
            binding.tvNumber.text = number.toString()
            binding.tvNumber.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.ca_color
                )
            )
            binding.tvNumber.setBackgroundResource(R.drawable.score_unselect_drawable)
            binding.tvNumber.setOnClickListener {
                if (lastSelected == number) {
                    return@setOnClickListener
                }
                notifyItemChanged(lastSelected)
                binding.tvNumber.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.white
                    )
                )
                binding.tvNumber.setBackgroundResource(R.drawable.score_select_drawable)
                lastSelected = number
                onRatingSelectListener.onSelectRating(lastSelected)
            }

        }
    }

    interface OnRatingSelectListener {
        fun onSelectRating(rating: Int)
    }
}
