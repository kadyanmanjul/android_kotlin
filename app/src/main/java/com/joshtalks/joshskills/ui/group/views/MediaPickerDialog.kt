package com.joshtalks.joshskills.ui.group.views

import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.MediaPickerDialogBinding

class MediaPickerDialog: DialogFragment() {

    private lateinit var binding: MediaPickerDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.media_dialog_theme)
        changeDialogConfiguration()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        isCancelable = true
        binding = DataBindingUtil.inflate(inflater, R.layout.media_picker_dialog, container, false)
        binding.lifecycleOwner = this
        binding.dialog = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewBinding()
    }

    private fun initViewBinding() {
        // TODO("Not yet implemented")
    }

    private fun changeDialogConfiguration() {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun recordAudio(view: View) {
        showToast("Audio Clicked")
    }

    fun cameraCapture(view: View) {
        showToast("Camera Clicked")
//        ImagePicker.with(this)
//            .crop()
//            .cameraOnly()
//            .saveDir(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, "ImagePicker"))
//            .start(ImagePicker.REQUEST_CODE)
//        dismiss()
    }

    fun openGallery(view: View) {
        showToast("Gallery Clicked")
    }

    companion object {
        const val TAG = "MediaPickerDialog"

        fun newInstance() = MediaPickerDialog()

        fun showDialog(supportFragmentManager: FragmentManager) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val previous = supportFragmentManager.findFragmentByTag(TAG)
            if (previous != null) {
                fragmentTransaction.remove(previous)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance().show(supportFragmentManager, TAG)
        }
    }
}