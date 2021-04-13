package com.joshtalks.joshskills.ui.course_progress_new

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.repository.server.course_overview.CourseOverviewResponse


class CourseProgressAdapterDiffCallback : DiffUtil.Callback() {

    private var mOldModelList: MutableList<CourseOverviewResponse> = ArrayList()
    private var mNewModelList: MutableList<CourseOverviewResponse> = ArrayList()

    fun setItems(oldItems: List<CourseOverviewResponse>?, newItems: List<CourseOverviewResponse>?) {
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
        return (oldProgress.data.equals(newProgress.data))
    }

}