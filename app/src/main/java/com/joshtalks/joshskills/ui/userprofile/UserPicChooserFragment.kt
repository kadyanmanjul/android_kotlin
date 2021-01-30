package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.UserPicChooserDialogBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus

class UserPicChooserFragment : BottomSheetDialogFragment() {

    private lateinit var binding: UserPicChooserDialogBinding
    private var isUserProfilePicEmpty: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BaseBottomSheetDialog)
        changeDialogConfiguration()
        arguments?.let {
            isUserProfilePicEmpty = it.getBoolean(IS_PROFILE_PIC_PRESENT)
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
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.user_pic_chooser_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
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

    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager?.beginTransaction()
            ft?.add(this, tag)
            ft?.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {
        }
    }

    fun delete() {
        RxBus2.publish(DeleteProfilePicEventBus(""))
        dismiss()
    }

    fun change() {
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .galleryOnly()
            .start()
        dismiss()
    }

    fun captureImage() {
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .cameraOnly()
            .start()
        dismiss()
    }

    companion object {
        const val TAG = "UserPicChooserFragment"
        const val IS_PROFILE_PIC_PRESENT = "is_profile_pic_present"
        fun newInstance(isProfileEmpty: Boolean) =
            UserPicChooserFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_PROFILE_PIC_PRESENT, isProfileEmpty)
                }
            }

        fun showDialog(
            supportFragmentManager: FragmentManager,
            isProfileEmpty: Boolean
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(isProfileEmpty)
                .show(supportFragmentManager, TAG)
        }
    }

}
