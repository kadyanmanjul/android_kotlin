package com.joshtalks.joshskills.ui.signup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.crashlytics.android.Crashlytics
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.SignUpStepStatus
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentLoginDialogBinding
import com.truecaller.android.sdk.*
import io.reactivex.disposables.CompositeDisposable
import java.util.*

class LoginDialogFragment : DialogFragment() {

    private var compositeDisposable = CompositeDisposable()
    private lateinit var binding: FragmentLoginDialogBinding
    private lateinit var viewModel: SignUpViewModel
    private var listener: OnLoginCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        viewModel = activity?.run { ViewModelProvider(this).get(SignUpViewModel::class.java) }
            ?: throw Exception("Invalid Activity")
    }


    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = AppObjectController.screenWidth * .9
            dialog.window?.setLayout(width.toInt(), FrameLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_login_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTrueCallerSDK()
        addObserver()
    }

    private fun initTrueCallerSDK() {
        try {
            val trueScope = TrueSdkScope.Builder(requireContext(), trueCallerSDKCallback)
                .consentMode(TrueSdkScope.CONSENT_MODE_POPUP)  //TrueSdkScope.CONSENT_MODE_POPUP
                .consentTitleOption(TrueSdkScope.SDK_CONSENT_TITLE_VERIFY)
                .footerType(TrueSdkScope.FOOTER_TYPE_CONTINUE) //TrueSdkScope.FOOTER_TYPE_CONTINUE
                .build()

            TrueSDK.init(trueScope)
            if (TrueSDK.getInstance().isUsable) {
                val locale = Locale("en")
                TrueSDK.getInstance().setLocale(locale)
                binding.orRl.visibility = View.VISIBLE
                binding.btnTruecallerLogin.visibility = View.VISIBLE
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun addObserver() {
        viewModel.signUpStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            hideProgress()
            when (it) {
                SignUpStepStatus.SignUpCompleted -> {
                    dismissAllowingStateLoss()
                    return@Observer
                }

                else -> return@Observer
            }
        })
        viewModel.progressDialogStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it) {
                hideProgress()
            }
        })
    }


    private val trueCallerSDKCallback: ITrueCallback = object : ITrueCallback {
        override fun onSuccessProfileShared(@NonNull trueProfile: TrueProfile) {
            showProgress()
            viewModel.verifyUserViaTrueCaller(trueProfile)
        }

        override fun onVerificationRequired() {
            Crashlytics.log(3, "Truecaller Issue 2", "onVerificationRequired")
        }

        override fun onFailureProfileShared(@NonNull trueError: TrueError) {
            if (trueError.errorType == TrueError.ERROR_TYPE_CONTINUE_WITH_DIFFERENT_NUMBER) {
                signUp()
            }
            Crashlytics.log(3, "Truecaller Issue", trueError.errorType.toString())
        }
    }

    fun verifyViaTrueCaller() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addParam("name", this.javaClass.simpleName)
            .addParam(AnalyticsEvent.TYPE_PARAM.NAME, AnalyticsEvent.TRUECALLER_PARAM.NAME)
            .push()
        TrueSDK.getInstance().getUserProfile(this)
        showProgress()
        AppObjectController.uiHandler.postDelayed({ hideProgress() }, 1500)
    }

    fun signUp() {
        AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
            .addBasicParam()
            .addParam("name",this.javaClass.simpleName)
            .addParam(AnalyticsEvent.TYPE_PARAM.NAME,AnalyticsEvent.MOBILE_OTP_PARAM.NAME)
            .push()
        startActivityForResult(Intent(requireActivity(), SignUpActivity::class.java).apply {
            putExtra(IS_ACTIVITY_FOR_RESULT, true)
            putExtra(FROM_ACTIVITY, this@LoginDialogFragment.javaClass.simpleName)

        }, 101)
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
            .addParam("name", javaClass.simpleName)
            .push()
    }

    private fun showProgress() {
        binding.btnTruecallerLogin.showProgress {
            buttonTextRes = R.string.login_with_truecaller_label
            progressColors = intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.squash_light),
                Color.WHITE,
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            gravity = DrawableButton.GRAVITY_TEXT_END
            progressRadiusRes = R.dimen.dp4
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp2
        }
        binding.btnTruecallerLogin.isEnabled = false

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (TrueSDK.getInstance().isUsable) {
                TrueSDK.getInstance().onActivityResultObtained(requireActivity(), resultCode, data)
            }
        } catch (ex: Exception) {
        }
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            listener?.onLoginSuccessfully()
            dismissAllowingStateLoss()
        }
    }

    private fun hideProgress() {
        try {
            binding.btnTruecallerLogin.isEnabled = true
            binding.btnTruecallerLogin.hideProgress(getString(R.string.login_with_truecaller_label))
        } catch (e: Exception) {
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLoginCallback) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = LoginDialogFragment()
    }

    interface OnLoginCallback {
        fun onLoginSuccessfully()
    }

}
