package com.joshtalks.joshskills.ui.conversation_practice.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.QuizPractiseItemLayoutBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.AnswersModel
import com.joshtalks.joshskills.repository.server.conversation_practice.QuizModel
import com.joshtalks.joshskills.ui.conversation_practice.extra.QuizPractiseOptionView

class QuizPractiseAdapter(
    var items: List<QuizModel>,
    var listener: OnChoiceClickListener2? = null
) :
    RecyclerView.Adapter<QuizPractiseAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): QuizPractiseAdapter.ViewHolder {
        val binding = QuizPractiseItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }


    override fun getItemId(position: Int): Long {
        return items[position].id.toLong()
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: QuizPractiseAdapter.ViewHolder, position: Int) {
        holder.bind(items[position])
    }


    inner class ViewHolder(val binding: QuizPractiseItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root), OnChoiceClickListener {
        init {
            binding.rvChoice.builder.setItemViewCacheSize(1).setHasFixedSize(true)
        }

        fun bind(quizModel: QuizModel) {
            binding.quizModel = quizModel
            if (binding.rvChoice.viewAdapter == null || binding.rvChoice.viewAdapter.itemCount == 0) {
                quizModel.answersModel.sortedBy { it.sortOrder }
                    .forEachIndexed { index, answersModel ->
                        binding.rvChoice.addView(QuizPractiseOptionView(index, answersModel, this))

                    }
            }
            binding.rvChoice.refresh()
        }

        override fun onChoiceClick(position: Int, answersModel: AnswersModel) {
            items[bindingAdapterPosition].answersModel.forEach { it.isSelectedByUser = false }
            answersModel.isSelectedByUser = true
            binding.rvChoice.refresh()
            listener?.onChoiceSelectListener()
        }
    }

}

interface OnChoiceClickListener {
    fun onChoiceClick(position: Int, answersModel: AnswersModel)
}

interface OnChoiceClickListener2 {
    fun onChoiceSelectListener()

}



