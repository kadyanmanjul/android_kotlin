package com.joshtalks.joshskills.premium.ui.certification_exam.report.vh

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.databinding.QuestionListItemViewBinding
import com.joshtalks.joshskills.premium.messaging.RxBus2
import com.joshtalks.joshskills.premium.repository.local.eventbus.GotoCEQuestionEventBus
import com.joshtalks.joshskills.premium.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.premium.repository.server.certification_exam.QuestionReportType
import com.joshtalks.joshskills.premium.repository.server.certification_exam.UserSelectedAnswer

class ReportQuestionListAdapter(
    private var items: List<UserSelectedAnswer>,
    private val questionReportType: QuestionReportType = QuestionReportType.UNKNOWN
) :
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
                textView.setTextColor(ContextCompat.getColor(context, R.color.pure_white))

                if (obj.isNotAttempt == null) {
                    updateBgTint(frameLayout, R.color.disabled)
                } else {
                    if (obj.isAnswerCorrect) {
                        updateBgTint(frameLayout, R.color.success)
                    } else {
                        updateBgTint(frameLayout, R.color.critical)
                    }
                }

                /*  this logic for Report overview  View 3 */
                if (QuestionReportType.UNKNOWN == questionReportType) {
                    frameLayout.visibility = View.VISIBLE
                } else {
                    frameLayout.visibility = View.INVISIBLE
                    if (obj.isNotAttempt == null && QuestionReportType.UNANSWERED == questionReportType) {
                        frameLayout.visibility = View.VISIBLE
                    } else {
                        if (obj.isNotAttempt != null && obj.isAnswerCorrect && QuestionReportType.RIGHT == questionReportType) {
                            frameLayout.visibility = View.VISIBLE
                        } else if (obj.isNotAttempt != null && obj.isAnswerCorrect.not() && QuestionReportType.WRONG == questionReportType) {
                            frameLayout.visibility = View.VISIBLE
                        }
                    }
                }
                /* end */

                frameLayout.setOnClickListener {
                    val type = if (obj.isNotAttempt == null) {
                        QuestionReportType.UNANSWERED
                    } else {
                        if (obj.isAnswerCorrect) {
                            QuestionReportType.RIGHT
                        } else {
                            QuestionReportType.WRONG
                        }
                    }
                    RxBus2.publish(GotoCEQuestionEventBus(obj.question))
                }
            }
        }

        private fun updateBgTint(view: View, color: Int) {
            view.backgroundTintList = ContextCompat.getColorStateList(context, color)
        }
    }
}