package com.joshtalks.joshskills.ui.voip.favorite.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FppItemListBinding
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_CALL
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_PROFILE
import com.joshtalks.joshskills.ui.fpp.constants.FAV_USER_LONG_PRESS_CLICK
import com.joshtalks.joshskills.ui.inbox.adapter.FavoriteCallerDiffCallback
import com.joshtalks.joshskills.ui.inbox.adapter.InboxDiffCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FppFavoriteAdapter : RecyclerView.Adapter<FppFavoriteAdapter.FavoriteItemViewHolder>() {
    private var items: ArrayList<FavoriteCaller> = arrayListOf()
    private val context = AppObjectController.joshApplication
    var itemClick: ((FavoriteCaller, Int, Int) -> Unit)? = null

    fun addItems(newList: List<FavoriteCaller>) {
        if (newList.isEmpty()) {
            return
        }
        val diffCallback = FavoriteCallerDiffCallback(items, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    fun updateItem(favoriteCaller: FavoriteCaller, position: Int) {
        items[position] = favoriteCaller
        notifyItemChanged(position)
    }

    suspend fun clearSelections() {
        val oldItem = items.clone() as List<FavoriteCaller>
        items.stream().forEach { it.selected = false }
        withContext(Dispatchers.Main) {
            val diffResult = DiffUtil.calculateDiff(FavoriteDiffUtilCallback(oldItem, items))
            diffResult.dispatchUpdatesTo(this@FppFavoriteAdapter)
        }
    }

    suspend fun removeAndUpdated() {
        val oldItem = mutableListOf<FavoriteCaller>()
        oldItem.addAll(items)
        val newList = items.filter { !it.selected }
        items.clear()
        items.addAll(newList)
        withContext(Dispatchers.Main) {
            val diffResult = DiffUtil.calculateDiff(FavoriteDiffUtilCallback(oldItem, newList))
            diffResult.dispatchUpdatesTo(this@FppFavoriteAdapter)
        }
    }

    fun setListener(function: ((FavoriteCaller, Int, Int) -> Unit)?) {
        itemClick = function
    }

    fun getItemSize() = items.size


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavoriteItemViewHolder {
        val binding =
            FppItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FavoriteItemViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: FavoriteItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    fun getItemAtPosition(position: Int): FavoriteCaller {
        return items[position]
    }

    inner class FavoriteItemViewHolder(val binding: FppItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(favoriteCaller: FavoriteCaller, position: Int) {
            with(binding) {
                obj = favoriteCaller

                groupItemContainer.setOnClickListener {
                    itemClick?.invoke(favoriteCaller, FAV_CLICK_ON_PROFILE, position)
                }

                profileImage.setOnClickListener {
                    itemClick?.invoke(favoriteCaller, FAV_CLICK_ON_PROFILE, position)
                }

                fppCallIcon.setOnSingleClickListener {
                    itemClick?.invoke(favoriteCaller, FAV_CLICK_ON_CALL, position)
                }

                groupItemContainer.setOnLongClickListener {
                    itemClick?.invoke(favoriteCaller, FAV_USER_LONG_PRESS_CLICK, position)
                    true
                }

                tvSpokenTime.text = spokenTimeText(favoriteCaller.minutesSpoken)

                if (favoriteCaller.selected) {
                    fppCallIcon.setOnClickListener {
                        itemClick?.invoke(favoriteCaller, FAV_USER_LONG_PRESS_CLICK, position)
                        rootView.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                        ivTick.visibility = View.GONE
                    }
                    groupItemContainer.setOnClickListener {
                        itemClick?.invoke(favoriteCaller, FAV_USER_LONG_PRESS_CLICK, position)
                    }
                } else {
                    fppCallIcon.isEnabled = true
                }
            }
        }

        private fun spokenTimeText(minute: Int): String {
            val string = StringBuilder()
            string.append("Total time Spoken: $minute ")
            if (minute > 1) {
                string.append("minutes")
            } else {
                string.append("minute")
            }
            return string.toString()
        }
    }
     fun clearItem(position: Int) {
         try {
             items.removeAt(position)
             notifyItemRemoved(position)
         } catch (e: Exception) {
             e.printStackTrace()
         }
    }
}

class FavoriteDiffUtilCallback(val favoriteList : List<FavoriteCaller>, val newFavoriteList : List<FavoriteCaller>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return favoriteList.size
    }

    override fun getNewListSize(): Int {
        return newFavoriteList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val check = favoriteList[oldItemPosition].mentorId == newFavoriteList[newItemPosition].mentorId
        return check
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val check = favoriteList[oldItemPosition].name == newFavoriteList[newItemPosition].name && favoriteList[oldItemPosition].image == newFavoriteList[newItemPosition].image && favoriteList[oldItemPosition].selected == newFavoriteList[newItemPosition].selected
        return check
    }

}