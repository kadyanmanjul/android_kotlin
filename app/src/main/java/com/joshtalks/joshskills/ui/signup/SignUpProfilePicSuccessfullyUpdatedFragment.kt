package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentSignUpProfilePicSuccessfullyUpdatedBinding

class SignUpProfilePicSuccessfullyUpdatedFragment : BaseSignUpFragment() {
    private lateinit var viewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpProfilePicSuccessfullyUpdatedBinding

    companion object {
        const val TAG = "SignUpProfilePicSuccessfullyUpdatedFragment"
        fun newInstance() = SignUpProfilePicSuccessfullyUpdatedFragment()
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
                R.layout.fragment_sign_up_profile_pic_successfully_updated,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.picUploadedText.text=
            AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.PROFILE_PIC_SUCCESSFUL_TEXT)
    }

    fun startCourse() {
        logStartCourseEvent()
        viewModel.changeSignupStatusToStartAfterPicUploaded()
    }

    private fun logStartCourseEvent() {
        AppAnalytics.create(AnalyticsEvent.START_COURSE_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .push()
    }

}
