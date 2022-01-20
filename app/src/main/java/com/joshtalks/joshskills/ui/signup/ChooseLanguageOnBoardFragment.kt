package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.core.interfaces.OnChooseLanguage
import com.joshtalks.joshskills.databinding.FragmentChooseLanguageOnboardBinding
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.repository.server.LanguageSelectionResponse
import com.joshtalks.joshskills.ui.signup.adapters.ChooseLanguageAdapter

class ChooseLanguageOnBoardFragment : BaseFragment(), OnChooseLanguage {

    private lateinit var binding: FragmentChooseLanguageOnboardBinding
    private lateinit var languageAdapter: ChooseLanguageAdapter

    val viewModel by lazy {
        ViewModelProvider(this).get(FreeTrialOnBoardViewModel::class.java)
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

    }

    override fun setArguments() {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_choose_language_onboard,
            container,
            false
        )
//        languageAdapter = ChooseLanguageAdapter(LanguageSelectionResponse().availableLanguages, this@ChooseLanguageOnBoardFragment)
        languageAdapter = ChooseLanguageAdapter(
            LanguageSelectionResponse().availableLanguages,
            this@ChooseLanguageOnBoardFragment)
        binding.lifecycleOwner = this
        initRV()
//        viewModel.languagesResponse.observe(viewLifecycleOwner) {
//            languageAdapter.setData(it.availableLanguages)
//        }
//        languageAdapter.setData(LanguageSelectionResponse().availableLanguages)
        return binding.root
    }

    private fun initRV() {

        val linearLayoutManager = LinearLayoutManager(activity)
//        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.rvChooseLanguage.apply {
            setHasFixedSize(true)
            layoutManager = linearLayoutManager
//            languageAdapter = ChooseLanguageAdapter(this@ChooseLanguageOnBoardFragment)
            adapter = languageAdapter
        }
    }

    override fun selectLanguageOnBoard(language: ChooseLanguages) {
        Log.i("ayushg", "selectLanguageOnBoard: " + language.languageName + " :clicked!")
        (activity as FreeTrialOnBoardActivity).showStartTrialPopup()
        Log.i("ayushg", "after completing show dialog")
    }
}