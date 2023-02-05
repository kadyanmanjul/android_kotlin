package com.joshtalks.joshskills.premium.ui.signup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.databinding.LiLanguageItemBinding
import com.joshtalks.joshskills.premium.repository.server.GoalSelectionResponse

class ChooseGoalAdapter : RecyclerView.Adapter<ChooseGoalAdapter.ChooseLanguageItemViewHolder>() {

    private val diffUtil = object : DiffUtil.ItemCallback<GoalSelectionResponse>() {
        override fun areItemsTheSame(oldItem: GoalSelectionResponse, newItem: GoalSelectionResponse) =
            oldItem.testId == newItem.testId

        override fun areContentsTheSame(oldItem: GoalSelectionResponse, newItem: GoalSelectionResponse) =
            oldItem == newItem
    }

    private val differ = AsyncListDiffer(this, diffUtil)
    private var onGoalItemClick: ((GoalSelectionResponse) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseLanguageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LiLanguageItemBinding.inflate(inflater, parent, false)
        return ChooseLanguageItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChooseLanguageItemViewHolder, position: Int) {
        val goal = differ.currentList[position]
        holder.bind(goal)
    }

    override fun getItemCount(): Int = differ.currentList.size

    fun setData(updatedGoalList: List<GoalSelectionResponse>) {
        differ.submitList(updatedGoalList)
    }

    inner class ChooseLanguageItemViewHolder(val binding: LiLanguageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(selectedGoal: GoalSelectionResponse) {
            with(binding) {
                tvLanguage.text = selectedGoal.goal
                container.setOnClickListener {
                    onGoalItemClick?.invoke(selectedGoal)
                }
                tvLanguage.setOnClickListener {
                    onGoalItemClick?.invoke(selectedGoal)
                }
            }
        }
    }

    fun setGoalItemClickListener(listener: (GoalSelectionResponse) -> Unit) {
        onGoalItemClick = listener
    }
}