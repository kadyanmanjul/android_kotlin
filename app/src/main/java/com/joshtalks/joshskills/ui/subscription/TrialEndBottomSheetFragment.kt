package com.joshtalks.joshskills.ui.subscription

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.databinding.FragmentTrialEndBottomsheetBinding
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import com.joshtalks.joshskills.ui.userprofile.UserPicChooserFragment

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable = false

        binding.txtTrialEndMsg.text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.FREE_TRIAL_DIALOG_TXT).replace("\\n", "\n")

        binding.btnUnlock.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.FREE_TRIAL_DIALOG_BTN_TXT)
    }

    fun cancel() {
        try {
            requireActivity().finish()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), R.style.BaseBottomSheetDialog) {
            override fun onBackPressed() {
                requireActivity().finish()
            }
        }
    }

    fun unlockCourses() {
        FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
            requireActivity(),
            AppObjectController.getFirebaseRemoteConfig().getString(
                FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
            )

        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = TrialEndBottomSheetFragment()

        fun showDialog(
            supportFragmentManager: FragmentManager
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(UserPicChooserFragment.TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance().show(supportFragmentManager, UserPicChooserFragment.TAG)
        }
    }

}
