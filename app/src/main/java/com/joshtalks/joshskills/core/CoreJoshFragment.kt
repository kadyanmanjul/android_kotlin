package com.joshtalks.joshskills.core

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
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
            PointSnackbar.make(view, duration, action_lable)?.show()
        }
    }
}
