package com.joshtalks.joshskills.ui.inbox.adapter

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller

data class FavoriteCallerDiffCallback(
    private val mOldModelList: List<FavoriteCaller>,
    private val mNewModelList: List<FavoriteCaller>
) : DiffUtil.Callback() {

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
        val oldMovie = mOldModelList[oldItemPosition]
        val newMovie = mNewModelList[newItemPosition]
        return oldMovie.id == newMovie.id
    }

}