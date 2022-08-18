package com.joshtalks.badebhaiya.signup

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.Fade
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.transition.MaterialSharedAxis
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.io.AppDirectory
import com.joshtalks.badebhaiya.core.workers.WorkManagerAdmin
import com.joshtalks.badebhaiya.databinding.ActivitySignUpBinding
import com.joshtalks.badebhaiya.feed.Call
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.notifications.FCM_ACTIVE
import com.joshtalks.badebhaiya.notifications.FirebaseNotificationService
import com.joshtalks.badebhaiya.privacyPolicy.WebViewFragment
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.FCMData
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.fragments.*
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import com.joshtalks.badebhaiya.utils.PRIVACY_POLICY_URL
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.events.makeLinks
import com.truecaller.android.sdk.ITrueCallback
import com.truecaller.android.sdk.TrueError
import com.truecaller.android.sdk.TrueProfile
import com.truecaller.android.sdk.TruecallerSDK
import com.truecaller.android.sdk.TruecallerSdkScope
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.android.synthetic.main.activity_sign_up.btnWelcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity(), Call {

    private lateinit var binding: ActivitySignUpBinding
    private val viewModel by lazy {
        ViewModelProvider(this).get(SignUpViewModel::class.java)
    }

//    @Inject
//    lateinit var impressionsManager: ImpressionsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up)
        binding.handler = this
        binding.viewModel = viewModel
        //handleIntent()
        addObservers()
        setSpanText()
        //setOnClickListeners()

    }

//    private fun handleIntent() {
//        if (intent.getStringExtra(REDIRECT) == REDIRECT_TO_ENTER_NAME) {
//            binding.btnWelcome.visibility = View.GONE
//            openEnterNameFragment()
//        }
//        if (intent.getStringExtra(REDIRECT) == REDIRECT_TO_ENTER_PROFILE_PIC) {
//            binding.btnWelcome.visibility = View.GONE
//            openUploadProfilePicFragment()
//        }
//    }


    private fun setSpanText() {
        binding.termsOfServiceText.makeLinks(
            Pair(
                getString(R.string.privacy_policy),
                View.OnClickListener {
                    WebViewFragment.showDialog(supportFragmentManager, PRIVACY_POLICY_URL)
                }
            )
        )
    }

    override fun onStart() {
        super.onStart()
        initTrueCallerUI()
//        if (intent.getBooleanExtra(IS_REDIRECTED, false))

//            openTrueCallerBottomSheet()
//        }
    }

    private fun addObservers() {
        viewModel.signUpStatus.observe(this) {
            btnWelcome.visibility = View.GONE
            when (it) {
                SignUpStepStatus.RequestForOTP -> {
                    openOTPVerificationFragment()
                }
                SignUpStepStatus.NameMissing -> {
                    openEnterNameFragment()
                }
                SignUpStepStatus.ProfilePicMissing, SignUpStepStatus.NameEntered -> {
                    openUploadProfilePicFragment()
                }
                SignUpStepStatus.ProfilePicSkipped, SignUpStepStatus.ProfileCompleted, SignUpStepStatus.ProfilePicUploaded -> {
                    openNextActivity()
//                    finish()
                }

                SignUpStepStatus.SpeakerFollowed ->{
                    openSpeakerToFollow()
                }
            }
        }

        viewModel.openProfile.observe(this){
            ProfileFragment.openOnTop(supportFragmentManager, R.id.container, it)
        }
    }

    private fun openEnterPhoneNumberFragment() {
        binding.btnWelcome.visibility = View.GONE
        supportFragmentManager.commit(true) {
            val fragment=SignUpEnterPhoneFragment()
            fragment.apply {
                exitTransition = MaterialSharedAxis(
                    MaterialSharedAxis.Z,
                    /* forward= */ false
                ).apply {
                    duration = 500
                }
            }
            addToBackStack(SignUpEnterPhoneFragment::class.java.name)
            replace(
                R.id.container,
                fragment,
                SignUpEnterPhoneFragment::class.java.name
            )
        }
    }

    private fun openOTPVerificationFragment() {
        supportFragmentManager.commit(true) {
            val fragment=SignUpEnterOTPFragment()
            fragment.apply {
                exitTransition = MaterialSharedAxis(
                    MaterialSharedAxis.Z,
                    /* forward= */ false
                ).apply {
                    duration = 500
                }
            }
            replace(
                R.id.container,
                fragment,
                SignUpEnterOTPFragment::class.java.name
            )
        }
    }

    private fun openEnterNameFragment() {
        supportFragmentManager.commit(true) {
            val fragment=SignUpEnterNameFragment()
            fragment.apply {
                exitTransition = MaterialSharedAxis(
                    MaterialSharedAxis.Z,
                    /* forward= */ false
                ).apply {
                    duration = 500
                }
            }
            replace(
                R.id.container,
                fragment,
                SignUpEnterNameFragment::class.java.name
            )
        }
    }

    private fun openSpeakerToFollow()
    {
        val fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.root_view, PeopleToFollowFragment()).addToBackStack(null)
        fragmentTransaction.commit()
    }

    private fun openUploadProfilePicFragment() {
        supportFragmentManager.commit(true) {
            val fragment=SignUpAddProfilePhotoFragment()
            fragment.apply {
                exitTransition = MaterialSharedAxis(
                    MaterialSharedAxis.Z,
                    /* forward= */ false
                ).apply {
                    duration = 500
                }
            }
            replace(
                R.id.container,
                fragment,
                SignUpAddProfilePhotoFragment::class.java.name
            )
        }
    }

    private fun openNextActivity() {
        WorkManagerAdmin.appStartWorker()
        when {
            PrefManager.getBoolValue(IS_NEW_USER) -> {
                PeopleToFollowFragment.open(supportFragmentManager, R.id.container)
            }
            intent.extras?.getString(REDIRECT) == REDIRECT_TO_PROFILE_ACTIVITY -> {
                Log.i("CHECKGUEST", "openNextActivity: wapas profile Pe")
                //ProfileActivity.openProfileActivity(this, intent.extras?.getString(USER_ID) ?: EMPTY)
                val bundle = Bundle()
                val fragment=ProfileFragment()
                fragment.apply {
                    exitTransition = MaterialSharedAxis(
                        MaterialSharedAxis.Z,
                        /* forward= */ false
                    ).apply {
                        duration = 500
                    }
                }
                bundle.putString("user", intent.extras?.getString(USER_ID))
                bundle.putString("request_dialog",intent.extras?.getString("request_room"))
                supportFragmentManager.findFragmentByTag(ProfileFragment::class.java.simpleName)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.root_view, fragment, ProfileFragment::class.java.simpleName)
                    .commit()

                this@SignUpActivity.finishAffinity()

            }
            else -> Intent(this, FeedActivity::class.java).also { it ->
                startActivity(it)
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out)

                this@SignUpActivity.finishAffinity()

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val url = data?.data?.path ?: EMPTY
        if (url.isNotBlank() && resultCode == Activity.RESULT_OK) {
            val imageUpdatedPath = AppDirectory.getImageSentFilePath()
            AppDirectory.copy(url, imageUpdatedPath)
            viewModel.uploadMedia(imageUpdatedPath)
            return
        }

        PrefManager.put(IS_TC_INSTALLED, TruecallerSDK.getInstance().isUsable)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance()
                .onActivityResultObtained(this, requestCode, resultCode, data)
            return
        }
    }

    private fun initTrueCallerUI() {
        val trueScope = TruecallerSdkScope.Builder(this, sdkCallback)
            .consentMode(TruecallerSdkScope.CONSENT_MODE_BOTTOMSHEET)
            .ctaTextPrefix(TruecallerSdkScope.CTA_TEXT_PREFIX_CONTINUE_WITH)
            .consentTitleOption(TruecallerSdkScope.SDK_CONSENT_TITLE_VERIFY)
            .footerType(TruecallerSdkScope.FOOTER_TYPE_ANOTHER_METHOD)
            .sdkOptions(TruecallerSdkScope.SDK_OPTION_WITHOUT_OTP)
            .build()
        TruecallerSDK.init(trueScope)
        if (TruecallerSDK.getInstance().isUsable) {
            TruecallerSDK.getInstance().setLocale(Locale("en"))
        }
    }

    fun openTrueCallerBottomSheet() {
        if (viewModel.redirect == "ENTER_NAME") {
            binding.btnWelcome.visibility = View.GONE
            openEnterNameFragment()
        }
        else if (viewModel.redirect == "ENTER_PIC") {
            binding.btnWelcome.visibility = View.GONE
            openUploadProfilePicFragment()
        }
        else {
            if (TruecallerSDK.getInstance().isUsable) {
                TruecallerSDK.getInstance().getUserProfile(this)
            } else
                openEnterPhoneNumberFragment()
        }

    }

    private val sdkCallback: ITrueCallback = object : ITrueCallback {

        override fun onFailureProfileShared(trueError: TrueError) {
            openEnterPhoneNumberFragment()
        }

        override fun onVerificationRequired(p0: TrueError?) {
        }

        override fun onSuccessProfileShared(trueProfile: TrueProfile) {
            viewModel.trueCallerLogin(trueProfile)

        }
    }

    companion object {
        private const val REDIRECT = ""
        const val REDIRECT_TO_PROFILE_ACTIVITY = "redirect_to_profile_activity"
        const val REDIRECT_TO_ENTER_NAME = "REDIRECT_TO_ENTER_NAME"
        const val IS_REDIRECTED = "is_redirected"


        @JvmStatic
        fun start(context: Context, redirect: String? = null, userId: String? = null, isRedirected: Boolean = false, requestRoom:Boolean?=false) {
            Log.i("CHECKGUEST", "start: $redirect")
            val starter = Intent(context, SignUpActivity::class.java)
                .putExtra(IS_REDIRECTED, isRedirected)
                .putExtra(REDIRECT, redirect)
                .putExtra(USER_ID, userId)
                .putExtra("request_dialog", requestRoom)
            context.startActivity(starter)
        }

        fun getIntent(context: Context, redirect: String? = null, userId: String? = null): Intent =
            Intent(context, SignUpActivity::class.java)
                .putExtra(REDIRECT, redirect)
                .putExtra(USER_ID, userId)
    }

    override fun itemClick(userId: String) {
        val nextFrag = ProfileFragment()
        val bundle = Bundle()
        bundle.putString("user", userId) // use as per your need
        nextFrag.arguments = bundle
        this.supportFragmentManager.beginTransaction()
            .replace(R.id.find, nextFrag, "findThisFragment")
            //?.addToBackStack(null)
            .commit()
    }
}