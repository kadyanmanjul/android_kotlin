package com.joshtalks.joshskills.ui.assessment.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.AssessmentListItemBinding
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.ui.assessment.AssessmentQuestionViewType

class AssessmentQuestionAdapter(
    private var type: AssessmentType,
    private var status: AssessmentStatus,
    private var viewType: AssessmentQuestionViewType,
    private var questionList: List<AssessmentQuestionWithRelations>
) : RecyclerView.Adapter<AssessmentQuestionAdapter.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AssessmentListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding).apply {
            setIsRecyclable(false)
        }
    }


    override fun getItemCount(): Int = questionList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(questionList.sortedBy { it.question.sortOrder }[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.questionView.unBind()
        holder.binding.choiceView.unBind()
    }

    override fun getItemViewType(position: Int): Int {
        return questionList.sortedBy { it.question.sortOrder }[position].question.mediaType.intValue
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    inner class ViewHolder(val binding: AssessmentListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(assessmentQuestion: AssessmentQuestionWithRelations) {
            binding.questionView.bind(assessmentQuestion)
            binding.choiceView.bind(type, status, viewType, assessmentQuestion)
        }
    }

}
