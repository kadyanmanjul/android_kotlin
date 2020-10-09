package com.tyagiabhinav.dialogflowchatlibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.tyagiabhinav.dialogflowchatlibrary.databinding.ImageDialogFragmentBinding

const val TRIAL_TEST_ID = 13

class ImageDialogFragment : DialogFragment() {

    private lateinit var binding: ImageDialogFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.image_dialog_fragment,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    fun cancel() {
        try {
            requireActivity().finish()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(title: String, description: String, imgUrl: String): ImageDialogFragment {
            val args = Bundle()
            args.putString("title", title)
            args.putString("description", description)
            args.putString("imgUrl", imgUrl)
            val fragment = ImageDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

}
