package com.joshtalks.joshskills.ui.assessment

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
import com.joshtalks.joshskills.databinding.FragmentTestSummaryReportBinding
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus


class TestSummaryFragment : Fragment() {
    private lateinit var binding: FragmentTestSummaryReportBinding
    var assessmentId: Int = -1
    private lateinit var viewModel: AssessmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assessmentId = it.getInt(ASSESSMENT_ID, -1)
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
        addObservers()
        viewModel.fetchAssessmentDetails(assessmentId)
    }

    private fun addObservers() {
        viewModel.assessmentLiveData.observe(viewLifecycleOwner, Observer { assessment ->
            if (binding.recyclerView.adapter == null || binding.recyclerView.adapter!!.itemCount == 0) {
                assessment?.let {
                    it.questionList.sortedBy { it.question.sortOrder }.also {
                        if (assessment.assessment.status == AssessmentStatus.STARTED || assessment.assessment.status == AssessmentStatus.NOT_STARTED)
                            binding.recyclerView.addView(
                                TestSummaryHeaderViewHolder(
                                    assessment, requireContext()
                                )
                            )
                        else if (assessment.assessment.status == AssessmentStatus.COMPLETED)
                            binding.recyclerView.addView(
                                TestScoreCardViewHolder(
                                    assessment.assessment, requireContext()
                                )
                            )
                    }.forEach { questionList ->
                        binding.recyclerView.addView(
                            TestItemViewHolder(
                                questionList.question,
                                assessment.assessment.status,
                                requireContext()
                            )
                        )
                    }
                }
            }
        })
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

        @JvmStatic
        fun newInstance(assessmentId: Int) =
            TestSummaryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ASSESSMENT_ID, assessmentId)
                }
            }
    }
}
