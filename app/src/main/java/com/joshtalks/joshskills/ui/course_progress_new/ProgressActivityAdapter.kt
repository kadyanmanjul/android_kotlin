package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ProgressActivityAdapterLayoutBinding

class ProgressActivityAdapter(val context: Context) :
    RecyclerView.Adapter<ProgressActivityAdapter.ProgressViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProgressActivityAdapter.ProgressViewHolder {
        val binding = ProgressActivityAdapterLayoutBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        )
        binding.handler = this
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ProgressActivityAdapter.ProgressViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return 3
    }

    inner class ProgressViewHolder(val binding: ProgressActivityAdapterLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        lateinit var adapter: CourseProgressAdapter
        fun bind(position: Int) {
            binding.progressTitleTv.text = "Beginer"
            adapter = CourseProgressAdapter(context)
            binding.progressRv.adapter = adapter

        }

    }

}