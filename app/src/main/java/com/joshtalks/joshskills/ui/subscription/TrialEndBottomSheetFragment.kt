package com.joshtalks.joshskills.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.IS_SUBSCRIPTION_ENDED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.FragmentTrialEndBottomsheetBinding
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity

const val SUBSCRIPTION_TEST_ID = 122
const val TRIAL_TEST_ID = 13

class TrialEndBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTrialEndBottomsheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_trial_end_bottomsheet,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val isSubscriptionEnded = PrefManager.getBoolValue(IS_SUBSCRIPTION_ENDED, true)

        binding.txtTrialEndMsg.text = if (isSubscriptionEnded) {
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_END_SCREEN_MESSAGE)
        } else {
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.TRAIL_END_SCREEN_MESSAGE)
        }

        binding.btnUnlock.text = if (isSubscriptionEnded) {
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.SUBSCRIPTION_END_SCREEN_CTA_LABEL)
        } else {
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.TRAIL_END_SCREEN_CTA_LABEL)
        }
    }

    fun cancel() {
        try {
            requireActivity().finish()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun unlockCourses() {
        PaymentSummaryActivity.startPaymentSummaryActivity(
            requireActivity(),
            SUBSCRIPTION_TEST_ID.toString()
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = TrialEndBottomSheetFragment()
    }

}
