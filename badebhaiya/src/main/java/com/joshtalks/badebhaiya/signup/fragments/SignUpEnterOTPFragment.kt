package com.joshtalks.badebhaiya.signup.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.RxBus2
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.FragmentSignupEnterOtpBinding
import com.joshtalks.badebhaiya.repository.eventbus.OTPReceivedEventBus
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import com.joshtalks.codeinputview.OTPListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SignUpEnterOTPFragment: Fragment() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var binding: FragmentSignupEnterOtpBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }
    companion object {
        fun newInstance() = SignUpEnterOTPFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_signup_enter_otp, container, false)
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvPhoneNumberText.text = getString(R.string.verify_otp_for_phone_text)
        processOTP()
        addOTPObserver()
    }

    private fun processOTP() {
        binding.otpView.otpListener = object : OTPListener {
            override fun onOTPComplete(otp: String?) {
                verifyOTP()
            }

            override fun onInteractionListener() {}
        }
    }

    private fun addOTPObserver() {
        viewModel.signUpStatus.observe(viewLifecycleOwner) {
            when(it) {
                SignUpStepStatus.ReGeneratedOTP -> {
                    binding.otpView.otp = EMPTY
                    hideProgress()
                }
                SignUpStepStatus.WRONG_OTP -> {
                    binding.otpView.otp = EMPTY
                    showToast(getString(R.string.wrong_otp))
                    hideProgress()
                }
            }
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(OTPReceivedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.i("BadeBhaiya", "onReceive: otp: $it")
                    binding.otpView.otp = it.otp
                }, {
                    it.printStackTrace()
                })
        )
    }

    fun verifyOTP() {
        if (binding.otpView.otp.isNullOrEmpty().not()) {
            startProgress()
            viewModel.verifyOTP(binding.otpView.otp, "9013207656")
        } else {
            showToast(getString(R.string.please_enter_otp))
        }
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

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}