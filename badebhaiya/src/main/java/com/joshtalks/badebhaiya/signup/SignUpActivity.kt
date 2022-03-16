package com.joshtalks.badebhaiya.signup

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.databinding.ActivitySignUpBinding
import com.joshtalks.badebhaiya.signup.fragments.SignUpEnterOTPFragment
import com.joshtalks.badebhaiya.signup.fragments.SignUpEnterPhoneFragment
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel

class SignUpActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        binding.handler = viewModel
        openEnterPhoneNumberFragment()
        addObservers()
    }

    private fun addObservers() {
        viewModel.signUpStatus.observe(this) {
            when(it) {
                SignUpStepStatus.RequestForOTP -> {
                    openOTPVerificationFragment()
                }
                SignUpStepStatus.NameMissing -> {
                    openEnterNameFragment()
                }
                SignUpStepStatus.ProfilePicMissing -> {
                    openUploadProfilePicFragment()
                }
                SignUpStepStatus.ProfilePicSkipped, SignUpStepStatus.SignUpCompleted, SignUpStepStatus.ProfilePicUploaded -> {
                    openFeedActivity()
                    this@SignUpActivity.finishAffinity()
                }
            }
        }
    }

    private fun openEnterPhoneNumberFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpEnterPhoneFragment::class.java.name)
            replace(R.id.container, SignUpEnterPhoneFragment.newInstance(), SignUpEnterPhoneFragment::class.java.name)
        }
    }

    private fun openOTPVerificationFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpEnterOTPFragment::class.java.name)
            replace(R.id.container, SignUpEnterOTPFragment.newInstance(), SignUpEnterOTPFragment::class.java.name)
        }
    }

    private fun openEnterNameFragment() {

    }

    private fun openUploadProfilePicFragment() {

    }

    private fun openFeedActivity() {

    }
}