package com.joshtalks.joshskills.premium.ui.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.databinding.FragmentSignUpProfilePicUpdateBinding
import com.joshtalks.joshskills.premium.ui.userprofile.fragments.UserPicChooserFragment

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
                    FirebaseRemoteConfigKey.ADD_PROFILE_PHOTO_TEXT + com.joshtalks.joshskills.premium.core.PrefManager.getStringValue(
                        com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID, defaultValue = com.joshtalks.joshskills.premium.core.DEFAULT_COURSE_ID
                    )
                )
    }

    fun submitProfilePic() {
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
