package com.joshtalks.joshskills.ui.signup.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.ItemReasonScreenBinding
import com.joshtalks.joshskills.repository.server.GoalList
import com.joshtalks.joshskills.ui.activity_feed.setImage
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_GOAL_CARD

class ChooseGoalAdapter : RecyclerView.Adapter<ChooseGoalAdapter.ChooseLanguageItemViewHolder>() {
    var goalList: List<GoalList> = listOf()
    var prevHolder: ChooseLanguageItemViewHolder? = null
    var count = 1
    private var onGoalItemClick: ((GoalList, Int,Int, String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseLanguageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemReasonScreenBinding.inflate(inflater, parent, false)
        return ChooseLanguageItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChooseLanguageItemViewHolder, position: Int) {
        holder.bind(goalList[position], position)
    }

    override fun getItemCount(): Int = goalList.size

    fun setData(updatedGoalList: List<GoalList>) {
        goalList = updatedGoalList
        notifyDataSetChanged()
    }

    fun setListener(function: ((GoalList, Int, Int,String) -> Unit)?) {
        onGoalItemClick = function
    }

    inner class ChooseLanguageItemViewHolder(val binding: ItemReasonScreenBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(selectedGoal: GoalList, position: Int) {
            with(binding) {
                tvGoal.text = selectedGoal.goal
                ivGoalIcon.setImage(selectedGoal.imageUrl)
                rootView.setOnClickListener {
                    if (count > 2 && !selectedGoal.isSelected) {
                        showToast("You can’t select more than 2 options")
                        return@setOnClickListener
                    }
                    if (!selectedGoal.isSelected) {
                        showToast("${selectedGoal.testId}")
                        ++count
                        selectedGoal.isSelected = true
                        rootView.setBackgroundDrawable(AppObjectController.joshApplication.getDrawable(R.drawable.block_button_round_stroke_alpha_blue))
                        onGoalItemClick?.invoke(selectedGoal, position, CLICK_GOAL_CARD ,selectedGoal.goal)
                    }else{
                        --count
                        rootView.setBackgroundDrawable(AppObjectController.joshApplication.getDrawable(R.drawable.block_button_round_stroke_alpha_gray))
                        selectedGoal.isSelected = false
                    }
                }
            }
        }
    }
}