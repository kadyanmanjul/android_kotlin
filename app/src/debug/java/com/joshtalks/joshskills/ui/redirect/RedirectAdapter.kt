package com.joshtalks.joshskills.ui.redirect

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemNameCountBinding

class RedirectAdapter : RecyclerView.Adapter<RedirectAdapter.RedirectViewHolder>() {
    private var list = mutableListOf<String>()
    private var onClickListener: ((String, Int) -> Unit) = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RedirectViewHolder =
        RedirectViewHolder(ItemNameCountBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: RedirectViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size

    fun submitList(list: List<String>) {
        this.list.clear()
        this.list.addAll(list)
        notifyDataSetChanged()
    }

    fun setOnClickListener(onClickListener: (String, Int) -> Unit) {
        this.onClickListener = onClickListener
    }

    inner class RedirectViewHolder(private val binding: ItemNameCountBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activityName: String) {
            binding.data = Pair(activityName, -1)
            binding.root.setOnClickListener { onClickListener.invoke(activityName, absoluteAdapterPosition) }
        }
    }

}