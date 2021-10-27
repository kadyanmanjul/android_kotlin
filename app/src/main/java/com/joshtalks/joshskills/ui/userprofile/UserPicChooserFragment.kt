package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableField
import androidx.fragment.app.FragmentManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.UserPicChooserDialogBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus
import java.io.File

class UserPicChooserFragment : BottomSheetDialogFragment() {

    private lateinit var binding: UserPicChooserDialogBinding
    private var isUserProfilePicEmpty: Boolean = false
    private var isFromRegistration:Boolean=false
    private var isFromGroup : Boolean = false
    val header = ObservableField("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialog)
        changeDialogConfiguration()
        arguments?.let {
            isUserProfilePicEmpty = it.getBoolean(IS_PROFILE_PIC_PRESENT)
            isFromRegistration = it.getBoolean(IS_FROM_REGISTRATION)
            isFromGroup = it.getBoolean(IS_FROM_GROUP, false)
        }
    }


    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.WRAP_CONTENT
        params?.gravity = Gravity.BOTTOM
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = true
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.user_pic_chooser_dialog,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.close.visibility = View.VISIBLE
        if (isUserProfilePicEmpty.not()) {
            binding.deleteIcon.visibility = View.VISIBLE
            binding.removeText.visibility = View.VISIBLE
        }
        if(isFromGroup)
            header.set("Group Icon")
        else
            header.set(resources.getString(R.string.profile_photo))
        logChooserAnalyticsEvent(AnalyticsEvent.UPLOAD_PIC_CHOOSER_OPENED.NAME,isFromRegistration)
    }

    private fun logChooserAnalyticsEvent(eventName:String,paramvalue:Boolean) {
        AppAnalytics.create(eventName)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.IS_FROM_REGISTRATION_SCREEN.NAME,paramvalue)
            .push()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {
        }
    }

    fun delete() {
        logChooserAnalyticsEvent(AnalyticsEvent.DELETE_PIC.NAME,isFromRegistration)
        RxBus2.publish(DeleteProfilePicEventBus(""))
        dismiss()
    }

    fun change() {
        logChooserAnalyticsEvent(AnalyticsEvent.GALLERY_UPLOAD.NAME,isFromRegistration)
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .galleryOnly()
            .saveDir(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, "ImagePicker"))
            .start(ImagePicker.REQUEST_CODE)
        dismiss()
    }

    fun captureImage() {
        logChooserAnalyticsEvent(AnalyticsEvent.CAMERA_UPLOAD.NAME,isFromRegistration)
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .cameraOnly()
            .saveDir(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, "ImagePicker"))
            .start(ImagePicker.REQUEST_CODE)
        dismiss()
    }

    companion object {
        const val TAG = "UserPicChooserFragment"
        const val IS_PROFILE_PIC_PRESENT = "is_profile_pic_present"
        const val IS_FROM_REGISTRATION = "is_from_registration"
        const val IS_FROM_GROUP = "is_from_group"
        fun newInstance(isProfileEmpty: Boolean,isFromRegistration:Boolean, isFromGroup : Boolean = false) =
            UserPicChooserFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_PROFILE_PIC_PRESENT, isProfileEmpty)
                    putBoolean(IS_FROM_REGISTRATION, isFromRegistration)
                    putBoolean(IS_FROM_GROUP, isFromGroup)
                }
            }

        fun showDialog(
            supportFragmentManager: FragmentManager,
            isProfileEmpty: Boolean,
            isFromGroup : Boolean = false,
            isFromRegistration:Boolean=false
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(isProfileEmpty,isFromRegistration, isFromGroup = isFromGroup)
                .show(supportFragmentManager, TAG)
        }
    }

}
