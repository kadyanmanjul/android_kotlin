package com.joshtalks.joshskills.common.ui.extra

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.common.databinding.FragmentSignupPermissionDialogBinding

class SignUpPermissionDialogFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSignupPermissionDialogBinding

    companion object {
        const val TAG = "SignUpPermissionDialogFragment"
        fun newInstance(): SignUpPermissionDialogFragment {
            return SignUpPermissionDialogFragment()
        }

        fun showDialog(supportFragmentManager: FragmentManager) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance()
                .show(supportFragmentManager, TAG)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_signup_permission_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.desc.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.SIGNIN_DIALOG_DESCRIPTION)
    }

    fun allow() {
        dismiss()
        navigateToSignUpScreen()
    }

    private fun navigateToSignUpScreen() {
//        val intent = Intent(requireActivity(), com.joshtalks.joshskills.auth.freetrail.SignUpActivity::class.java).apply {
//            putExtra(com.joshtalks.joshskills.auth.freetrail.FLOW_FROM, "inbox flow journey")
//        }
//        startActivity(intent)
//        requireActivity().finish()
    }
}