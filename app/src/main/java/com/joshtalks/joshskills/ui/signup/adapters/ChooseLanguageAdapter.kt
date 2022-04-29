package com.joshtalks.joshskills.ui.signup.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.databinding.LiLanguageItemBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages

class ChooseLanguageAdapter: RecyclerView.Adapter<ChooseLanguageAdapter.ChooseLanguageItemViewHolder>() {
    private val diffUtil = object : DiffUtil.ItemCallback<ChooseLanguages>() {
        override fun areItemsTheSame(oldItem: ChooseLanguages, newItem: ChooseLanguages) = oldItem.testId == newItem.testId
        override fun areContentsTheSame(oldItem: ChooseLanguages, newItem: ChooseLanguages) = oldItem == newItem
    }
    private val differ = AsyncListDiffer<ChooseLanguages>(this, diffUtil)
    private var onLanguageItemClick: ((ChooseLanguages) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseLanguageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiLanguageItemBinding.inflate(inflater, parent, false)
        return ChooseLanguageItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChooseLanguageItemViewHolder, position: Int) {
        val language = differ.currentList[position]
        holder.bind(language)
    }

    override fun getItemCount(): Int = differ.currentList.size

    fun setData(updatedLanguageList: List<ChooseLanguages>) {
        differ.submitList(updatedLanguageList.sortedBy { it.testId.toInt() })
    }

    inner class ChooseLanguageItemViewHolder(val binding: LiLanguageItemBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(selectedLanguage: ChooseLanguages) {
            with(binding) {
                tvLanguage.text = selectedLanguage.languageName
                container.setOnClickListener {
                    MixPanelTracker.publishEvent(MixPanelEvent.LANGUAGE_SELECTED)
                        .addParam(ParamKeys.SELECTED_LANGUAGE,selectedLanguage.languageName)
                        .push()
                    onLanguageItemClick?.invoke(selectedLanguage)
                }
                tvLanguage.setOnClickListener {
                    MixPanelTracker.publishEvent(MixPanelEvent.LANGUAGE_SELECTED)
                        .addParam(ParamKeys.SELECTED_LANGUAGE,selectedLanguage.languageName)
                        .push()
                    onLanguageItemClick?.invoke(selectedLanguage)
                }
            }
        }
    }

    fun setLanguageItemClickListener(listener: (ChooseLanguages) -> Unit) {
        onLanguageItemClick = listener
    }
}