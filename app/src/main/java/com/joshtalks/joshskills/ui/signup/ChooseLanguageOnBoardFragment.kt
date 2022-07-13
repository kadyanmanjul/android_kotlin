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
import com.joshtalks.joshskills.core.LANGUAGE_SELECTION_SCREEN_OPENED
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.databinding.FragmentChooseLanguageOnboardBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.ui.signup.adapters.ChooseLanguageAdapter

class ChooseLanguageOnBoardFragment : BaseFragment() {
    private lateinit var binding: FragmentChooseLanguageOnboardBinding
    private var languageAdapter = ChooseLanguageAdapter()
    private var is100PointsActive = false
    private var eftActive = false

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
        viewModel.saveImpression(LANGUAGE_SELECTION_SCREEN_OPENED)
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
        viewModel.abTestRepository.apply {
            eftActive = isVariantActive(VariantKeys.EFT_ENABLED)
            is100PointsActive = isVariantActive(VariantKeys.POINTS_HUNDRED_ENABLED)
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
        language.let {
            (requireActivity() as FreeTrialOnBoardActivity).showStartTrialPopup(
                it,
                is100PointsActive
            )
        }
    }

    fun onBackPressed() {
        requireActivity().onBackPressed()
    }
}