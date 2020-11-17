package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
                binding.progressIv.visibility = View.VISIBLE
                binding.progressIv.visibility = View.GONE
                binding.progressIndexTv.text = context.getString(R.string.exam)

                binding.progressIv.alpha = 0.5f

                binding.root.setOnClickListener {
                    onItemClickListener.onCertificateExamClick()
                }
                binding.radialProgressView.visibility = View.GONE
                binding.progressIv.visibility = View.VISIBLE

            } else {
                binding.progressIv.visibility = View.GONE
                binding.radialProgressView.visibility = View.VISIBLE
                val item = itemList[position]
                binding.radialProgressView.setOuterProgress(
                    item.grammarPercentage.toDouble().toInt()
                )
                binding.radialProgressView.setCenterProgress(
                    item.vpPercentage.toDouble().toInt()
                )
                binding.radialProgressView.setInnerProgress(item.rpPercentageval.toDouble().toInt())
                binding.radialProgressView.setInnerMostProgress(
                    item.speakingPercentage.toDouble().toInt()
                )

                if (item.status == LESSON_STATUS.NO.name)
                    binding.progressIv.alpha = 0.5f
                else
                    binding.progressIv.alpha = 1f

                binding.progressIndexTv.text = "${item.lessonNo}"

                binding.progressIv.visibility = View.GONE

                binding.root.setOnClickListener {
                    onItemClickListener.onProgressItemClick(itemList[position])
                }
            }

            val outerColor = ArrayList<Int>()
            outerColor.add(Color.parseColor("#E10717"))
            outerColor.add(Color.parseColor("#FD3085"))
            binding.radialProgressView.setOuterProgressColor(outerColor)


            val vocabColor = ArrayList<Int>()
            vocabColor.add(Color.parseColor("#3ADD03"))
            vocabColor.add(Color.parseColor("#B6FD04"))
            binding.radialProgressView.setCenterProgressColor(vocabColor)


            val readingColor = ArrayList<Int>()
            readingColor.add(Color.parseColor("#09C9DB"))
            readingColor.add(Color.parseColor("#0DF9D0"))
            binding.radialProgressView.setInnerProgressColor(readingColor)


            val speakingColor = ArrayList<Int>()
            speakingColor.add(Color.parseColor("#560FBC"))
            speakingColor.add(Color.parseColor("#560FBC"))
            binding.radialProgressView.setInnerMostProgressColor(speakingColor)

        }

    }

    interface ProgressItemClickListener {
        fun onProgressItemClick(item: CourseOverviewItem)
        fun onCertificateExamClick()
    }

}