package com.joshtalks.joshskills.ui.signup

import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.codeinputview.OTPListener
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.databinding.FragmentSignUpVerificationBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.eventbus.OTPReceivedEventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class SignUpVerificationFragment : Fragment() {
    private var compositeDisposable = CompositeDisposable()
    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpVerificationBinding
    private var lastTime = 0
    private var timer: CountDownTimer? = null

    companion object {
        fun newInstance() = SignUpVerificationFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
        viewModel.currentTime = System.currentTimeMillis()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        binding.otpView2.otpListener = object : OTPListener {
            override fun onOTPComplete(otp: String) {
                verifyOTP()
            }

            override fun onInteractionListener() {
            }
        }
        // STOPSHIP: Only for testing
        if (BuildConfig.DEBUG) {
            // TODO: uncomment this.
//            binding.otpView2.otp = "0000"
        }

        viewModel.signUpStatus.observe(viewLifecycleOwner, Observer {
            it?.run {
                if (this == SignUpStepStatus.ReGeneratedOTP || this == SignUpStepStatus.WRONG_OTP) {
                    binding.otpView2.otp = EMPTY
                    lastTime = 0
                    startVerificationTimer()
                }
                if (this == SignUpStepStatus.WRONG_OTP) {
                    viewModel.incrementIncorrectAttempts()
                    showToast(getString(R.string.wrong_otp))
                }
            }
            hideProgress()
        })
        viewModel.verificationStatus.observe(viewLifecycleOwner, Observer {
            it.run {
                when {
                    this == VerificationStatus.INITIATED -> {
                        binding.otpView2.otp = EMPTY
                        lastTime = 0
                        startVerificationTimer()
                    }
                    this == VerificationStatus.SUCCESS -> {
                        RxBus2.publish(
                            LoginViaEventBus(
                                LoginViaStatus.NUMBER_VERIFY,
                                viewModel.countryCode.get()!!,
                                viewModel.phoneNumber.get()!!
                            )
                        )
                    }
                    else -> {
                        onTimeoutSMSVerification()
                    }
                }
            }
            hideProgress()
        })
        viewModel.signUpStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                SignUpStepStatus.ERROR -> {
                    hideProgress()
                }
                else -> return@Observer
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
        timer?.cancel()
        binding.resendCodeTv.isEnabled = false
        timer = object : CountDownTimer(TIMEOUT_TIME, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                lastTime = millisUntilFinished.toInt()
                if (timer != null && isAdded && isVisible) {
                    AppObjectController.uiHandler.post {
                        binding.tvOtpTimer.visibility = View.VISIBLE
                        binding.tvOtpTimer.text = String.format(
                            "00:%02d",
                            TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)
                        )
                    }
                }
            }

            override fun onFinish() {
                onTimeoutSMSVerification()
            }
        }
        timer?.start()
    }

    fun onTimeoutSMSVerification() {
        AppObjectController.uiHandler.post {
            if (isResumed) {
//                binding.tvOtpTimer.text = EMPTY
//                binding.tvOtpTimer.visibility = View.GONE
                binding.resendCodeTv.isEnabled = true
            }
        }
    }

    fun verifyOTP() {
        MixPanelTracker.publishEvent(MixPanelEvent.SUBMIT_OTP).push()
        if (binding.otpView2.otp.isNullOrEmpty().not() || viewModel.otpField.get().isNullOrEmpty()
                .not()
        ) {
            /*if ((requireActivity() as SignUpActivity).verification != null) {
                viewModel.progressBarStatus.postValue(true)
                (requireActivity() as SignUpActivity).verification?.verify(binding.otpView2.otp)
            } else {*/
            startProgress()
            viewModel.postGoal(GoalKeys.OTP_SUBMITTED)
            viewModel.verifyOTP(binding.otpView2.otp)
            //}
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
        MixPanelTracker.publishEvent(MixPanelEvent.EDIT_NUMBER).push()
        requireActivity().onBackPressed()
    }

    fun regeneratedOTP() {
        /*if ((requireActivity() as SignUpActivity).verification != null) {
            (requireActivity() as SignUpActivity).createVerification(
                viewModel.countryCode,
                viewModel.phoneNumber
            )
        } else {*/
        viewModel.incrementResendAttempts()
        viewModel.regeneratedOTP()
        //}
    }

    private fun startProgress() {
        binding.btnVerify.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(ContextCompat.getColor(requireActivity(), R.color.white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8

        }
        binding.btnVerify.isEnabled = false
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        (activity as BaseActivity).showWebViewDialog(url)
    }
    private fun hideProgress() {
        binding.btnVerify.isEnabled = true
        binding.btnVerify.hideProgress(R.string.next)
    }
}