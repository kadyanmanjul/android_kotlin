package com.joshtalks.joshskills.core

import android.content.Context
import androidx.fragment.app.Fragment

open class CoreJoshFragment : Fragment() {

    var coreJoshActivity: CoreJoshActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is CoreJoshActivity) {
            this.coreJoshActivity = context
        }
    }
}