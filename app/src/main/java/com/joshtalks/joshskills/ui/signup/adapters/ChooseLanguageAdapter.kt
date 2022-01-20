package com.joshtalks.joshskills.ui.signup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.core.interfaces.OnChooseLanguage
import com.joshtalks.joshskills.databinding.LiLanguageItemBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages

class ChooseLanguageAdapter(
    var languageSelectionList: List<ChooseLanguages>,
    val chooseLanguage: OnChooseLanguage
): RecyclerView.Adapter<ChooseLanguageAdapter.ChooseLanguageItemViewHolder>()
/*,
    ListAdapter<ChooseLanguages, ChooseLanguageAdapter.ChooseLanguageItemViewHolder>(
        object : DiffUtil.Callback() {
            override fun getOldListSize(): Int {
                TODO("Not yet implemented")
            }

            override fun getNewListSize(): Int {
                TODO("Not yet implemented")
            }

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                TODO("Not yet implemented")
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                TODO("Not yet implemented")
            }

        }
    ) */{

//    private lateinit var languageSelectionList: List<ChooseLanguages>

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
        languageSelectionList = updatedLanguageList
        notifyDataSetChanged()
    }

    inner class ChooseLanguageItemViewHolder(val binding: LiLanguageItemBinding):
        RecyclerView.ViewHolder(binding.root) {

        fun bind(selectedLanguage: ChooseLanguages) {
            with(binding) {
                tvLanguage.text = selectedLanguage.languageName
                root.setOnClickListener {
                    chooseLanguage.selectLanguageOnBoard(selectedLanguage)
                }
            }

        }
    }
}