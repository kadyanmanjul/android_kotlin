package com.joshtalks.joshskills.ui.signup_v2

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshlibrary.codeinputview.OTPListener
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentSignUpVerificationBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OTPReceivedEventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SignUpVerificationFragment : Fragment() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var viewModel: SignUpV2ViewModel
    private lateinit var binding: FragmentSignUpVerificationBinding
    private var lastTime = 0
    private var timer: CountDownTimer? = null

    companion object {
        fun newInstance() = SignUpVerificationFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpV2ViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_sign_up_verification,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // binding.otpView.setText(EMPTY)
        //  binding.otpView.requestFocus()
        binding.otpView2.otpListener = object : OTPListener {
            override fun onOTPComplete(otp: String) {
                verifyOTP()
            }

            override fun onInteractionListener() {
            }

        }

        binding.textView2.text = getString(
            R.string.otp_received_message,
            viewModel.countryCode + " " + viewModel.phoneNumber
        )
        binding.otpView.setOtpCompletionListener {
            verifyOTP()
        }
        viewModel.signUpStatus.observe(this, Observer {
            it?.run {
                if (this == SignUpStepStatus.ReGeneratedOTP || this == SignUpStepStatus.WRONG_OTP) {
                    binding.otpView2.otp = EMPTY
                    lastTime = 0
                    startVerificationTimer()
                }
                if (this == SignUpStepStatus.WRONG_OTP) {
                    showToast(getString(R.string.wrong_otp))
                }
            }
        })
        startVerificationTimer()
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        AppObjectController.uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        timer = null
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        compositeDisposable.clear()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(OTPReceivedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.otpView2.otp = it.otp
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun startVerificationTimer() {
        binding.otpCl.visibility = View.VISIBLE
        binding.otpResendCl.visibility = View.GONE
        timer = object : CountDownTimer(TIMEOUT_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                lastTime = millisUntilFinished.toInt()
                if (timer != null) {
                    if (isAdded && isVisible) {
                        AppObjectController.uiHandler.post {
                            binding.tvOtpTimer.visibility = View.VISIBLE
                            binding.tvOtpTimer.text = String.format(
                                "00:%02d",
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                            )
                        }
                    }
                }
            }

            override fun onFinish() {
                AppObjectController.uiHandler.post {
                    if (requireActivity().isFinishing.not() && isAdded && isVisible) {
                        binding.tvOtpTimer.text = EMPTY
                        binding.otpCl.visibility = View.GONE
                        binding.otpResendCl.visibility = View.VISIBLE
                    }
                }
            }
        }
        timer?.start()
    }

    fun verifyOTP() {

        if (binding.otpView2.otp.isNullOrEmpty().not() || viewModel.otpField.get().isNullOrEmpty()
                .not()
        ) {
            viewModel.verifyOTP(binding.otpView2.otp)
            AppAnalytics.create(AnalyticsEvent.OTP_SCREEN_SATUS.NAME)
                .addParam(AnalyticsEvent.NEXT_OTP_CLICKED.NAME, "Otp Submitted")
                .push()
        } else {
            showToast(getString(R.string.please_enter_otp))
            AppAnalytics.create(AnalyticsEvent.OTP_SCREEN_SATUS.NAME)
                .addParam(AnalyticsEvent.NEXT_OTP_CLICKED.NAME, "Blank Otp Submitted")
                .push()
        }
    }

    fun editNumber() {
        requireActivity().onBackPressed()
    }
}