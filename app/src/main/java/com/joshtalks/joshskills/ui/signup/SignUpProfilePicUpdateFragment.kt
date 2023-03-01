package com.joshtalks.joshskills.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.databinding.FragmentSignUpProfilePicUpdateBinding
import com.joshtalks.joshskills.ui.userprofile.fragments.UserPicChooserFragment

class SignUpProfilePicUpdateFragment : BaseSignUpFragment() {

    private lateinit var binding: FragmentSignUpProfilePicUpdateBinding

    companion object {
        const val TAG = "SignUpProfilePicUpdateFragment"
        fun newInstance() = SignUpProfilePicUpdateFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_profile_pic_update, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.addPhoto.text = AppObjectController.getFirebaseRemoteConfig().getString(FirebaseRemoteConfigKey.ADD_PROFILE_PHOTO)
        binding.addPhotoText.text =
            AppObjectController.getFirebaseRemoteConfig()
                .getString(
                    FirebaseRemoteConfigKey.ADD_PROFILE_PHOTO_TEXT + PrefManager.getStringValue(
                        CURRENT_COURSE_ID, defaultValue = DEFAULT_COURSE_ID
                    )
                )
    }

    fun submitProfilePic(v:View) {
        //val requestMap = mutableMapOf<String, String?>()
        //viewModel.completingProfile(requestMap)
        MixPanelTracker.publishEvent(MixPanelEvent.ADD_PROFILE_PHOTO).push()
        logAnalyticsEvent(AnalyticsEvent.UPLOAD_PROFILE_PIC.NAME)
        UserPicChooserFragment.showDialog(
            childFragmentManager,
            true,
            isFromRegistration = true
        )
    }


    private fun logAnalyticsEvent(eventName:String) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .push()
    }
}
