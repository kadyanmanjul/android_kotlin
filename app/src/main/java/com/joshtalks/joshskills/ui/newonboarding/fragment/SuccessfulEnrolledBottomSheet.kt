package com.joshtalks.joshskills.ui.newonboarding.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.databinding.FragmentSuccessfulEnrolledBinding


class SuccessfulEnrolledBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentSuccessfulEnrolledBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_successful_enrolled,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.title.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.NEW_ONBOARD_FLOW_TEXT_ON_ENROLLED)
    }

    fun onStartLearningClicked() {
        startActivity((requireActivity() as BaseActivity).getInboxActivityIntent())
        requireActivity().finish()
    }

    companion object {
        const val TAG = "SuccessfulEnrolledBottomSheet"
        @JvmStatic
        fun newInstance() = SuccessfulEnrolledBottomSheet()
    }
}