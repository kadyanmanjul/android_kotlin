package com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.base.BaseFragment
import com.joshtalks.joshskills.common.constants.START_USER_INTEREST_FRAGMENT
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.databinding.FragmentUserEnglishLevelBinding
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.viewmodels.CallInterestViewModel

class UserEnglishLevelFragment : com.joshtalks.joshskills.common.base.BaseFragment() {

    lateinit var binding : FragmentUserEnglishLevelBinding
    private val viewModel by lazy { ViewModelProvider(this)[CallInterestViewModel::class.java] }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_user_english_level, container, false)
        return binding.root
    }
    override fun initViewBinding() {
        viewModel.saveImpression(INTEREST_FORM_LEVEL_SCREEN_OPEN)
        binding.btnContinue.setOnClickListener {
            val level = binding.radioBtnGroup.checkedRadioButtonId
            viewModel.sendUserLevel(level)
            viewModel.sendEvent(com.joshtalks.joshskills.common.constants.START_USER_INTEREST_FRAGMENT)
            viewModel.saveImpression(INTEREST_FORM_LEVEL_SAVED)
        }

        binding.txtVHeading.text = AppObjectController.getFirebaseRemoteConfig().getString(
            FirebaseRemoteConfigKey.USER_ENGLISH_LEVEL.plus(
                PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID })
        )

        binding.btnLevelBeginner.setOnClickListener {
            binding.btnContinue.isEnabled = true
        }
        binding.btnLevelInter.setOnClickListener {
            binding.btnContinue.isEnabled = true
        }
        binding.btnLevelAdvanced.setOnClickListener {
            binding.btnContinue.isEnabled = true
        }

        binding.btnLevelBeginner.id = 1
        binding.btnLevelInter.id = 2
        binding.btnLevelAdvanced.id = 3

    }

    override fun initViewState() {}

    override fun setArguments() {}
}