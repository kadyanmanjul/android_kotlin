package com.joshtalks.joshskills.ui.feedback

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FeedbackOptionViewBinding
import com.joshtalks.joshskills.repository.server.feedback.RatingModel


class FeedbackOptionAdapter(context: FeedbackFragment, private var items: List<RatingModel>) :
    RecyclerView.Adapter<FeedbackOptionAdapter.ViewHolder>() {
    private var onFeedbackItemListener: OnFeedbackItemListener = context
    private var lastSelected: RatingModel? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = FeedbackOptionViewBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    inner class ViewHolder(val binding: FeedbackOptionViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ratingModel: RatingModel) {
            binding.ivTick.visibility = View.INVISIBLE

            binding.title.text =
                HtmlCompat.fromHtml(ratingModel.label, HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (ratingModel.enable.not()) {
                binding.title.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.button_primary_color
                    )
                )
            }
            binding.title.setOnClickListener {
                if (ratingModel.enable) {
                    if (ratingModel.click.not()) {
                        lastSelected = ratingModel
                        notifyDataSetChanged()
                    }
                } else {
                    onFeedbackItemListener.onWriteComment()

                }

            }
            if (lastSelected != null && lastSelected == ratingModel) {
                binding.ivTick.visibility = View.VISIBLE
            }
        }
    }


    interface OnFeedbackItemListener {
        fun onSelectOption(label: String)
        fun onWriteComment()
    }

}
