package com.joshtalks.joshskills.ui.signup

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentSignUpStep2Binding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OTPReceivedEventBus
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SignUpStep2Fragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = SignUpStep2Fragment()
    }

    private lateinit var signUpStep2Binding: FragmentSignUpStep2Binding
    private lateinit var viewModel: SignUpViewModel
    private val compositeDisposable = CompositeDisposable()
    private var timer: CountDownTimer? = null
    private lateinit var appAnalytics: AppAnalytics
    private val currentTime=System.currentTimeMillis()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel = activity?.run {
            ViewModelProvider(requireActivity())
                .get(SignUpViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        signUpStep2Binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_step2, container, false)
        signUpStep2Binding.lifecycleOwner = this
        signUpStep2Binding.signUpViewModel = viewModel
        signUpStep2Binding.handler = this
        return signUpStep2Binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        signUpStep2Binding.otpView.setText(EMPTY)
        startTimer()
        activity?.findViewById<View>(R.id.back_tv)?.visibility = View.VISIBLE
        signUpStep2Binding.otpView.requestFocus()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        initProgressView()
        signUpStep2Binding.otpView.setOtpCompletionListener {
            verifyOTP()
        }
        viewModel.progressDialogStatus.observe(viewLifecycleOwner, Observer {
            if (it.not()) {
                hideProgress()
            }
        })
        viewModel.signUpStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                SignUpStepStatus.SignUpResendOTP -> {
                    viewModel.incrementResendAttempts()
                    Toast.makeText(
                        AppObjectController.joshApplication,
                        getString(R.string.resend_otp_toast, viewModel.phoneNumber),
                        Toast.LENGTH_SHORT
                    ).show()
                    startTimer()
                }

                else -> return@Observer
            }
        })
        viewModel.otpVerifyStatus.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.incrementIncorrectAttempts()

                StyleableToast.Builder(requireActivity()).gravity(Gravity.CENTER)
                    .text(getString(R.string.wrong_otp)).cornerRadius(16).length(Toast.LENGTH_LONG)
                    .solidBackground().show()
                hideProgress()
                signUpStep2Binding.otpView.setText(EMPTY)
            }
        })
        signUpStep2Binding.tvMobile.text = viewModel.countryCode.plus(viewModel.phoneNumber)
        appAnalytics=AppAnalytics.create(AnalyticsEvent.ENTER_OTP_SCREEN.NAME)
            .addBasicParam()
            .addParam(AnalyticsEvent.USER_DETAILS.NAME,viewModel.countryCode.plus(viewModel.phoneNumber))

    }

    private fun initProgressView() {
        bindProgressButton(signUpStep2Binding.btnVerify)
        signUpStep2Binding.btnVerify.attachTextChangeAnimator()
    }


    fun verifyOTP() {
        if (signUpStep2Binding.otpView.text.isNullOrEmpty().not() || viewModel.otpField.get()
                .isNullOrEmpty().not()
        ) {
            if (Utils.isInternetAvailable().not()) {
                showToast(getString(R.string.internet_not_available_msz))
                appAnalytics.addParam(AnalyticsEvent.NEXT_OTP_CLICKED.NAME,"Internet Not Available")
                return
            }
            showProgress()
            viewModel.verifyOTP(signUpStep2Binding.otpView.text?.toString())
            appAnalytics.addParam(AnalyticsEvent.NEXT_OTP_CLICKED.NAME,"Otp Submitted")

        } else {
            showToast(getString(R.string.please_enter_otp))
            appAnalytics.addParam(AnalyticsEvent.NEXT_OTP_CLICKED.NAME,"Blank Otp Submitted")
            return
        }
    }

    fun resendOTP() {
        Timber.tag(TAG).e("************* resend")
        showProgress()
        viewModel.resendOTP(viewModel.phoneNumber)
    }


    private fun showProgress() {
        signUpStep2Binding.btnVerify.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(Color.WHITE, Color.RED, Color.GREEN)
            gravity = DrawableButton.GRAVITY_TEXT_END
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        signUpStep2Binding.btnVerify.isEnabled = false
        signUpStep2Binding.tvResendMessage.isEnabled = false
    }


    private fun hideProgress() {
        signUpStep2Binding.tvResendMessage.isEnabled = true
        signUpStep2Binding.btnVerify.isEnabled = true
        signUpStep2Binding.btnVerify.hideProgress(getString(R.string.next))
    }


    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onStop() {
        super.onStop()
        timer?.cancel()
        timer = null
        Timber.tag(TAG).e("************* backgrounded")
        Log.d(TAG, "onStop() called  ${viewModel.incorrectAttempt} ${viewModel.resendAttempt}")
        appAnalytics.addParam(AnalyticsEvent.INCORRECT_OTP_ATTEMPTS.NAME,viewModel.incorrectAttempt)
        appAnalytics.addParam(AnalyticsEvent.NO_OF_TIMES_OTP_SEND.NAME,viewModel.resendAttempt)
        appAnalytics.addParam(AnalyticsEvent.TIME_TAKEN.NAME.plus("(in ms"),System.currentTimeMillis()-currentTime)
        appAnalytics.push()
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        compositeDisposable.clear()

    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(OTPReceivedEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    signUpStep2Binding.otpView.setText(it.otp)
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun startTimer() {
        timer = object : CountDownTimer(10_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (timer != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            signUpStep2Binding.tvResendMessage.text = getString(
                                R.string.resend_timer_text,
                                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished).toString()
                            )
                            signUpStep2Binding.tvResendMessage.isEnabled = false
                        } catch (ex: Exception) {
                        }
                    }
                }
            }

            override fun onFinish() {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        signUpStep2Binding.tvResendMessage.text = getString(R.string.resend_otp)
                        signUpStep2Binding.tvResendMessage.isEnabled = true
                    } catch (ex: Exception) {
                    }
                }
            }
        }
        timer?.start()
    }


}