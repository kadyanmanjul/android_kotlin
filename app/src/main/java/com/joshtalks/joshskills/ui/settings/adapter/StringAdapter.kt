package com.joshtalks.joshskills.ui.settings.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SELECTED_LANGUAGE
import com.joshtalks.joshskills.core.SELECTED_QUALITY
import com.joshtalks.joshskills.databinding.StringAdapterItemBinding

const val ACTION_LANGUAGE = "language"
const val ACTION_QUALITY = "quality"

class StringAdapter(
    val actionType: String,
    val itemList: Array<String>,
    private var onItemClick: (item: String, position: Int) -> Unit
) :
    RecyclerView.Adapter<StringAdapter.StringViewHolder>() {

    var context: Context? = null
    var selectedItem: String

    init {
        if (actionType == ACTION_LANGUAGE) {
            selectedItem = PrefManager.getStringValue(SELECTED_LANGUAGE)
            if (selectedItem.isEmpty())
                selectedItem = "English"
        } else {
            selectedItem = PrefManager.getStringValue(SELECTED_QUALITY)
            if (selectedItem.isEmpty())
                selectedItem =
                    context?.resources?.getStringArray(R.array.resolutions)?.get(2) ?: "Low"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StringViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val binding = StringAdapterItemBinding.inflate(inflater, parent, false)
        return StringViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StringViewHolder, position: Int) {
        holder.bind(itemList[position], position)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class StringViewHolder(val binding: StringAdapterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: String, position: Int) {
            binding.itemNameTv.text = item

            if (selectedItem == item) {
                binding.tickIv.visibility = View.VISIBLE
            } else
                binding.tickIv.visibility = View.GONE

            binding.root.setOnClickListener {
                selectedItem = item
                onItemClick(item, position)
                notifyDataSetChanged()
            }
        }
    }
}