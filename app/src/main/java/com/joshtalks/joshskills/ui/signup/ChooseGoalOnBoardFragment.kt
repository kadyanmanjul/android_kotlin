package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.REASON_GOVT_EXAM_CLICKED
import com.joshtalks.joshskills.core.REASON_OTHERS_CLICKED
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
        if (viewModel.availableGoals.value == null || viewModel.availableGoals.value.isNullOrEmpty()) {
            if (isInternetAvailable())
                viewModel.getAvailableCourseGoals()
            else
                showErrorView()
        }
    }

    fun showErrorView() {
        binding.progress.visibility = View.GONE
        errorView?.resolved().let {
            errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                override fun onRetryButtonClicked() {
                    if (isInternetAvailable()) {
                        viewModel.getAvailableCourseGoals()
                    } else {
                        errorView?.get()?.enableRetryBtn()
                        Snackbar.make(
                            binding.root,
                            getString(R.string.internet_not_available_msz),
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(getString(R.string.settings)) {
                                startActivity(
                                    Intent(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                                        else
                                            Settings.ACTION_WIRELESS_SETTINGS
                                    )
                                )
                            }.show()
                    }
                }
            })
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
        viewModel.apiStatus.observe(viewLifecycleOwner) {
            when (it) {
                ApiCallStatus.START -> binding.progress.visibility = View.VISIBLE
                ApiCallStatus.SUCCESS -> {
                    binding.progress.visibility = View.GONE
                    errorView?.resolved()?.let {
                        errorView!!.get().onSuccess()
                    }
                }
                ApiCallStatus.FAILED -> showErrorView()
                else -> {}
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
            viewModel.saveImpression(REASON_GOVT_EXAM_CLICKED)
            viewModel.postGoal(GoalKeys.GOVT_EXAMS_SELECTED)
        } else {
            viewModel.saveImpression(REASON_OTHERS_CLICKED)
        }
        try {
            (requireActivity() as FreeTrialOnBoardActivity).startFreeTrial(
                goalSelectionResponse.testId ?: HINDI_TO_ENGLISH_TEST_ID
            )
        } catch (e: Exception) {
            showToast(getString(R.string.something_went_wrong))
        }
    }
}