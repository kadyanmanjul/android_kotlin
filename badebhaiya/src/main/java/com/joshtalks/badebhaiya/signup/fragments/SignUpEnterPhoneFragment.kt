package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.showProgress
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.core.hideKeyboard
import com.joshtalks.badebhaiya.core.isValidFullNumber
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentSignupEnterPhoneBinding
import com.joshtalks.badebhaiya.signup.SignUpActivity
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.fragment_signup_enter_phone.*


class SignUpEnterPhoneFragment: Fragment() {
    private lateinit var binding: FragmentSignupEnterPhoneBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    companion object {
        fun newInstance() = SignUpEnterPhoneFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup_enter_phone, container, false)
        binding.handler = this
        //(activity as SignUpActivity).btnWelcome.visibility=View.GONE

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as SignUpActivity).btnWelcome.visibility=View.VISIBLE
                //showToast("Back Pressed")
                activity?.run {
                    supportFragmentManager.beginTransaction().remove(this@SignUpEnterPhoneFragment)
                        .commitAllowingStateLoss()
                }

            }
        })
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addObservers()
    }

    private fun addObservers() {

    }

    fun loginViaPhoneNumber() {
        if (binding.etPhone.text.isNullOrEmpty() || isValidFullNumber("+91", binding.etPhone.text.toString()).not()) {
            showToast(getString(R.string.please_enter_valid_number))
            return
        }

        hideKeyboard(requireActivity(), binding.etPhone)
        viewModel.sendPhoneNumberForOTP(binding.etPhone.text.toString(), "+91")
        var check=viewModel.valid.get()

        if(check) {
            startProgress()
            viewModel.signUpStatus.value = SignUpStepStatus.RequestForOTP
            startSmsListener()
        }
        else
        {
            showToast("Invalid Number")
        }
    }

        private fun startSmsListener(){
            val client = SmsRetriever.getClient(requireActivity() /* context */)
            val task = client.startSmsRetriever()
            task.addOnSuccessListener {
                showToast("Waiting for the OTP")
            }

            task.addOnFailureListener {
                showToast("Cannot Start SMS Retriever")
            }
        }


    private fun startProgress() {
        binding.btnNext.showProgress {
            progressColors =
                intArrayOf(ContextCompat.getColor(requireContext(), R.color.white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnNext.isEnabled = false
    }
}