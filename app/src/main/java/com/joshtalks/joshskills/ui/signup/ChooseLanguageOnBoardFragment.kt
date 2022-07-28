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
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.LANGUAGE_SELECTION_SCREEN_OPENED
import com.joshtalks.joshskills.core.Utils.isInternetAvailable
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.databinding.FragmentChooseLanguageOnboardBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.signup.adapters.ChooseLanguageAdapter
import com.joshtalks.joshskills.ui.special_practice.utils.ErrorView

class ChooseLanguageOnBoardFragment : BaseFragment() {
    private lateinit var binding: FragmentChooseLanguageOnboardBinding
    private var languageAdapter = ChooseLanguageAdapter()
    private var is100PointsActive = false
    private var isGovernmentCourseActive = false
    private var eftActive = false

    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FreeTrialOnBoardViewModel::class.java)
    }

    private var errorView: Stub<ErrorView>? = null

    companion object {
        fun newInstance() = ChooseLanguageOnBoardFragment()
    }

    override fun initViewBinding() {
        binding.let {
            viewModel.isLanguageFragment.set(true)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_choose_language_onboard, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
        viewModel.saveImpression(LANGUAGE_SELECTION_SCREEN_OPENED)
        errorView = Stub(view.findViewById(R.id.error_view))

//        if (Utils.isInternetAvailable().not()) {
//            binding.noInternetContainer.visibility = View.VISIBLE
//        } else {
//            binding.noInternetContainer.visibility = View.GONE
//            viewModel.getAvailableLanguages()
//        }
        if (isInternetAvailable()) {
            viewModel.getAvailableLanguages()
            errorView?.resolved()?.let {
                errorView!!.get().onSuccess()
            }
        } else {
            binding.progress.visibility = View.GONE
            errorView?.resolved().let {
                errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                    override fun onRetryButtonClicked() {
                        viewModel.getAvailableLanguages()
                    }
                })
            }
        }
    }

    private fun addObservers() {
        viewModel.availableLanguages.observe(viewLifecycleOwner) {
            errorView?.resolved()?.let {
                errorView!!.get().onSuccess()
            }
            if (it.isNullOrEmpty().not()) {
                languageAdapter.setData(it)
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
                ApiCallStatus.FAILED -> {
                    errorView?.resolved().let {
                        errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                            override fun onRetryButtonClicked() {
                                viewModel.getAvailableLanguages()
                            }
                        })
                    }
                }
                else -> {}
            }
        }
        viewModel.abTestRepository.apply {
            eftActive = isVariantActive(VariantKeys.EFT_ENABLED)
            is100PointsActive = isVariantActive(VariantKeys.POINTS_HUNDRED_ENABLED)
            isGovernmentCourseActive = isVariantActive(VariantKeys.ENGLISH_FOR_GOVT_EXAM_ENABLED)
        }
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(activity)
        languageAdapter.setLanguageItemClickListener(this::onLanguageSelected)
        binding.rvChooseLanguage.apply {
            layoutManager = linearLayoutManager
            adapter = languageAdapter
        }
    }


    fun onLanguageSelected(language: ChooseLanguages) {
        if (language.testId == HINDI_TO_ENGLISH_TEST_ID) {
            viewModel.postGoal(GoalKeys.HINDI_LANG_SELECTED)
        }
        try {
            if (language.testId == HINDI_TO_ENGLISH_TEST_ID && isGovernmentCourseActive) {
                (requireActivity() as FreeTrialOnBoardActivity).openGoalFragment()
            } else {
                language.let { (requireActivity() as FreeTrialOnBoardActivity).showStartTrialPopup(it.testId) }
            }
        } catch (e: Exception) {
            showToast(getString(R.string.something_went_wrong))
        }
    }

    fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}