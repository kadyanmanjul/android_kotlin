package com.joshtalks.joshskills.ui.video_player

import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
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
        try {
            activity?.let {
                onDismissListener = it as OnDismissListener
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        AppAnalytics.create(AnalyticsEvent.VIDEO_WATCH_ACTIVITY.NAME).push()

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

    override fun onDestroy() {
        super.onDestroy()
        AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME).addParam("name", javaClass.simpleName)
            .push()
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
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDismissListener?.onDismiss()
        super.onDismiss(dialog)
    }

    interface OnDismissListener {
        fun onDismiss()
    }
}
