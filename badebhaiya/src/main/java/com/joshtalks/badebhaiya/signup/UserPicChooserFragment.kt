package com.joshtalks.badebhaiya.signup

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
import androidx.lifecycle.ViewModelProvider
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.RxBus2
import com.joshtalks.badebhaiya.databinding.UserPicChooserDialogBinding
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.signup.viewmodel.SignUpViewModel
import com.joshtalks.badebhaiya.utils.events.DeleteProfilePicEventBus
import java.io.File

class UserPicChooserFragment : BottomSheetDialogFragment() {

    private lateinit var binding: UserPicChooserDialogBinding
    private var isUserProfilePicEmpty: Boolean = false
    val header = ObservableField("")

    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(SignUpViewModel::class.java)
    }

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
        isCancelable = true
        binding = DataBindingUtil.inflate(inflater, R.layout.user_pic_chooser_dialog, container, false)
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

        header.set(resources.getString(R.string.profile_photo))
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
        RxBus2.publish(DeleteProfilePicEventBus(""))
        dismiss()
    }

    fun change() {
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .galleryOnly()
            .saveDir(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, "ImagePicker"))
            .start(ImagePicker.REQUEST_CODE)
        viewModel.sendEvent(Impression("CHOOSE_PIC","UPLOADED_PROFILE_PIC"))
        dismiss()
    }

    fun captureImage() {
        ImagePicker.with(this)
            .crop()                    //Crop image(Optional), Check Customization for more option
            .cameraOnly()
            .saveDir(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, "ImagePicker"))
            .start(ImagePicker.REQUEST_CODE)
        viewModel.sendEvent(Impression("CHOOSE_PIC","UPLOADED_PROFILE_PIC"))
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
            isProfileEmpty: Boolean,
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
