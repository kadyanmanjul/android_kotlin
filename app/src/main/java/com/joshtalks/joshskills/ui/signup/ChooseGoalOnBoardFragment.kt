package com.joshtalks.joshskills.ui.signup

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.REASON_GOVT_EXAM_CLICKED
import com.joshtalks.joshskills.core.REASON_OTHERS_CLICKED
import com.joshtalks.joshskills.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.databinding.FragmentChooseGoalBinding
import com.joshtalks.joshskills.repository.server.GoalList
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.signup.adapters.ChooseGoalAdapter
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_CONTINUE
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_GOAL_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_BACK_PRESS
import com.joshtalks.joshskills.ui.special_practice.utils.ErrorView

class ChooseGoalOnBoardFragment : BaseFragment() {
    private lateinit var binding: FragmentChooseGoalBinding
    private var errorView: Stub<ErrorView>? = null
    private var testId: String = EMPTY
    var goalListAdapter = ChooseGoalAdapter()
    var goalList: GoalList? = null
    var reasonImpression: String = EMPTY
    val sb = StringBuilder()
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
            viewModel.isLanguageFragment.set(false)
            binding.handler = viewModel
            binding.executePendingBindings()
        }
    }

    override fun initViewState() {
        liveData.observe(this) {
            when (it.what) {
                CLICK_GOAL_CARD -> {
                    setGoalData(it.obj as GoalList)
                    binding.btnContinue.setBackgroundState(true)
                }
                CLICK_ON_BACK_PRESS -> {
                    requireActivity().onBackPressed()
                }
                CLICK_CONTINUE -> {
                    goalList?.let { onGoalSelected(it) }
                }

            }
        }
    }

    fun setGoalData(goalList: GoalList) {
        this.goalList = goalList
    }

    override fun setArguments() {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

    fun MaterialButton.setBackgroundState(boolean: Boolean, string: String? = "") {
        when (boolean) {
            true -> {
                this.isEnabled = true
                this.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.colorPrimary)
                this.setTextColor(ContextCompat.getColor(context, R.color.white))
                if (string?.isEmpty()?.not() == true)
                    this.text = "Continue"
            }
            false -> {
                this.isEnabled = false
                this.backgroundTintList = AppCompatResources.getColorStateList(context, R.color.light_shade_of_gray)
                this.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
        }
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
        Log.d("ChooseGoalOnBoardFragment.kt", "SAGAR => onGoalSelected:173 ${goalSelectionResponse.goal} $reasonImpression")
        if (reasonImpression == EMPTY || reasonImpression != goalSelectionResponse.goal) {
            reasonImpression = goalSelectionResponse.goal
            sb.append(reasonImpression)
        } else {
            sb.append("_${goalSelectionResponse.goal}")
        }
        Log.d("ChooseGoalOnBoardFragment.kt", "SAGAR => onGoalSelected:181 ${sb.toString()}")
        if (goalSelectionResponse.testId != null) {
            viewModel.saveImpression(sb.toString())
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