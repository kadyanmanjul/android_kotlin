package com.joshtalks.joshskills.ui.signup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentSignUpProfileForFreeTrialBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import kotlinx.android.synthetic.main.fragment_sign_up_profile.*
import kotlinx.android.synthetic.main.instruction_top_view_holder.view.*
import java.util.*

class SignUpProfileForFreeTrialFragment : BaseSignUpFragment() {

    private lateinit var viewModel: SignUpViewModel
    private lateinit var viewModelFreeTrial: FreeTrialOnBoardViewModel
    private lateinit var binding: FragmentSignUpProfileForFreeTrialBinding

    companion object {
        fun newInstance() = SignUpProfileForFreeTrialFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_sign_up_profile_for_free_trial,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUI()
        addObservers()
        binding.nameEditText.requestFocus()
        val imm: InputMethodManager? =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private fun initUI() {
        val user = User.getInstance()

        if (user.firstName.isNullOrEmpty().not()) {
            binding.nameEditText.setText(user.firstName)
            binding.nameEditText.isEnabled = true
        }
        val name = binding.nameEditText.text.toString()
        if (name != user.firstName) {
            user.firstName = name
        }
    }

    private fun addObservers() {
        binding.nameEditText.setOnEditorActionListener { v, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    submitProfile()
                    true
                }
                else -> false
            }
        }
        viewModel.signUpStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it) {
                SignUpStepStatus.ProfileCompleted -> {
                    viewModel.startFreeTrial(Mentor.getInstance().getId())
                }
                else -> {
                    hideProgress()
                    return@Observer
                }
            }
        })
        viewModel.apiStatus.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    hideProgress()
                    moveToInboxScreen()
                }
                else -> {
                    hideProgress()
                }
            }
        })
    }

    fun submitProfile() {
        activity?.let { hideKeyboard(it, binding.nameEditText) }
        if (binding.nameEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.name_error_toast))
            return
        }
        startProgress()
        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = binding.nameEditText.text?.toString() ?: EMPTY
        requestMap["is_free_trial"] = "Y"
        viewModel.completingProfile(requestMap, false)
        PrefManager.put(ONBOARDING_STAGE, OnBoardingStage.NAME_ENTERED.value)
    }

    private fun moveToInboxScreen() {
        AppAnalytics.create(AnalyticsEvent.FREE_TRIAL_ONBOARDING.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
            .push()
        val intent = Intent(requireActivity(), InboxActivity::class.java).apply {
            putExtra(FLOW_FROM, "free trial onboarding journey")
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }


    private fun startProgress() {
        binding.btnLogin.showProgress {
            buttonTextRes = R.string.plz_wait
            progressColors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnLogin.isEnabled = false
    }

    private fun hideProgress() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.hideProgress(R.string.register)
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        (activity as BaseActivity).showWebViewDialog(url)
    }

}
