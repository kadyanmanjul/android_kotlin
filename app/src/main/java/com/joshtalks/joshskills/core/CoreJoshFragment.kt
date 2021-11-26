package com.joshtalks.joshskills.core

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.track.TrackFragment

open class CoreJoshFragment : TrackFragment() {

    var coreJoshActivity: CoreJoshActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is CoreJoshActivity) {
            this.coreJoshActivity = context
        }
    }

    fun showSnackBar(view: View, duration: Int, action_lable: String?) {
        if (PrefManager.getBoolValue(IS_PROFILE_FEATURE_ACTIVE)) {
            // SoundPoolManager.getInstance(AppObjectController.joshApplication).playSnackBarSound()
            PointSnackbar.make(view, duration, action_lable)?.show()
            playSnackbarSound(requireActivity())
        }
    }

    override fun getConversationId(): String? {
        return (requireActivity() as AppCompatActivity).intent.getStringExtra(CONVERSATION_ID)
    }
}
