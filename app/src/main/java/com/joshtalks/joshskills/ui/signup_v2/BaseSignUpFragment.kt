package com.joshtalks.joshskills.ui.signup_v2

import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.credentials.*
import com.joshtalks.joshskills.core.VerificationVia
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.interfaces.OnSelectVerificationMethodListener


private const val MOBILE_NUMBER_HINT_REQUEST_CODE = 9001
private const val EMAIL_ID_HINT_REQUEST_CODE = 9002

open class BaseSignUpFragment : Fragment(), OnSelectVerificationMethodListener {


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

    override fun onSelect(verificationVia: VerificationVia) {
        if (verificationVia == VerificationVia.FLASH_CALL) {
            retryVerificationThrowFlashCall()
        } else {
            retryVerificationThrowSms()
        }
    }
}