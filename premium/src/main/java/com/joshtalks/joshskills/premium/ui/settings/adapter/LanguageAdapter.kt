package com.joshtalks.joshskills.premium.ui.settings.adapter

import android.content.Context
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.base.EventLiveData
import com.joshtalks.joshskills.premium.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.databinding.StringAdapterItemBinding
import com.joshtalks.joshskills.premium.repository.server.LanguageItem

class LanguageAdapter(
    val itemList: List<LanguageItem>,
    private var onItemClick: (item: LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    var context: Context? = null
    var selectedItem: String

    init {
        selectedItem = com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
            com.joshtalks.joshskills.premium.core.USER_LOCALE
        )
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
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    inner class LanguageViewHolder(val binding: StringAdapterItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageItem) {
            binding.itemNameTv.text = item.name
            if (selectedItem == item.code) {
                binding.root.isClickable = false
                binding.tickIv.visibility = View.VISIBLE
            } else
                binding.tickIv.visibility = View.GONE

            binding.root.setOnClickListener {
                if(selectedItem != item.code) {
                    selectedItem = item.code
                    onItemClick(item)

                    val message = Message()
                    message.what = SHOW_PROGRESS_BAR
                    EventLiveData.value = message

                    when (item.name) {
                        "Hinglish" -> {
                            com.joshtalks.joshskills.premium.core.PrefManager.put(
                                com.joshtalks.joshskills.premium.core.IS_HINGLISH_SELECTED, true)
                            MixPanelTracker.publishEvent(MixPanelEvent.HINGLISH).push()
                        }
                        "Hindi" -> {
                            com.joshtalks.joshskills.premium.core.PrefManager.put(
                                com.joshtalks.joshskills.premium.core.IS_HINDI_SELECTED, true)
                            MixPanelTracker.publishEvent(MixPanelEvent.HINDI).push()
                        }
                        else -> {
                            com.joshtalks.joshskills.premium.core.PrefManager.put(
                                com.joshtalks.joshskills.premium.core.IS_HINDI_SELECTED, false)
                            com.joshtalks.joshskills.premium.core.PrefManager.put(
                                com.joshtalks.joshskills.premium.core.IS_HINGLISH_SELECTED, false)
                        }
                    }
                    AppAnalytics.create(AnalyticsEvent.SELECT_LANGUAGE_CHANGED.name)
                        .addBasicParam()
                        .addUserDetails()
                        .addParam("selected_value", selectedItem)
                        .push()
                }
            }
        }
    }
}