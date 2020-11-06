package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressItemBinding

class CourseProgressAdapter(val context: Context) :
    RecyclerView.Adapter<CourseProgressAdapter.CourseProgressViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseProgressViewHolder {
        val binding = CourseProgressItemBinding.inflate(LayoutInflater.from(context), parent, false)
        binding.handler = this
        return CourseProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourseProgressViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return 30
    }

    inner class CourseProgressViewHolder(val binding: CourseProgressItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.progressIndexTv.text = "${position + 1}"

            if (position > 17)
                binding.progressIv.alpha = 0.5f

            if (position == 29) {
                binding.progressIndexTv.text = context.getString(R.string.exam)
                binding.progressIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.gold_medal
                    )
                )
            } else {
                binding.progressIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_progress
                    )
                )
            }
        }

    }


}