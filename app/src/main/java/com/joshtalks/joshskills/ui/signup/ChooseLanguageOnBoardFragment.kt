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
import com.joshtalks.joshskills.core.IS_EFT_VARIENT_ENABLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.databinding.FragmentChooseLanguageOnboardBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.ui.signup.adapters.ChooseLanguageAdapter

class ChooseLanguageOnBoardFragment: BaseFragment() {
    private lateinit var binding: FragmentChooseLanguageOnboardBinding
    private var languageAdapter = ChooseLanguageAdapter()
    private var is100PointsActive = false
    private var eftActive = false
    lateinit var language: ChooseLanguages

    val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(FreeTrialOnBoardViewModel::class.java)
    }

    companion object {
        fun newInstance() = ChooseLanguageOnBoardFragment()
    }

    override fun initViewBinding() {
        binding.let {
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
        if (Utils.isInternetAvailable().not()) {
            binding.noInternetContainer.visibility = View.VISIBLE
        } else {
            binding.noInternetContainer.visibility = View.GONE
            viewModel.getAvailableLanguages()
        }
    }

    private fun addObservers() {
        viewModel.availableLanguages.observe(viewLifecycleOwner) {
            if (it.isNullOrEmpty().not()) {
                languageAdapter.setData(it)
            }
        }
        viewModel.eftABtestLiveData.observe(requireActivity()){ abTestCampaignData ->
            abTestCampaignData?.let { map ->
                eftActive =(map.variantKey == VariantKeys.EFT_ENABLED.NAME) && map.variableMap?.isEnabled == true
                PrefManager.put(IS_EFT_VARIENT_ENABLED, eftActive)
            }
        }
        viewModel.points100ABtestLiveData.observe(requireActivity()) { map ->
            if (map != null) {
                is100PointsActive =
                    (map.variantKey == VariantKeys.POINTS_HUNDRED_ENABLED.NAME) && map.variableMap?.isEnabled == true

                (requireActivity() as FreeTrialOnBoardActivity).showStartTrialPopup(
                    language,
                    is100PointsActive
                )
            }else{
                (requireActivity() as FreeTrialOnBoardActivity).showStartTrialPopup(
                    language,
                    false
                )
            }
        }
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(activity)
        languageAdapter.setLanguageItemClickListener(this::initABTest)
        binding.rvChooseLanguage.apply {
            layoutManager = linearLayoutManager
            adapter = languageAdapter
        }
    }


    fun initABTest(language: ChooseLanguages) {
        this.language = language
        viewModel.get100PCampaignData(CampaignKeys.HUNDRED_POINTS.NAME, CampaignKeys.EXTEND_FREE_TRIAL.name)
    }

    fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}