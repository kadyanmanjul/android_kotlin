package com.joshtalks.joshskills.ui.course_progress_new

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.custom_ui.tvproperty.BulletSpanWithRadius
import com.joshtalks.joshskills.databinding.UnlockDialogLayoutBinding

class ExamUnlockDialogFragment(
    val instructionList: List<String>?,
    val marks: Int?,
    val totalQue: Int?,
    val time: Int?,
    val totalLessons: Int?,
    val unLockCount: Int?,
    val title:String= EMPTY
) : DialogFragment() {

    lateinit var binding: UnlockDialogLayoutBinding

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, WRAP_CONTENT)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.unlock_dialog_layout, container, false)

        binding.handler = this

        binding.examInfoTv.text = getString(R.string.exam_info, totalQue, time, marks)
        binding.examInfoCategory.text = getString(R.string.exam_title, title)
        binding.textview1.text =
            getString(R.string.you_can_only_attempt_the_certification_exam, unLockCount)
        if (instructionList != null)
            setInstructions(instructionList)
        return binding.root
    }

    private fun setInstructions(listText: List<String>) {
        val spannableStringBuilder = SpannableStringBuilder()
        listText.forEach {
            spannableStringBuilder.append(getBulletSpan(it)).append("\n\n")
        }
        binding.instructionTv.text = spannableStringBuilder
    }

    private fun getBulletSpan(text: String): SpannableString {
        val spanString = SpannableString(text)
        spanString.setSpan(
            BulletSpanWithRadius(
                6,
                20,
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            ), 0, text.length, 0
        )
        return spanString
    }

    fun dismissDialog() {
        dismiss()
    }
}