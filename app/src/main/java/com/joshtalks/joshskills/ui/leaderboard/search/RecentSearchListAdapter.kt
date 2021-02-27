package com.joshtalks.joshskills.ui.leaderboard.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.RecentSearchItemBinding
import com.joshtalks.joshskills.repository.local.entity.leaderboard.RecentSearch

class RecentSearchListAdapter(
    val itemList: List<RecentSearch>,
    val performSearch: ((keyword: String) -> Unit)? = null
) :
    RecyclerView.Adapter<RecentSearchListAdapter.RecentSearchViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        return RecentSearchViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.recent_search_item, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class RecentSearchViewHolder(val binding: RecentSearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RecentSearch) {
            binding.insertIv.setOnClickListener { performSearch?.invoke(item.keyword) }
            binding.root.setOnClickListener { performSearch?.invoke(item.keyword) }
            binding.searchKeywordTv.text = item.keyword
        }
    }
}