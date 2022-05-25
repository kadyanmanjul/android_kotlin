package com.joshtalks.joshskills.ui.settings.adapter

import android.content.Context
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.core.IS_HINDI_SELECTED
import com.joshtalks.joshskills.core.IS_HINGLISH_SELECTED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_LOCALE
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.databinding.StringAdapterItemBinding
import com.joshtalks.joshskills.repository.server.LanguageItem

class LanguageAdapter(
    val itemList: List<LanguageItem>,
    private var onItemClick: (item: LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    var context: Context? = null
    var selectedItem: String

    init {
        selectedItem = PrefManager.getStringValue(USER_LOCALE)
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
                selectedItem = item.code
                onItemClick(item)

                val message = Message()
                message.what = SHOW_PROGRESS_BAR
                EventLiveData.value = message

                when (item.name) {
                    "Hinglish" -> {
                        PrefManager.put(IS_HINGLISH_SELECTED,true)
                        MixPanelTracker.publishEvent(MixPanelEvent.HINGLISH).push()
                    }
                    "Hindi" -> {
                        PrefManager.put(IS_HINDI_SELECTED,true)
                        MixPanelTracker.publishEvent(MixPanelEvent.HINDI).push()
                    }
                    else -> {
                        PrefManager.put(IS_HINDI_SELECTED,false)
                        PrefManager.put(IS_HINGLISH_SELECTED,false)
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