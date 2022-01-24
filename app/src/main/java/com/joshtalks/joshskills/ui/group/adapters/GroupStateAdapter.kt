package com.joshtalks.joshskills.ui.group.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.GroupsLoadingBinding

class GroupStateAdapter : LoadStateAdapter<GroupStateAdapter.GroupStateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): GroupStateViewHolder {
        val view = DataBindingUtil.inflate<GroupsLoadingBinding>(LayoutInflater.from(parent.context), R.layout.groups_loading, parent, false)
        return GroupStateViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupStateViewHolder, loadState: LoadState) {
        holder.onBind(loadState)
    }

    class GroupStateViewHolder(private val view : GroupsLoadingBinding) : RecyclerView.ViewHolder(view.root) {
        fun onBind(loadState: LoadState) {
            view.loader.isVisible = loadState == LoadState.Loading
        }
    }
}