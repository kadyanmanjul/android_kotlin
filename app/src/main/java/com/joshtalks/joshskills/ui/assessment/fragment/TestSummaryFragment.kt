package com.joshtalks.joshskills.ui.assessment.fragment

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FragmentTestSummaryReportBinding
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.ui.assessment.AssessmentActivity
import com.joshtalks.joshskills.ui.assessment.viewholder.TestItemViewHolder
import com.joshtalks.joshskills.ui.assessment.viewholder.TestScoreCardViewHolder
import com.joshtalks.joshskills.ui.assessment.viewholder.TestSummaryHeaderViewHolder
import com.joshtalks.joshskills.ui.assessment.viewmodel.AssessmentViewModel


class TestSummaryFragment : Fragment() {
    private lateinit var binding: FragmentTestSummaryReportBinding
    var assessmentId: Int = -1
    var isTestAlreadyAttempted: Boolean = false
    private lateinit var viewModel: AssessmentViewModel
    private var testData: AssessmentWithRelations? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assessmentId = it.getInt(ASSESSMENT_ID, -1)
            isTestAlreadyAttempted = it.getBoolean(IS_TEST_ATTEMPTED, false)
        }
        viewModel = activity?.run {
            ViewModelProvider(requireActivity()).get(AssessmentViewModel::class.java)
        }!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_test_summary_report,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        subscribeObserver()
        if (isTestAlreadyAttempted) {
            binding.progressBar.visibility = View.VISIBLE
            AppObjectController.uiHandler.postDelayed({
                viewModel.getTestReport(assessmentId)
            }, 3000)
        } else {
            viewModel.assessmentLiveData.value?.let {
                testData = it
                initView(testData)
            }
            if (testData == null) {
                requireActivity().finish()
            }
        }
    }

    private fun subscribeObserver() {

        viewModel.assessmentLiveData.observe(requireActivity(), Observer { assessmentWithRelation ->
            initView(assessmentWithRelation)
        })
    }

    private fun initView(assessmentWithRelation: AssessmentWithRelations?) {

        binding.progressBar.visibility = View.GONE
        binding.recyclerView.removeAllViews()

        assessmentWithRelation?.let { assessment ->
            assessment.questionList.sortedBy { it.question.sortOrder }.also {

                if (assessmentWithRelation.assessment.status == AssessmentStatus.STARTED || assessmentWithRelation.assessment.status == AssessmentStatus.NOT_STARTED)
                    binding.recyclerView.addView(
                        TestSummaryHeaderViewHolder(
                            assessment
                        )
                    )
                else binding.recyclerView.addView(
                    TestScoreCardViewHolder(
                        assessment.assessment
                    )
                )

            }.forEach { questionList ->
                binding.recyclerView.addView(
                    TestItemViewHolder(
                        questionList,
                        assessment.assessment.status
                    )
                )
            }
        }
    }

    private fun initRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        divider.setDrawable(
            ColorDrawable(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.seek_bar_background
                )
            )
        )
        binding.recyclerView.addItemDecoration(divider)
    }

    fun dismiss() {
        (requireActivity() as AssessmentActivity).submitTest()
    }

    companion object {
        const val ASSESSMENT_ID = "assessment_id"
        const val IS_TEST_ATTEMPTED = "is_test_attempted"

        @JvmStatic
        fun newInstance(assessmentId: Int, isTestAlreadyAttempted: Boolean = false) =
            TestSummaryFragment()
                .apply {
                arguments = Bundle().apply {
                    putInt(ASSESSMENT_ID, assessmentId)
                    putBoolean(IS_TEST_ATTEMPTED, isTestAlreadyAttempted)
                }
            }
    }
}
