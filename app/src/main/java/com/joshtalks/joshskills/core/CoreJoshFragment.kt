package com.joshtalks.joshskills.core

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar

open class CoreJoshFragment : Fragment() {

    var coreJoshActivity: CoreJoshActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is CoreJoshActivity) {
            this.coreJoshActivity = context
        }
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            //SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
            PointSnackbar.make(view, duration, action_lable)?.show()
            val mediaplayer: MediaPlayer = MediaPlayer.create(
                requireActivity(),
                R.raw.ting
            ) //You Can Put Your File Name Instead Of abc

            mediaplayer.setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
                override fun onCompletion(mediaPlayer: MediaPlayer) {
                    mediaPlayer.reset()
                    mediaPlayer.release()
                }
            })
            mediaplayer.start()
        }
    }
}
