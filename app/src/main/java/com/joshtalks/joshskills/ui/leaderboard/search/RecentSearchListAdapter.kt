package com.joshtalks.joshskills.ui.leaderboard.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.RecentSearchHeaderItemBinding
import com.joshtalks.joshskills.databinding.RecentSearchItemBinding
import com.joshtalks.joshskills.repository.local.entity.leaderboard.RecentSearch

class RecentSearchListAdapter(
    val itemList: List<RecentSearch>,
    val performSearch: ((keyword: String) -> Unit)? = null
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            0 -> { RecentSearchHeaderViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.recent_search_header_item, parent, false
                    )
                )
            }
            else->{ RecentSearchViewHolder(
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.recent_search_item, parent, false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder.itemViewType) {
            0 -> {
                (holder as RecentSearchHeaderViewHolder).bind()
            }
            else -> {
                (holder as RecentSearchViewHolder).bind(itemList[position])
            }
        }
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (itemList.get(position).keyword.isBlank()) 0 else 1
    }

    inner class RecentSearchViewHolder(val binding: RecentSearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecentSearch) {
            binding.insertIv.setOnClickListener { performSearch?.invoke(item.keyword) }
            binding.root.setOnClickListener { performSearch?.invoke(item.keyword) }
            binding.searchKeywordTv.text = item.keyword
        }
    }
    inner class RecentSearchHeaderViewHolder(val binding: RecentSearchHeaderItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.recentSearch.text = "Recent Searches"
            binding.clearSearch.text = "Clear"
            //binding.clearSearch.setOnClickListener { performSearch?.invoke() }
        }
    }
}