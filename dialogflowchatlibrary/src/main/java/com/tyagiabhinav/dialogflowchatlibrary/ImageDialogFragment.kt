package com.tyagiabhinav.dialogflowchatlibrary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.tyagiabhinav.dialogflowchatlibrary.databinding.ImageDialogFragmentBinding

class ImageDialogFragment : DialogFragment() {

    private lateinit var binding: ImageDialogFragmentBinding

    var title: String? = null
    var description: String? = null
    var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        arguments?.let {
            title = it.getString("title")
            description = it.getString("description")
            imageUrl = it.getString("imgUrl")
        }
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Light_NoTitleBar)
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.setLayout(width, height)
    }

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

        updateUi()

        (requireActivity() as ChatbotActivity).logEvent("thumbnail_enlarge $title")
        return binding.root
    }

    private fun updateUi() {
        binding.titleTv.text = title
        binding.timeTv.visibility = View.GONE
        binding.description.text = description
        println("imageulf $imageUrl")
        Glide.with(requireContext())
            .load(imageUrl)
            .into(binding.courseIv)
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
