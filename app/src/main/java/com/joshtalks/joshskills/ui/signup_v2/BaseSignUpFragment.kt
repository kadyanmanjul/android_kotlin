package com.joshtalks.joshskills.ui.signup_v2

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.facebook.FacebookSdk.getApplicationContext
import com.google.android.gms.auth.api.credentials.*
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.interfaces.OnSelectVerificationMethodListener
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaEventBus
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.sinch.verification.*
import com.truecaller.android.sdk.TrueException
import com.truecaller.android.sdk.TruecallerSDK
import com.truecaller.android.sdk.clients.VerificationCallback
import com.truecaller.android.sdk.clients.VerificationDataBundle
import timber.log.Timber


private const val MOBILE_NUMBER_HINT_REQUEST_CODE = 9001
private const val EMAIL_ID_HINT_REQUEST_CODE = 9002

open class BaseSignUpFragment : Fragment(), OnSelectVerificationMethodListener {

    private val sinchConfig: Config = SinchVerification.config()
        .applicationKey(BuildConfig.SINCH_API_KEY)
        .appHash(AppSignatureHelper(AppObjectController.joshApplication).appSignatures[0])
        .context(getApplicationContext())
        .build()


    protected open fun onVerificationPermissionDeny() {}

    protected open fun onVerificationNumberStarting() {}
    protected open fun onVerificationNumberCompleted() {}
    protected open fun onVerificationNumberFailed() {}

    protected open fun retryVerificationThrowFlashCall() {}
    protected open fun retryVerificationThrowSms() {}


    protected fun mobileNumberHint() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .setEmailAddressIdentifierSupported(false)
            .build()
        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        val pendingIntent =
            Credentials.getClient(requireContext(), options).getHintPickerIntent(hintRequest)
        startIntentSenderForResult(
            pendingIntent.intentSender,
            MOBILE_NUMBER_HINT_REQUEST_CODE,
            null,
            0,
            0,
            0,
            null
        )
    }

    protected fun emailSelectionHint() {
        val hintRequest = HintRequest.Builder()
            .setHintPickerConfig(
                CredentialPickerConfig.Builder()
                    .setShowCancelButton(true)
                    .build()
            )
            .setPhoneNumberIdentifierSupported(false)
            .setEmailAddressIdentifierSupported(true)
            .setAccountTypes(IdentityProviders.GOOGLE)
            .build()
        val options = CredentialsOptions.Builder()
            .forceEnableSaveDialog()
            .build()
        val pendingIntent =
            Credentials.getClient(requireContext(), options).getHintPickerIntent(hintRequest)
        startIntentSenderForResult(
            pendingIntent.intentSender,
            EMAIL_ID_HINT_REQUEST_CODE,
            null,
            0,
            0,
            0,
            null
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            val credential: Credential? =
                data?.getParcelableExtra(Credential.EXTRA_KEY)
            credential?.id?.run {
                if (requestCode == MOBILE_NUMBER_HINT_REQUEST_CODE) {
                } else if (requestCode == EMAIL_ID_HINT_REQUEST_CODE) {
                }

            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
    }


    open fun createVerification(
        countryCode: String,
        phoneNumber: String,
        service: VerificationService = VerificationService.SINCH,
        verificationVia: VerificationVia = VerificationVia.FLASH_CALL
    ) {
        when (service) {
            VerificationService.SINCH -> {
                AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
                    .addBasicParam()
                    .addUserDetails()
                    .addParam(AnalyticsEvent.LOGIN_VIA.NAME, AnalyticsEvent.SINCH_PARAM.NAME)
                    .push()
                verificationThroughSinch(phoneNumber, verificationVia)
            }
            VerificationService.TRUECALLER -> {

                verificationThroughTrueCaller(phoneNumber, verificationVia)
            }
            else -> {
                RxBus2.publish(
                    LoginViaEventBus(
                        LoginViaStatus.SMS_VERIFY,
                        countryCode,
                        phoneNumber
                    )
                )
            }
        }
    }


    //Use link = https://developers.sinch.com/docs/verification-for-android
    private fun verificationThroughSinch(phoneNumber: String, verificationVia: VerificationVia) {
        val listener = object : VerificationListener {
            override fun onInitiationFailed(e: Exception) {
                onVerificationNumberFailed()
                Timber.tag("Verification Number Sinch").e(e)
                e.printStackTrace()
                when (e) {
                    is InvalidInputException -> {
                        // Incorrect number provided
                    }
                    is ServiceErrorException -> {
                        // Verification initiation aborted due to early reject feature,
                        // client callback denial, or some other Sinch service error.
                        // Fallback to other verification method here.
                    }
                    else -> {
                        // Other system error, such as UnknownHostException in case of network error
                    }
                }
            }

            override fun onVerified() {
                Timber.tag("Verification Number Sinch").e("Sinch Verify Completed")
                onVerificationNumberCompleted()

            }

            override fun onInitiated(p0: InitiationResult) {
                onVerificationNumberStarting()
                Timber.tag("Verification Number Sinch").e("Sinch onInitiated")
            }

            override fun onVerificationFailed(e: Exception) {
                onVerificationNumberFailed()
                Timber.tag("Verification Number Sinch").e("onVerificationFailed")

                when (e) {
                    is InvalidInputException -> {
                        // Incorrect number or code provided
                    }
                    is CodeInterceptionException -> {
                        // Intercepting the verification code automatically failed, input the code manually with verify()
                    }
                    is IncorrectCodeException -> {
                        // The verification code provided was incorrect
                    }
                    is ServiceErrorException -> {
                        // Sinch service error
                    }
                    else -> {
                        // Other system error, such as UnknownHostException in case of network error
                    }
                }
                e.printStackTrace()
            }

            override fun onVerificationFallback() {
                Timber.tag("Verification Number Sinch").d("onVerificationFallback")
            }
        }


        val defaultRegion: String = PhoneNumberUtils.getDefaultCountryIso(requireContext())
        val phoneNumberInE164: String =
            PhoneNumberUtils.formatNumberToE164(phoneNumber, defaultRegion)

        if (verificationVia == VerificationVia.FLASH_CALL) {
            flashCallVerificationPermissionCheck {
                verificationViaFLASHCallUsingSinch(sinchConfig, phoneNumberInE164, listener)
            }
        } else {
            verificationViaSMSUsingSinch(sinchConfig, phoneNumberInE164, listener)
        }
    }

    private fun verificationViaSMSUsingSinch(
        config: Config,
        phoneNumberInE164: String,
        listener: VerificationListener
    ) {
        val verification =
            SinchVerification.createSmsVerification(config, phoneNumberInE164, listener)
        verification.initiate()
    }

    private fun verificationViaFLASHCallUsingSinch(
        config: Config,
        phoneNumberInE164: String,
        listener: VerificationListener
    ) {
        val verification =
            SinchVerification.createFlashCallVerification(config, phoneNumberInE164, listener)
        verification.initiate()
    }

    //Use link = https://docs.truecaller.com/truecaller-sdk/android/integrating-with-your-app/verifying-non-truecaller-users
    private fun verificationThroughTrueCaller(
        phoneNumber: String,
        verificationVia: VerificationVia
    ) {
        val apiCallback: VerificationCallback = object : VerificationCallback {
            override fun onRequestSuccess(
                requestCode: Int,
                @Nullable extras: VerificationDataBundle?
            ) {
                Log.e("treucaller", "success")
                when (requestCode) {
                    VerificationCallback.TYPE_MISSED_CALL_INITIATED -> {
                        onVerificationNumberStarting()
                    }
                    VerificationCallback.TYPE_MISSED_CALL_RECEIVED -> {
                        onVerificationNumberCompleted()
                    }
                    VerificationCallback.TYPE_OTP_INITIATED -> {
                        onVerificationNumberStarting()
                    }
                    VerificationCallback.TYPE_OTP_RECEIVED -> {
                    }
                    VerificationCallback.TYPE_VERIFICATION_COMPLETE -> {
                        onVerificationNumberCompleted()
                    }
                    VerificationCallback.TYPE_PROFILE_VERIFIED_BEFORE -> {
                    }
                }
            }

            override fun onRequestFailure(requestCode: Int, @NonNull e: TrueException) {
                Timber.tag("Verification Number TrueCaller")
                    .e("code= $requestCode  " + e.exceptionMessage)

                if (requestCode == 4) {
                    return
                }
                if (requestCode == 3) {
                    createVerification(
                        EMPTY,
                        phoneNumber,
                        VerificationService.SINCH,
                        VerificationVia.FLASH_CALL
                    )
                    return
                }
                onVerificationNumberFailed()
            }
        }

        flashCallVerificationPermissionCheck {
            TruecallerSDK.getInstance()
                .requestVerification("IN", phoneNumber, apiCallback, requireActivity())
        }
    }

    fun flashCallVerificationFailed() {
        val prev =
            childFragmentManager.findFragmentByTag(VerificationTimeoutBottomSheet::class.java.name)
        if (prev != null) {
            return
        }

        val bottomSheetFragment = VerificationTimeoutBottomSheet.newInstance()
        bottomSheetFragment.show(
            childFragmentManager,
            VerificationTimeoutBottomSheet::class.java.name
        )
    }

    private fun flashCallVerificationPermissionCheck(callback: () -> Unit = {}) {
        Dexter.withContext(activity)
            .withPermissions(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.ANSWER_PHONE_CALLS
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            callback()
                            return@let
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            onVerificationPermissionDeny()
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.flash_call_verify_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    onVerificationPermissionDeny()
                    token?.continuePermissionRequest()
                }

            }).check()
    }

    override fun onSelect(verificationVia: VerificationVia) {
        if (verificationVia == VerificationVia.FLASH_CALL) {
            retryVerificationThrowFlashCall()
        } else {
            retryVerificationThrowSms()

        }

    }

}