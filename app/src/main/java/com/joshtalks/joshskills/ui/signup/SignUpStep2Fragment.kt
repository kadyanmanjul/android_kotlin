package com.joshtalks.joshskills.ui.signup

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.*
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.Utils
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
import java.util.concurrent.TimeUnit

class SignUpStep2Fragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() =
            SignUpStep2Fragment().apply {
            }
    }

    private lateinit var signUpStep2Binding: FragmentSignUpStep2Binding

    private lateinit var viewModel: SignUpViewModel
    private val compositeDisposable = CompositeDisposable()
    private var timer: CountDownTimer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel = activity?.run {
            ViewModelProvider(activity!!)
                .get(SignUpViewModel::class.java)
        }
            ?: throw Exception("Invalid Activity")
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
        viewModel.progressDialogStatus.observe(this, Observer {
            if (it.not()) {
                hideProgress()
            }
        })
        viewModel.signUpStatus.observe(this, Observer {
            when (it) {
                SignUpStepStatus.SignUpResendOTP -> {
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
        viewModel.otpVerifyStatus.observe(this, Observer {
            if (it) {
                StyleableToast.Builder(activity!!).gravity(Gravity.CENTER)
                    .text(getString(R.string.wrong_otp)).cornerRadius(16).length(Toast.LENGTH_LONG)
                    .solidBackground().show()
                hideProgress()
                signUpStep2Binding.otpView.setText(EMPTY)

            }
        })

        signUpStep2Binding.tvMobile.text = viewModel.countryCode.plus(viewModel.phoneNumber)

    }

    private fun initProgressView() {
        bindProgressButton(signUpStep2Binding.btnVerify)
        signUpStep2Binding.btnVerify.attachTextChangeAnimator()

    }


    fun verifyOTP() {
        if (signUpStep2Binding.otpView.text.isNullOrEmpty().not() || viewModel.otpField.get().isNullOrEmpty().not()) {
            showProgress()
            viewModel.verifyOTP(signUpStep2Binding.otpView.text?.toString())
        } else {
            Toast.makeText(
                AppObjectController.joshApplication,
                "Please enter OTP",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
    }

    fun resendOTP() {
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
        timer = object : CountDownTimer(60_000, 1000) {
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
                    signUpStep2Binding.tvResendMessage.text = getString(R.string.resend_otp)
                    signUpStep2Binding.tvResendMessage.isEnabled = true
                }
            }
        }
        timer?.start()
    }


}