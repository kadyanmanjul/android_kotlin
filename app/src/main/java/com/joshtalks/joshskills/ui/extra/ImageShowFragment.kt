package com.joshtalks.joshskills.ui.extra

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import kotlinx.android.synthetic.main.fragment_image_show.*


const val IMAGE_SOURCE = "image_source"

class ImageShowFragment : DialogFragment() {
    private lateinit var imagePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            imagePath = it.getString(IMAGE_SOURCE)
        }
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_show, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        big_image_view.showImage(Uri.parse(imagePath));


    }

    companion object {
        fun newInstance(path: String) = ImageShowFragment().apply {
            arguments = Bundle().apply {
                putString(IMAGE_SOURCE, path)
            }
        }
    }
}