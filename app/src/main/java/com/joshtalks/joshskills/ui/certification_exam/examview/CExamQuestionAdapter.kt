package com.joshtalks.joshskills.ui.certification_exam.examview

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.CexamListItemBinding
import com.joshtalks.joshskills.repository.server.certification_exam.Answer
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationExamView
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.ui.certification_exam.questionlistbottom.Callback

class CExamQuestionAdapter(
    var questionList: List<CertificationQuestion>, var examView: CertificationExamView,
    private val listener: Callback? = null
) : RecyclerView.Adapter<CExamQuestionAdapter.ViewHolder>() {

    private var context = AppObjectController.joshApplication
    val accentColor =
        ContextCompat.getColor(context, R.color.colorAccent)

    init {
        setHasStableIds(true)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = CexamListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, parent.context)
    }


    override fun getItemCount(): Int = questionList.size

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return holder.bind(questionList[position], position)
    }

    inner class ViewHolder(val binding: CexamListItemBinding, val context: Context) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(certificationQuestion: CertificationQuestion, position: Int) {
            with(binding) {
                tvQuestion.text = certificationQuestion.questionText
                if (radioGroup.childCount == 0) {
                    certificationQuestion.answers.forEach {
                        radioGroup.addView(
                            getRadioButton(
                                it,
                                certificationQuestion.userSelectedOption
                            )
                        )
                    }
                }
                radioGroup.setOnCheckedChangeListener { group, checkedId ->
                    certificationQuestion.userSelectedOption =
                        (group.findViewById(checkedId) as AppCompatRadioButton).id
                    certificationQuestion.isAttempted = true
                }
                if (CertificationExamView.RESULT_VIEW == examView) {
                    groupRoot.visibility = View.VISIBLE
                    tvExplanation.text = certificationQuestion.explanation
                    btnNextQuestion.setOnClickListener {
                        listener?.onGoToQuestion(position + 1)
                    }
                    if (position == questionList.size) {
                        btnNextQuestion.visibility = View.GONE
                    }
                }
            }
        }

        private fun getRadioButton(answer: Answer, userSelectedOption: Int?): AppCompatRadioButton {
            val radioButton: AppCompatRadioButton = LayoutInflater.from(context)
                .inflate(R.layout.radio_button_view, null, false) as AppCompatRadioButton
            val params: RadioGroup.LayoutParams = RadioGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, Utils.dpToPx(6), 0, Utils.dpToPx(6))
            radioButton.layoutParams = params
            radioButton.id = answer.id
            radioButton.text = answer.text
            radioButton.isChecked = false
            radioButton.tag = answer.id
            //radio_button_unselect_bg.xml
            if (CertificationExamView.RESULT_VIEW == examView) {
                radioButton.isClickable = false
            } else {
                radioButton.setBackgroundResource(R.drawable.radio_button_selector)
                val colorStateList = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ), intArrayOf(
                        accentColor,
                        Color.parseColor("#70107BE5")
                    )
                )
                radioButton.buttonTintList = colorStateList

                if (userSelectedOption != null && userSelectedOption == answer.id) {
                    radioButton.isChecked = true
                }
            }
            return radioButton
        }
    }

}


interface Callback {
    fun onGoToQuestion(position: Int)
}
