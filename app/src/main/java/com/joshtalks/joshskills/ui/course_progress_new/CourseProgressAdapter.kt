package com.joshtalks.joshskills.ui.course_progress_new

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.CourseProgressItemBinding
import com.joshtalks.joshskills.repository.local.entity.CExamStatus
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem
import com.joshtalks.joshskills.ui.lesson.GRAMMAR_POSITION
import com.joshtalks.joshskills.ui.lesson.READING_POSITION
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.lesson.VOCAB_POSITION

class CourseProgressAdapter(
    val context: Context,
    val itemList: ArrayList<CourseOverviewItem>,
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

    private val diffCallback: CourseOverviewAdapterDiffCallback by lazy { CourseOverviewAdapterDiffCallback() }

    val vocabColor = ArrayList<Int>().apply {
        this.add(Color.parseColor("#3ADD03"))
        this.add(Color.parseColor("#B6FD04"))
    }

    val speakingColor = ArrayList<Int>().apply {
        this.add(Color.parseColor("#560FBC"))
        this.add(Color.parseColor("#7B2ECB"))
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

    fun updateDataList(newList: List<CourseOverviewItem>?) {
        if (newList.isNullOrEmpty()) {
            return
        }
        diffCallback.setItems(itemList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        itemList.clear()
        itemList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
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
                when (GRAMMAR_POSITION) {
                    0 -> {
                        binding.radialProgressView.setOuterProgress(
                            item.grammarPercentage.toDouble().toInt()
                        )
                    }
                    1 -> {
                        binding.radialProgressView.setCenterProgress(
                            item.grammarPercentage.toDouble().toInt()
                        )
                    }
                    2 -> {
                        binding.radialProgressView.setInnerProgress(
                            item.grammarPercentage.toDouble().toInt()
                        )
                    }
                    3 -> {
                        binding.radialProgressView.hasThreeProgressView(false)
                        binding.radialProgressView.setInnerMostProgress(
                            item.grammarPercentage.toDouble().toInt()
                        )
                    }
                    else -> {
                        binding.radialProgressView.setOuterProgress(
                            item.grammarPercentage.toDouble().toInt()
                        )
                    }
                }

                when (VOCAB_POSITION) {
                    0 -> {
                        binding.radialProgressView.setOuterProgress(
                            item.vocabPercentage.toDouble().toInt()
                        )
                    }
                    1 -> {
                        binding.radialProgressView.setCenterProgress(
                            item.vocabPercentage.toDouble().toInt()
                        )
                    }
                    2 -> {
                        binding.radialProgressView.setInnerProgress(
                            item.vocabPercentage.toDouble().toInt()
                        )
                    }
                    3 -> {
                        binding.radialProgressView.hasThreeProgressView(false)
                        binding.radialProgressView.setInnerMostProgress(
                            item.vocabPercentage.toDouble().toInt()
                        )
                    }
                    else -> {
                        binding.radialProgressView.setCenterProgress(
                            item.vocabPercentage.toDouble().toInt()
                        )
                    }
                }

                when (READING_POSITION) {
                    0 -> {
                        binding.radialProgressView.setOuterProgress(
                            item.readingPercentage.toDouble().toInt()
                        )
                    }
                    1 -> {
                        binding.radialProgressView.setCenterProgress(
                            item.readingPercentage.toDouble().toInt()
                        )
                    }
                    2 -> {
                        binding.radialProgressView.setInnerProgress(
                            item.readingPercentage.toDouble().toInt()
                        )
                    }
                    3 -> {
                        binding.radialProgressView.hasThreeProgressView(false)
                        binding.radialProgressView.setInnerMostProgress(
                            item.readingPercentage.toDouble().toInt()
                        )
                    }
                    else -> {
                        binding.radialProgressView.setInnerProgress(
                            item.readingPercentage.toDouble().toInt()
                        )
                    }
                }

                if (item.speakingPercentage == null) {
                    binding.radialProgressView.hasThreeProgressView(true)
                } else {
                    when (SPEAKING_POSITION) {
                        0 -> {
                            binding.radialProgressView.setOuterProgress(
                                item.speakingPercentage!!.toDouble().toInt()
                            )
                        }
                        1 -> {
                            binding.radialProgressView.setCenterProgress(
                                item.speakingPercentage!!.toDouble().toInt()
                            )
                        }
                        2 -> {
                            binding.radialProgressView.setInnerProgress(
                                item.speakingPercentage!!.toDouble().toInt()
                            )
                        }
                        3 -> {
                            binding.radialProgressView.hasThreeProgressView(false)
                            binding.radialProgressView.setInnerMostProgress(
                                item.speakingPercentage!!.toDouble().toInt()
                            )
                        }
                        else -> {
                            binding.radialProgressView.hasThreeProgressView(false)
                            binding.radialProgressView.setInnerMostProgress(
                                item.speakingPercentage!!.toDouble().toInt()
                            )
                        }
                    }

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

            binding.radialProgressView.hasThreeProgressView(false)
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