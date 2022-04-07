package com.joshtalks.joshskills.ui.voip.favorite.adapter

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
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_CALL
import com.joshtalks.joshskills.ui.fpp.constants.FAV_CLICK_ON_PROFILE
import com.joshtalks.joshskills.ui.fpp.constants.FAV_USER_LONG_PRESS_CLICK
import com.joshtalks.joshskills.ui.inbox.adapter.FavoriteCallerDiffCallback

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

    fun clearSelections() {
        items.stream().forEach { it.selected = false }
        notifyDataSetChanged()
    }

    fun removeAndUpdated() {
        val list = items.filter { it.selected.not() }
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
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

                fppCallIcon.setOnClickListener {
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
}