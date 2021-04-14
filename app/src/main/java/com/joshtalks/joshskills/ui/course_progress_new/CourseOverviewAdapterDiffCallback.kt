package com.joshtalks.joshskills.ui.course_progress_new

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewItem

class CourseOverviewAdapterDiffCallback : DiffUtil.Callback() {

    private val mOldModelList: MutableList<CourseOverviewItem> = ArrayList()
    private val mNewModelList: MutableList<CourseOverviewItem> = ArrayList()
    fun setItems(oldItems: List<CourseOverviewItem>?, newItems: List<CourseOverviewItem>?) {
        mOldModelList.clear()
        mOldModelList.addAll(oldItems!!)
        mNewModelList.clear()
        mNewModelList.addAll(newItems!!)
    }

    override fun getOldListSize(): Int {
        return mOldModelList.size
    }

    override fun getNewListSize(): Int {
        return mNewModelList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldModelList[oldItemPosition] == mNewModelList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldProgress = mOldModelList[oldItemPosition]
        val newProgress = mNewModelList[newItemPosition]
        return (oldProgress.grammarPercentage == newProgress.grammarPercentage &&
                oldProgress.vocabPercentage == newProgress.vocabPercentage &&
                oldProgress.readingPercentage == newProgress.readingPercentage &&
                oldProgress.speakingPercentage.equals(newProgress.speakingPercentage))
    }

}