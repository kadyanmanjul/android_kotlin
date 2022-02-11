package com.joshtalks.joshskills.ui.fpp.adapters

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.ui.fpp.model.RecentCall

data class RecentDiffCallback(
    private val mOldInboxModelList: List<RecentCall>,
    private val mNewInboxModelList: List<RecentCall>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return mOldInboxModelList.size
    }


    override fun getNewListSize(): Int {
        return mNewInboxModelList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldInboxModelList[oldItemPosition] == mNewInboxModelList[newItemPosition]
    }


    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldMovie = mOldInboxModelList[oldItemPosition]
        val newMovie = mNewInboxModelList[newItemPosition]
        return oldMovie.callDuration == newMovie.callDuration
    }

}