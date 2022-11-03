package com.joshtalks.joshskills.ui.signup

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.FREE_TRIAL_ENTER_NAME_TEXT
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.databinding.FragmentSignUpProfileForFreeTrialBinding
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.chat.ConversationActivity
import com.joshtalks.joshskills.ui.inbox.InboxActivity

class SignUpProfileForFreeTrialFragment : BaseSignUpFragment() {

    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpProfileForFreeTrialBinding
    private var username = User.getInstance().firstName
    private var isUserVerified = User.getInstance().isVerified
    private var isNameEntered = false

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
        addObservers()
        binding.nameEditText.requestFocus()
        initUI()
    }

    private fun initUI() {
//        binding.toolbarLayout.ivBack.setOnClickListener {
//            requireActivity().onBackPressed()
//        }
        binding.textViewName.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FREE_TRIAL_ENTER_NAME_TEXT + PrefManager.getStringValue(FREE_TRIAL_TEST_ID, defaultValue = FREE_TRIAL_DEFAULT_TEST_ID))
        binding.nameEditText.setText(username)
        binding.nameEditText.isEnabled = true
    }

    private fun addObservers() {
        binding.nameEditText.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEND -> {
                    submitProfile()
                    true
                }
                else -> false
            }
        }
        viewModel.apiStatus.observe(viewLifecycleOwner, {
            when (it) {
                ApiCallStatus.START -> {
                    startProgress()
                    handleOnBackPressed(true)
                }
                ApiCallStatus.SUCCESS -> {
                    hideProgress()
                    moveToInboxScreen()
                    handleOnBackPressed(false)
                }
                else -> {
                    hideProgress()
                    handleOnBackPressed(false)
                }
            }
        })
    }

    fun submitProfile() {
        if (Utils.isInternetAvailable().not()){
            showToast(getString(R.string.internet_not_available_msz))
            return
        }
        viewModel.saveTrueCallerImpression(NAME_SUBMITTED)
        handleOnBackPressed(true)
        MarketingAnalytics.completeRegistrationAnalytics(
            false,
            RegistrationMethods.TRUE_CALLER
        )
        activity?.let { hideKeyboard(it, binding.nameEditText) }
        if (binding.nameEditText.text.isNullOrEmpty()) {
            showToast(getString(R.string.name_error_toast))
            return
        }
        submitForFreeTrial()
        binding.btnLogin.isEnabled = false

        val name = binding.nameEditText.text.toString()
        if(username.isNullOrEmpty()) {
            MixPanelTracker.publishEvent(MixPanelEvent.REGISTER_WITH_NAME)
                .addParam(ParamKeys.NAME_ENTERED,true)
                .push()
        }
        if (!username.isNullOrEmpty() && username != name) {
            if (!isNameEntered) {
                MixPanelTracker.publishEvent(MixPanelEvent.REGISTER_WITH_NAME)
                    .addParam(ParamKeys.NAME_CHANGED,true)
                    .push()
                viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_NAMECHANGED)
            }
        } else if(!username.isNullOrEmpty() && username==name) {
            MixPanelTracker.publishEvent(MixPanelEvent.REGISTER_WITH_NAME)
                .addParam(ParamKeys.NAME_CHANGED,false)
                .push()
        }
    }

    fun submitForFreeTrial() {
        val requestMap = mutableMapOf<String, String?>()
        requestMap["first_name"] = binding.nameEditText.text?.toString() ?: EMPTY
        requestMap["is_free_trial"] = "Y"
        viewModel.completingProfile(requestMap, isUserVerified)
        viewModel.postGoal(GoalKeys.NAME_SUBMITTED)
        viewModel.postGoal(GoalKeys.NAME_SELECTED)
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
            progressColors = intArrayOf(ContextCompat.getColor(requireContext(), R.color.pure_white))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnLogin.isEnabled = false
        binding.progressContainer.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.btnLogin.isEnabled = true
        binding.btnLogin.hideProgress(R.string.register)
        binding.progressContainer.visibility = View.GONE
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        (activity as BaseActivity).showWebViewDialog(url)
    }

    private fun handleOnBackPressed(enabled: Boolean) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(enabled){
                override fun handleOnBackPressed() {

                }
            })
    }
}
