package com.joshtalks.joshskills.common.core

import android.content.Context
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.joshtalks.joshskills.common.core.custom_ui.PointSnackbar

open class CoreJoshFragment : com.joshtalks.joshskills.common.track.TrackFragment() {

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
        try {
            if (isAdded && activity != null) {
                return (requireActivity() as AppCompatActivity).intent.getStringExtra(com.joshtalks.joshskills.common.track.CONVERSATION_ID)
            }
        }catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }
}
