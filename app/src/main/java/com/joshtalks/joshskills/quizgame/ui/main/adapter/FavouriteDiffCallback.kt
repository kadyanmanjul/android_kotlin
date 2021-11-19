package com.joshtalks.joshskills.quizgame.ui.main.adapter

import androidx.recyclerview.widget.DiffUtil
import com.joshtalks.joshskills.quizgame.ui.data.FavouriteDemoData
import com.joshtalks.joshskills.quizgame.ui.data.model.Favourite

data class FavouriteDiffCallback(
    private val mOldFavouriteModelList: ArrayList<Favourite>?,
    private val mNewFavouriteModelList: ArrayList<Favourite>?
) : DiffUtil.Callback(){
    override fun getOldListSize(): Int {
        return mOldFavouriteModelList?.size!!
    }

    override fun getNewListSize(): Int {
        return mNewFavouriteModelList?.size!!
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldFavouriteModelList?.get(oldItemPosition)?.uuid == mNewFavouriteModelList?.get(newItemPosition)?.uuid
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return mOldFavouriteModelList?.get(oldItemPosition) ?: 0 == mNewFavouriteModelList?.get(newItemPosition) ?: 0
    }

}