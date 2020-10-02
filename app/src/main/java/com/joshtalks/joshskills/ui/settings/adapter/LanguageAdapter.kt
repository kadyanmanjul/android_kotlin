package com.joshtalks.joshskills.ui.settings.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SELECTED_LANGUAGE
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.StringAdapterItemBinding
import com.joshtalks.joshskills.repository.server.LanguageItem

class LanguageAdapter(
    val itemList: List<LanguageItem>,
    private var onItemClick: (item: LanguageItem, position: Int) -> Unit
) :
    RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    var context: Context? = null
    var selectedItem: String

    init {
        selectedItem = PrefManager.getStringValue(SELECTED_LANGUAGE)
        if (selectedItem.isEmpty())
            selectedItem = "en"
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageAdapter.LanguageViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(parent.context)
        val binding = StringAdapterItemBinding.inflate(inflater, parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(itemList[position], position)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class LanguageViewHolder(val binding: StringAdapterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageItem, position: Int) {
            binding.itemNameTv.text = item.name

            if (selectedItem == item.code) {
                binding.tickIv.visibility = View.VISIBLE
            } else
                binding.tickIv.visibility = View.GONE

            binding.root.setOnClickListener {
                selectedItem = item.code

                AppAnalytics.create(AnalyticsEvent.SELECT_LANGUAGE_CHANGED.name)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam("selected_value", selectedItem)
                    .push()

                onItemClick(item, position)
                notifyDataSetChanged()
            }
        }
    }
}