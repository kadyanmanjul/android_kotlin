package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ProgressActivityAdapterLayoutBinding
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse

class ProgressActivityAdapter(
    val context: Context,
    val list: List<CourseOverviewResponse>,
    val onItemClickListener: CourseProgressAdapter.ProgressItemClickListener,
    val conversationId: String
) :
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
        holder.bind(position, list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ProgressViewHolder(val binding: ProgressActivityAdapterLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        lateinit var adapter: CourseProgressAdapter
        fun bind(position: Int, item: CourseOverviewResponse) {
            binding.progressTitleTv.text = item.title
            adapter = CourseProgressAdapter(
                context,
                item.data,
                onItemClickListener,
                conversationId,
                item.chatId,
                item.certificateExamId,
                item.examStatus
            )
            binding.progressRv.adapter = adapter

        }

    }

}