package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.databinding.FragmentSignupAddProfilePhotoBinding
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.signup.UserPicChooserFragment
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpAddProfilePhotoFragment: Fragment() {
    private lateinit var binding: FragmentSignupAddProfilePhotoBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    companion object {
        fun newInstance() = SignUpAddProfilePhotoFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup_add_profile_photo, container, false)
        binding.handler = this
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as SignUpActivity).btnWelcome.visibility=View.VISIBLE
                //showToast("Back Pressed")
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@SignUpAddProfilePhotoFragment)
                        .commitAllowingStateLoss()
                    SignUpActivity.start(requireContext(), SignUpActivity.REDIRECT_TO_ENTER_PROFILE_PIC)
                }
            }
        })
        binding.btnNext.setOnClickListener(){
            showToast("Please Upload a Profile Pic.")
        }
        return binding.root
    }

//    override fun onDetach() {
//        super.onDetach()
//        showToast("Detach is called")
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
    }

    private fun addObservers() {
        viewModel.profilePicUploadApiCallStatus.observe(viewLifecycleOwner) {
            when(it) {
                ApiCallStatus.START -> { startProgress() }
                ApiCallStatus.FAILED -> { hideProgress() }
            }
        }
    }

    fun submitProfilePic() {
        UserPicChooserFragment.showDialog(childFragmentManager, true)
    }

    fun onSkipPressed() {
        viewModel.changeSignUpStepStatusToSkip()
        //startActivityForState()
        PrefManager.put(SKIP,true)
        //SignUpActivity.start(requireContext(), SignUpActivity.REDIRECT_PROFILE_SKIPPED)
    }

    private fun startProgress() {
        binding.btnNext.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8

        }
        binding.btnNext.isEnabled = false
    }

    private fun hideProgress() {
        binding.btnNext.isEnabled = true
        binding.btnNext.hideProgress(R.string.next)
    }
}