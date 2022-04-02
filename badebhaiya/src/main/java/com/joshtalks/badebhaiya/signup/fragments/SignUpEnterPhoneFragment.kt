package com.joshtalks.badebhaiya.signup.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.showProgress
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.TemporaryFeedActivity
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentSignupEnterPhoneBinding
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import com.truecaller.android.sdk.*


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
        return binding.root
    }

    private fun initTrueCallerUI() {
        val trueScope = TruecallerSdkScope.Builder(requireActivity(), sdkCallback)
            .consentMode(TruecallerSdkScope.CONSENT_MODE_BOTTOMSHEET)
            .ctaTextPrefix(TruecallerSdkScope.CTA_TEXT_PREFIX_CONTINUE_WITH)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_ANOTHER_METHOD)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()
        TruecallerSDK.init(trueScope)
        if(TruecallerSDK.getInstance().isUsable())
        {
            TruecallerSDK.getInstance().getUserProfile(this)
        }
        else
        {
            showToast("Internet Error")
        }
    }







    /*override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup_enter_phone, container, false)
            binding.lifecycleOwner = this
            binding.handler = this
            return binding.root
        }*/
    private val sdkCallback: ITrueCallback = object : ITrueCallback {
        override fun onSuccessProfileShared(@NonNull trueProfile: TrueProfile) {
              var intent = Intent(activity, TemporaryFeedActivity::class.java)
            startActivity(intent)
        }
        override fun onFailureProfileShared(@NonNull trueError: TrueError) {
            showToast("Verification Failed. Try Again"+trueError.errorType)
        }
        override fun onVerificationRequired(@Nullable trueError: TrueError) {
            showToast("Verification Required")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTrueCallerUI()
    }

    fun loginViaTrueCaller() {
        showToast("loginViaTrueCaller")
        //initTrueCallerUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        TruecallerSDK.getInstance().onActivityResultObtained(requireActivity(),requestCode,resultCode,data)
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
