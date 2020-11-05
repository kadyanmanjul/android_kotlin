package com.joshtalks.joshskills.ui.certification_exam.report.vh

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.QuestionListItemViewBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.GotoCEQuestionEventBus
import com.joshtalks.joshskills.repository.server.certification_exam.UserSelectedAnswer

class ReportQuestionListAdapter(private var items: List<UserSelectedAnswer>) :
    RecyclerView.Adapter<ReportQuestionListAdapter.ViewHolder>() {
    private var context = AppObjectController.joshApplication

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = QuestionListItemViewBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], position)

    inner class ViewHolder(val binding: QuestionListItemViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(obj: UserSelectedAnswer, position: Int) {
            with(binding) {
                frameLayout.setBackgroundResource(R.drawable.circle_for_q_item_2)
                textView.text = (position + 1).toString()
                textView.setTextColor(ContextCompat.getColor(context, R.color.white))
                if (obj.isNotAttempt == null) {
                    updateBgTint(frameLayout, R.color.grey_68)
                } else {
                    if (obj.isAnswerCorrect) {
                        updateBgTint(frameLayout, R.color.green_3d)
                    } else {
                        updateBgTint(frameLayout, R.color.red_f6)
                    }
                }
                frameLayout.setOnClickListener {
                    RxBus2.publish(GotoCEQuestionEventBus(obj.question))
                }
            }
        }

        private fun updateBgTint(view: View, color: Int) {
            view.backgroundTintList = ContextCompat.getColorStateList(context, color)
        }
    }
}