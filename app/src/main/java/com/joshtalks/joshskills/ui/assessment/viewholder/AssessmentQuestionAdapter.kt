package com.joshtalks.joshskills.ui.assessment.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.AssessmentListItemBinding
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionResponse

class AssessmentQuestionAdapter(private var items: List<AssessmentQuestionResponse>) :
    RecyclerView.Adapter<AssessmentQuestionAdapter.ViewHolder>() {
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AssessmentListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding).apply {
            setIsRecyclable(true)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(items[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.questionView.unBind()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder(val binding: AssessmentListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(obj: AssessmentQuestionResponse) {
            binding.questionView.bind(obj)
        }
    }

}
