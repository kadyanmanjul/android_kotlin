package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressItemBinding
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem

class CourseProgressAdapter(
    val context: Context,
    val itemList: List<CourseOverviewItem>,
    val onItemClickListener: ProgressItemClickListener,
    val conversationId: String,
    val chatMessageId: String,
    val certificationId: Int,
    val cExamStatus: CExamStatus = CExamStatus.FRESH,
    val lastAvailableLessonNo: Int?,
    val parentPosition: Int,
    val title: String
) :
    RecyclerView.Adapter<CourseProgressAdapter.CourseProgressViewHolder>() {

    val vocabColor = ArrayList<Int>().apply {
        this.add(Color.parseColor("#3ADD03"))
        this.add(Color.parseColor("#B6FD04"))
    }

    val speakingColor = ArrayList<Int>().apply {
        this.add(Color.parseColor("#560FBC"))
        this.add(Color.parseColor("#560FBC"))
    }

    val readingColor = ArrayList<Int>().apply {
        this.add(Color.parseColor("#09C9DB"))
        this.add(Color.parseColor("#0DF9D0"))
    }

    val outerColor = ArrayList<Int>().apply {
        this.add(Color.parseColor("#E10717"))
        this.add(Color.parseColor("#FD3085"))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseProgressViewHolder {
        return CourseProgressViewHolder(
            CourseProgressItemBinding.inflate(
                LayoutInflater.from(
                    context
                ), parent, false
            )
        )
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
                binding.progressIndexTv.textSize = 9f
                binding.progressIndexTv.maxLines = 2
                if (itemList.size > 0) {
                    if (itemList[position - 1].status == LESSON_STATUS.CO.name)
                        binding.progressIv.alpha = 1f
                    else
                        binding.progressIv.alpha = 0.5f

                    binding.root.setOnClickListener {
                        onItemClickListener.onCertificateExamClick(
                            itemList.get(layoutPosition - 1),
                            conversationId,
                            chatMessageId,
                            certificationId,
                            cExamStatus,
                            parentPosition.div(2),
                            title
                        )
                    }
                } else {
                    binding.progressIv.alpha = 0.5f
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
                    item.vocabPercentage.toDouble().toInt()
                )
                binding.radialProgressView.setInnerProgress(
                    item.readingPercentage.toDouble().toInt()
                )
                if (item.speakingPercentage == null) {
                    binding.radialProgressView.hasThreeProgressView(true)
                } else {
                    binding.radialProgressView.hasThreeProgressView(false)
                    binding.radialProgressView.setInnerMostProgress(
                        item.speakingPercentage!!.toDouble().toInt()
                    )
                }
                if (item.status == LESSON_STATUS.NO.name)
                    binding.progressIv.alpha = 0.5f
                else
                    binding.progressIv.alpha = 1f

                binding.progressIndexTv.text = "${item.lessonNo}"
                lastAvailableLessonNo?.let {
                    if (it == item.lessonNo) {
                        binding.progressIndexTv.background =
                            ContextCompat.getDrawable(context, R.drawable.lesson_number_bg)
                        binding.progressIndexTv.setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                    }
                }
                binding.progressIv.visibility = View.GONE

                binding.root.setOnClickListener {
                    if (position > 0) {
                        onItemClickListener.onProgressItemClick(
                            itemList[position],
                            itemList[position - 1]
                        )
                    } else {
                        onItemClickListener.onProgressItemClick(
                            itemList[position],
                            null
                        )
                    }
                }
            }

            binding.radialProgressView.hasThreeProgressView(true)
            binding.radialProgressView.setOuterProgressColor(outerColor)
            binding.radialProgressView.setCenterProgressColor(vocabColor)
            binding.radialProgressView.setInnerProgressColor(readingColor)
            binding.radialProgressView.setInnerMostProgressColor(speakingColor)
        }

    }

    interface ProgressItemClickListener {
        fun onProgressItemClick(item: CourseOverviewItem, previousItem: CourseOverviewItem? = null)
        fun onCertificateExamClick(
            previousLesson: CourseOverviewItem, conversationId: String,
            chatMessageId: String,
            certificationId: Int,
            cExamStatus: CExamStatus = CExamStatus.FRESH,
            parentPosition: Int,
            title: String
        )
    }

}