package com.joshtalks.joshskills.ui.signup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.LiLanguageItemBinding
import com.joshtalks.joshskills.repository.server.GoalSelectionResponse

class ChooseGoalAdapter : RecyclerView.Adapter<ChooseGoalAdapter.ChooseLanguageItemViewHolder>() {

    private val diffUtil = object : DiffUtil.ItemCallback<GoalSelectionResponse>() {
        override fun areItemsTheSame(oldItem: GoalSelectionResponse, newItem: GoalSelectionResponse) =
            oldItem.testId == newItem.testId

        override fun areContentsTheSame(oldItem: GoalSelectionResponse, newItem: GoalSelectionResponse) =
            oldItem == newItem
    }

    private val differ = AsyncListDiffer(this, diffUtil)
    private var onLanguageItemClick: ((GoalSelectionResponse) -> Unit)? = null

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

    fun setData(updatedGoalList: List<GoalSelectionResponse>) {
        differ.submitList(updatedGoalList.sortedByDescending { it.testId?.toInt() ?: 0 })
    }

    inner class ChooseLanguageItemViewHolder(val binding: LiLanguageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(selectedGoal: GoalSelectionResponse) {
            with(binding) {
                tvLanguage.text = selectedGoal.goal
                container.setOnClickListener {
                    onLanguageItemClick?.invoke(selectedGoal)
                }
                tvLanguage.setOnClickListener {
                    onLanguageItemClick?.invoke(selectedGoal)
                }
            }
        }
    }

    fun setGoalItemClickListener(listener: (GoalSelectionResponse) -> Unit) {
        onLanguageItemClick = listener
    }
}