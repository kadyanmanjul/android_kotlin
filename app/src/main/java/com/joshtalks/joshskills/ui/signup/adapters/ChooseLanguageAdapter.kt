package com.joshtalks.joshskills.ui.signup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.interfaces.OnChooseLanguage
import com.joshtalks.joshskills.databinding.LiLanguageItemBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages

class ChooseLanguageAdapter(
    val chooseLanguage: OnChooseLanguage,
): RecyclerView.Adapter<ChooseLanguageAdapter.ChooseLanguageItemViewHolder>() {

    private val languageSelectionList = ArrayList<ChooseLanguages>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseLanguageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiLanguageItemBinding.inflate(inflater, parent, false)
        return ChooseLanguageItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChooseLanguageItemViewHolder, position: Int) {
        holder.bind(languageSelectionList[position])
    }

    override fun getItemCount(): Int = languageSelectionList.size

    fun setData(updatedLanguageList: List<ChooseLanguages>) {
        languageSelectionList.clear()
        languageSelectionList.addAll(updatedLanguageList.sortedBy { it.testId.toInt() })
        notifyDataSetChanged()
    }

    inner class ChooseLanguageItemViewHolder(val binding: LiLanguageItemBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(selectedLanguage: ChooseLanguages) {
            with(binding) {
                tvLanguage.text = selectedLanguage.languageName

                container.setOnClickListener {
                    chooseLanguage.selectLanguageOnBoard(selectedLanguage)
                }

                tvLanguage.setOnClickListener {
                    chooseLanguage.selectLanguageOnBoard(selectedLanguage)
                }
            }
        }
    }
}