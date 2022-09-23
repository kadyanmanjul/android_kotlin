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
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.databinding.FragmentChooseGoalBinding
import com.joshtalks.joshskills.repository.server.GoalList
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_GOAL_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_BACK_PRESS
import com.joshtalks.joshskills.ui.special_practice.utils.ErrorView

class ChooseGoalOnBoardFragment : BaseFragment() {
    private lateinit var binding: FragmentChooseGoalBinding
    private var errorView: Stub<ErrorView>? = null
    private var testId: String = EMPTY
    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FreeTrialOnBoardViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            testId = it.getString("TEST_ID", EMPTY)
        }
    }

    companion object {
        fun newInstance(testId: String) =
            ChooseGoalOnBoardFragment().apply {
                arguments = Bundle().apply {
                    putString("TEST_ID", testId)
                }
            }
    }

    override fun initViewBinding() {
        binding.let {
            binding.handler = viewModel
            binding.executePendingBindings()
        }
    }

    override fun initViewState() {
        liveData.observe(this) {
            when (it.what) {
                CLICK_GOAL_CARD -> {
                    onGoalSelected(it.obj as GoalList)
                }
                CLICK_ON_BACK_PRESS -> {
                    requireActivity().onBackPressed()
                }
            }
        }
    }

    override fun setArguments() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_choose_goal, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        errorView = Stub(view.findViewById(R.id.error_view))
        if (isInternetAvailable())
            viewModel.getAvailableCourseGoals(testId)
        else
            showErrorView()
    }

    fun showErrorView() {
        binding.progress.visibility = View.GONE
        errorView?.resolved().let {
            errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                override fun onRetryButtonClicked() {
                    if (isInternetAvailable()) {
                        viewModel.getAvailableCourseGoals(testId)
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
        viewModel.apiStatus.observe(viewLifecycleOwner) {
            when (it) {
                //   ApiCallStatus.START -> binding.progress.visibility = View.VISIBLE
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

    fun onGoalSelected(goalSelectionResponse: GoalList) {
        try {
            viewModel.saveImpression(goalSelectionResponse.goal)
            viewModel.postGoal(GoalKeys.GOVT_EXAMS_SELECTED)
            (requireActivity() as FreeTrialOnBoardActivity).startFreeTrial(
                goalSelectionResponse.testId ?: HINDI_TO_ENGLISH_TEST_ID
            )
        } catch (e: Exception) {
            showToast(getString(R.string.something_went_wrong))
        }
    }
}