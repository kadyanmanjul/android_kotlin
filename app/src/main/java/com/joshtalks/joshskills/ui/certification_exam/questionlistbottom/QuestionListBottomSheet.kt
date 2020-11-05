package com.joshtalks.joshskills.ui.certification_exam.questionlistbottom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.decorator.GridSpacingItemDecoration
import com.joshtalks.joshskills.core.interfaces.CertificationExamListener
import com.joshtalks.joshskills.databinding.BottomsheetQuestionListBinding
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestion
import com.joshtalks.joshskills.ui.certification_exam.examview.CERTIFICATION_EXAM_QUESTION
import com.joshtalks.joshskills.ui.certification_exam.examview.CURRENT_QUESTION


class QuestionListBottomSheet : BottomSheetDialogFragment(), Callback {

    companion object {
        @JvmStatic
        fun newInstance(questionList: List<CertificationQuestion>, currentQuestion: Int) =
            QuestionListBottomSheet()
                .apply {
                    arguments = Bundle().apply {
                        putParcelableArrayList(CERTIFICATION_EXAM_QUESTION, ArrayList(questionList))
                        putInt(CURRENT_QUESTION, currentQuestion)
                    }
                }
    }

    private lateinit var binding: BottomsheetQuestionListBinding
    private var questionList: List<CertificationQuestion> = emptyList()
    private var currentQuestion: Int = -1
    private var listener: CertificationExamListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            questionList = it.getParcelableArrayList(CERTIFICATION_EXAM_QUESTION) ?: emptyList()
            currentQuestion = it.getInt(CURRENT_QUESTION, -1)
        }
        listener = requireActivity() as CertificationExamListener
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.bottomsheet_question_list,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this

        val baseDialog = dialog
        if (baseDialog != null && baseDialog is BottomSheetDialog) {
            val behavior: BottomSheetBehavior<*> = baseDialog.behavior
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutManager = GridLayoutManager(requireContext(), 8)
        binding.recyclerView.addItemDecoration(
            GridSpacingItemDecoration(8, Utils.dpToPx(requireContext(), 4f), true)
        )
        binding.recyclerView.apply {
            setHasFixedSize(true)
            setLayoutManager(layoutManager)
        }

        binding.recyclerView.adapter = QuestionListAdapter(questionList, currentQuestion, this)
    }

    fun pauseAndExit() {
        dismissAllowingStateLoss()
        listener?.onPauseExit()
    }

    fun finishExam() {
        dismissAllowingStateLoss()
        listener?.onFinishExam()
    }

    override fun onGoToQuestion(position: Int) {
        dismissAllowingStateLoss()
        listener?.onGoToQuestion(position)
    }

}