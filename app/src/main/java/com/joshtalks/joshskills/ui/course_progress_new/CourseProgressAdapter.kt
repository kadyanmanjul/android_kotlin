package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressItemBinding
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem

class CourseProgressAdapter(
    val context: Context,
    val itemList: List<CourseOverviewItem>,
    val onItemClickListener: ProgressItemClickListener
) :
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
        return itemList.size + 1
    }

    inner class CourseProgressViewHolder(val binding: CourseProgressItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {


            if (position == itemList.size) {
                binding.progressIndexTv.text = context.getString(R.string.exam)
                binding.progressIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.gold_medal
                    )
                )
                binding.progressIv.alpha = 0.5f

                binding.root.setOnClickListener {
                    onItemClickListener.onCertificateExamClick()
                }

            } else {
                if (itemList[position].status == LESSON_STATUS.NO.name)
                    binding.progressIv.alpha = 0.5f
                else
                    binding.progressIv.alpha = 1f

                binding.progressIndexTv.text = "${position + 1}"

                binding.progressIv.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.ic_progress
                    )
                )

                binding.root.setOnClickListener {
                    onItemClickListener.onProgressItemClick(itemList[position])
                }
            }
        }

    }

    interface ProgressItemClickListener {
        fun onProgressItemClick(item: CourseOverviewItem)
        fun onCertificateExamClick()
    }

}