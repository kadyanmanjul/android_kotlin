package com.joshtalks.badebhaiya.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.badebhaiya.databinding.ItemSearchSuggestionsBinding
import com.joshtalks.badebhaiya.feed.model.searchSuggestion.User
import com.joshtalks.badebhaiya.utils.setUserInitialInRect

class SearchSuggestionAdapter(
    private val onItemClick: (User) -> Unit
) :
    PagingDataAdapter<User, SearchSuggestionAdapter.SearchSuggestionViewHolder>(SearchSuggestionDIffUtil()) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchSuggestionViewHolder {
        val binding = ItemSearchSuggestionsBinding.inflate(
            LayoutInflater.from(
                parent.context
            ), parent,
            false
        )

        return SearchSuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchSuggestionViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class SearchSuggestionViewHolder(private val binding: ItemSearchSuggestionsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: User?) {
            with(binding) {
                user = item
                userCard.setOnClickListener {
                    item?.let {
                        onItemClick(it)
                    }
                }
            }
        }
    }
}

class SearchSuggestionDIffUtil : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}