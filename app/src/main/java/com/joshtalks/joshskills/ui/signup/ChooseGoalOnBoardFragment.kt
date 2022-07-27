package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.ApiCallStatus.*
import com.joshtalks.joshskills.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.databinding.FragmentChooseLanguageOnboardBinding
import com.joshtalks.joshskills.repository.server.GoalSelectionResponse
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.signup.adapters.ChooseGoalAdapter
import com.joshtalks.joshskills.ui.special_practice.utils.ErrorView

class ChooseGoalOnBoardFragment : BaseFragment() {
    private lateinit var binding: FragmentChooseLanguageOnboardBinding
    private var goalAdapter = ChooseGoalAdapter()
    private var errorView: Stub<ErrorView>? = null

    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FreeTrialOnBoardViewModel::class.java)
    }

    companion object {
        fun newInstance() = ChooseGoalOnBoardFragment()
    }

    override fun initViewBinding() {
        binding.let {
            viewModel.isLanguageFragment.set(false)
            binding.handler = viewModel
            binding.executePendingBindings()
        }
    }

    override fun initViewState() {
        initRV()
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun setArguments() {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_choose_language_onboard, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        errorView = Stub(view.findViewById(R.id.error_view))
        if (isInternetAvailable()) {
            viewModel.getAvailableCourseGoals()
            errorView?.resolved()?.let {
                errorView!!.get().onSuccess()
            }
        } else {
            errorView?.resolved().let {
                errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                    override fun onRetryButtonClicked() {
                        viewModel.getAvailableCourseGoals()
                    }
                })
            }
        }
    }

    private fun addObservers() {
        viewModel.availableGoals.observe(viewLifecycleOwner) {
            errorView?.resolved()?.let {
                errorView!!.get().onSuccess()
            }
            if (it.isNullOrEmpty().not()) {
                goalAdapter.setData(it)
            }
        }
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(activity)
        goalAdapter.setGoalItemClickListener(this::onGoalSelected)
        binding.rvChooseLanguage.apply {
            layoutManager = linearLayoutManager
            adapter = goalAdapter
        }
    }


    fun onGoalSelected(goalSelectionResponse: GoalSelectionResponse) {
        if (goalSelectionResponse.testId != null) {
            viewModel.postGoal(GoalKeys.GOVT_EXAMS_SELECTED)
        }
        try {
            (requireActivity() as FreeTrialOnBoardActivity).showStartTrialPopup(
                goalSelectionResponse.testId ?: HINDI_TO_ENGLISH_TEST_ID
            )
        } catch (e: Exception) {
            showToast(getString(R.string.something_went_wrong))
        }
    }

    fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}