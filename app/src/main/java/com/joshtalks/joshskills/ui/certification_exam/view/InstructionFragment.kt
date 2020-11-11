package com.joshtalks.joshskills.ui.certification_exam.view

import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.tvproperty.BulletSpanWithRadius
import com.joshtalks.joshskills.databinding.CeInstructionFragmentBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.ui.certification_exam.CertificationExamViewModel

class InstructionFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = InstructionFragment()
    }

    private lateinit var binding: CeInstructionFragmentBinding

    private lateinit var viewModel: CertificationExamViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(CertificationExamViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.ce_instruction_fragment, container, false)
        binding.lifecycleOwner = this
        binding.handler = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.certificationQuestionLiveData.observe(viewLifecycleOwner, {
            updateView(it)
        })

        viewModel.resumeExamLiveData.observe(viewLifecycleOwner, {
            it?.run {
                isExamResume(this)
            }
        })
        viewModel.resumeExamLiveData.value?.let {
            isExamResume(it)
        }
        updateView(viewModel.certificationQuestionLiveData.value)
        binding.instructionTv.movementMethod = ScrollingMovementMethod()

    }

    private fun updateView(certificationQuestionModel: CertificationQuestionModel?) {
        certificationQuestionModel?.run {
            instruction?.let {
                setInstructions(it)
            }
            if (attemptCount > 0) {
                binding.btnPreviousResult.visibility = View.VISIBLE
            }
            if (attemptCount == max_attempt) {
                binding.btnAttemptOver.visibility = View.VISIBLE
            }

            initRV(this)
            updateAttempt(this)
        }
    }

    private fun updateAttempt(obj: CertificationQuestionModel) {
        if (obj.max_attempt == obj.attemptCount) {
            binding.btnPreviousResult.visibility = View.GONE
            binding.btnStartExam.visibility = View.GONE

        }

    }

    private fun isExamResume(flag: Boolean) {
        if (flag) {
            binding.btnStartExam.text = getString(R.string.resume_examination)
        } else {
            binding.btnStartExam.text = getString(R.string.start_examination)
        }
    }

    private fun initRV(obj: CertificationQuestionModel) {
        binding.recyclerView.removeAllViews()
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.SPACE_AROUND
        layoutManager.flexWrap = FlexWrap.NOWRAP
        binding.recyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(layoutManager)
        binding.recyclerView.addView(
            InstructionTopViewHolder(
                obj.totalQuestion,
                getString(R.string.questions),
                "ic_ce_question",
                "#EBE3EE",
                "#9467A6"
            )
        )
        binding.recyclerView.addView(
            InstructionTopViewHolder(
                obj.totalMinutes,
                getString(R.string.minutes),
                "ic_ce_minute",
                "#E7F0F5",
                "#629EC2"
            )
        )
        binding.recyclerView.addView(
            InstructionTopViewHolder(
                obj.totalMarks,
                getString(R.string.marks),
                "ic_ce_mark",
                "#FAE8E8",
                "#F15D5D"
            )
        )

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
        spanString.setSpan(BulletSpanWithRadius(6, 20), 0, text.length, 0)
        return spanString
    }
}
