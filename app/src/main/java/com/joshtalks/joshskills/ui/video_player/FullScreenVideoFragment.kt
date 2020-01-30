package com.joshtalks.joshskills.ui.video_player

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentFullScreenVideoBinding

private const val ARG_VIDEO_URL = "video_url"

class FullScreenVideoFragment : DialogFragment() {
    private var videoUrl: String? = null

    private var onDismissListener: OnDismissListener? = null

    private lateinit var fullScreenVideoBinding: FragmentFullScreenVideoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoUrl = it.getString(ARG_VIDEO_URL)
        }
        activity?.let {
            onDismissListener = it as OnDismissListener
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
        try {
            fullScreenVideoBinding.pvPlayer.onStart()
        } catch (ex: Exception) {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fullScreenVideoBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_full_screen_video, container, false)
        fullScreenVideoBinding.lifecycleOwner = this
        fullScreenVideoBinding.handler = this
        return fullScreenVideoBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fullScreenVideoBinding.pvPlayer.setUrl(videoUrl)
        fullScreenVideoBinding.pvPlayer.downloadStreamPlay()
        fullScreenVideoBinding.pvPlayer.setActivity(activity)
        fullScreenVideoBinding.pvPlayer.fitToScreen()
        fullScreenVideoBinding.pvPlayer.supportFullScreen()
        setToolbar()
    }

    private fun setToolbar() {
        fullScreenVideoBinding.ivBack.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onStop() {
        super.onStop()
        try {

            fullScreenVideoBinding.pvPlayer.onStop()

        } catch (ex: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            fullScreenVideoBinding.pvPlayer.onPause()
        } catch (ex: Exception) {

        }

    }

    override fun onResume() {
        super.onResume()
        try {
            fullScreenVideoBinding.pvPlayer.onResume()
        } catch (ex: Exception) {

        }
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {
        @JvmStatic
        fun newInstance(paramVideoUrl: String) =
            FullScreenVideoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_URL, paramVideoUrl)
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDismissListener?.onDismiss()
        super.onDismiss(dialog)
    }

    interface OnDismissListener {
        fun onDismiss()
    }
}
