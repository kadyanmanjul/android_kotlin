package com.joshtalks.joshskills.ui.voip.favorite.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.interfaces.OnClickUserProfile
import com.joshtalks.joshskills.databinding.FppItemListBinding
import com.joshtalks.joshskills.repository.local.entity.practise.FavoriteCaller
import com.joshtalks.joshskills.ui.inbox.adapter.FavoriteCallerDiffCallback
import java.util.*


class FavoriteAdapter(
    private var lifecycleProvider: LifecycleOwner,
    private val onClickUserProfile: OnClickUserProfile,
) :
    RecyclerView.Adapter<FavoriteAdapter.FavoriteItemViewHolder>() {
    private var items: ArrayList<FavoriteCaller> = arrayListOf()
    private val context = AppObjectController.joshApplication

    init {
        setHasStableIds(true)
    }

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

    fun getItemSize() = items.size


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavoriteItemViewHolder {
        val binding =
            FppItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.apply {
            lifecycleOwner = lifecycleProvider
        }
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

                tvName.text = favoriteCaller.name
                if (favoriteCaller.isOnline)
                    ivOnlineTick.visibility = View.VISIBLE

                binding.groupItemContainer.setOnClickListener{
                    onClickUserProfile.clickOnProfile(position)
                }

                profileImage.setOnClickListener {
                    onClickUserProfile.clickOnProfile(position)
                }

                fppCallIcon.setOnClickListener {
                    onClickUserProfile.clickOnPhoneCall(position)
                }

                groupItemContainer.setOnLongClickListener {
                    onClickUserProfile.clickLongPressDelete(position)
                    true
                }

                tvSpokenTime.text = spokenTimeText(favoriteCaller.minutesSpoken)
                if (favoriteCaller.selected) {
                    fppCallIcon.setOnClickListener {
                        onClickUserProfile.clickLongPressDelete(position)
                        rootView.setCardBackgroundColor(
                            ContextCompat.getColor(
                                context,
                                R.color.white
                            )
                        )
                        ivTick.visibility = View.GONE
                    }
                    groupItemContainer.setOnClickListener {
                        onClickUserProfile.clickLongPressDelete(position)
                    }
                    groupItemContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.select_bg_color
                        )
                    )
                    ivTick.visibility = View.VISIBLE
                } else {
                    fppCallIcon.isEnabled = true
                    groupItemContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
                        )
                    )
                    ivTick.visibility = View.GONE
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