package com.joshtalks.joshskills.certificate.examview

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
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.databinding.CexamListItemBinding
import com.joshtalks.joshskills.common.repository.server.certification_exam.Answer
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationExamView
import com.joshtalks.joshskills.common.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.certificate.questionlistbottom.Callback

class CExamQuestionAdapter(
    var questionList: List<CertificationQuestion>, var examView: CertificationExamView,
    private val listener: Callback? = null
) : RecyclerView.Adapter<CExamQuestionAdapter.ViewHolder>() {

    private var context = AppObjectController.joshApplication
    private val accentColor =
        ContextCompat.getColor(context, R.color.primary_500)
    private val colorStateList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ), intArrayOf(
            accentColor,
            Color.parseColor("#70107BE5")
        )
    )
    private val resultColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ), intArrayOf(
            Color.parseColor("#70107BE5"),
            Color.parseColor("#70107BE5")
        )
    )

    private val resultErrorColorStateList = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        ), intArrayOf(
            Color.parseColor("#70ff0000"),
            Color.parseColor("#ff0000")
        )
    )

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
                                certificationQuestion.userSelectedOption,
                                certificationQuestion.correctOptionId
                            )
                        )
                    }
                }
                radioGroup.setOnCheckedChangeListener { group, checkedId ->
                    certificationQuestion.userSelectedOption =
                        (group.findViewById(checkedId) as AppCompatRadioButton).id
                    certificationQuestion.isAttempted = true
                    listener?.onGoToQuestion(position + 1)
                }
                if (CertificationExamView.RESULT_VIEW == examView) {
                    tvExplanation.text = certificationQuestion.explanation
                    btnNextQuestion.setOnClickListener {
                        listener?.onGoToQuestion(position + 1)
                    }
                    btnNextQuestion.visibility = View.VISIBLE
                    textView.visibility = View.VISIBLE
                    tvExplanation.visibility = View.VISIBLE
                    if (position == (questionList.size - 1)) {
                        btnNextQuestion.visibility = View.GONE
                    }
                }
            }
        }

        private fun getRadioButton(
            answer: Answer,
            userSelectedOption: Int,
            correctOptionId: Int,
        ): AppCompatRadioButton {
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
            radioButton.tag = answer.id
            radioButton.isFocusable = true

            if (CertificationExamView.RESULT_VIEW == examView) {
                radioButton.isClickable = false
                if (correctOptionId == answer.id) {
                    radioButton.isFocusable = true
                    radioButton.setBackgroundResource(R.drawable.rb_selector_result)
                    radioButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_tick_extra_smallest,
                        0
                    )
                    radioButton.buttonTintList = colorStateList
                } else {
                    radioButton.setBackgroundResource(R.drawable.rb_selector_disable)
                    if ((answer.id == userSelectedOption)) {
                        radioButton.buttonTintList = resultErrorColorStateList
                        radioButton.backgroundTintList =
                            ColorStateList.valueOf(Color.parseColor("#70F57777"))
                    } else {
                        radioButton.buttonTintList = resultColorStateList
                    }
                }
                if (correctOptionId == answer.id && correctOptionId != userSelectedOption) {
                    radioButton.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#B3DFC4"))
                }
            } else {
                radioButton.setBackgroundResource(R.drawable.radio_button_selector)
                radioButton.buttonTintList = colorStateList
            }
            radioButton.isChecked = userSelectedOption == answer.id

            return radioButton
        }
    }
}
