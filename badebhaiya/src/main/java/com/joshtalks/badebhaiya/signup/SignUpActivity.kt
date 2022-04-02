package com.joshtalks.badebhaiya.signup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.TemporaryFeedActivity
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.core.USER_ID
import com.joshtalks.badebhaiya.core.io.AppDirectory
import com.joshtalks.badebhaiya.databinding.ActivitySignUpBinding
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.signup.fragments.SignUpAddProfilePhotoFragment
import com.joshtalks.badebhaiya.signup.fragments.SignUpEnterNameFragment
import com.joshtalks.badebhaiya.signup.fragments.SignUpEnterOTPFragment
import com.joshtalks.badebhaiya.signup.fragments.SignUpEnterPhoneFragment
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel

class SignUpActivity : AppCompatActivity() {

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
        initTrueCaller()
    }

    private fun initTrueCaller() {
        TODO("Not yet implemented")
    }

    private fun addObservers() {
        viewModel.signUpStatus.observe(this) {
            when (it) {
                SignUpStepStatus.RequestForOTP -> {
                    openOTPVerificationFragment()
                }
                SignUpStepStatus.NameMissing -> {
                    openEnterNameFragment()
                }
                SignUpStepStatus.ProfilePicMissing, SignUpStepStatus.NameEntered -> {
                    openUploadProfilePicFragment()
                }
                SignUpStepStatus.ProfilePicSkipped, SignUpStepStatus.ProfileCompleted, SignUpStepStatus.ProfilePicUploaded -> {
                    openNextActivity()
                    this@SignUpActivity.finishAffinity()
                }
            }
        }
    }

    private fun openEnterPhoneNumberFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpEnterPhoneFragment::class.java.name)
            replace(
                R.id.container,
                SignUpEnterPhoneFragment.newInstance(),
                SignUpEnterPhoneFragment::class.java.name
            )
        }
    }

    private fun openOTPVerificationFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpEnterOTPFragment::class.java.name)
            replace(
                R.id.container,
                SignUpEnterOTPFragment.newInstance(),
                SignUpEnterOTPFragment::class.java.name
            )
        }
    }

    private fun openEnterNameFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpEnterNameFragment::class.java.name)
            replace(
                R.id.container,
                SignUpEnterNameFragment.newInstance(),
                SignUpEnterNameFragment::class.java.name
            )
        }
    }

    private fun openUploadProfilePicFragment() {
        supportFragmentManager.commit(true) {
            addToBackStack(SignUpAddProfilePhotoFragment::class.java.name)
            replace(
                R.id.container,
                SignUpAddProfilePhotoFragment.newInstance(),
                SignUpAddProfilePhotoFragment::class.java.name
            )
        }
    }

    private fun openNextActivity() {
        if (intent.extras?.getString(REDIRECT) == REDIRECT_TO_PROFILE_ACTIVITY)
            ProfileActivity.openProfileActivity(this, intent.extras?.getString(USER_ID) ?: EMPTY)
        else
            TemporaryFeedActivity.openFeedActivity(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val url = data?.data?.path ?: EMPTY
        if (url.isNotBlank() && resultCode == Activity.RESULT_OK) {
            val imageUpdatedPath = AppDirectory.getImageSentFilePath()
            AppDirectory.copy(url, imageUpdatedPath)
            viewModel.uploadMedia(imageUpdatedPath)
        }
    }

    companion object {
        private const val REDIRECT = ""
        const val REDIRECT_TO_PROFILE_ACTIVITY = "redirect_to_profile_activity"

        @JvmStatic
        fun start(context: Context, redirect: String? = null, userId: String? = null) {
            val starter = Intent(context, SignUpActivity::class.java)
                .putExtra(REDIRECT, redirect)
                .putExtra(USER_ID, userId)
            context.startActivity(starter)
        }

        fun getIntent(context: Context, redirect: String? = null, userId: String? = null): Intent =
            Intent(context, SignUpActivity::class.java)
                .putExtra(REDIRECT, redirect)
                .putExtra(USER_ID, userId)
    }
}