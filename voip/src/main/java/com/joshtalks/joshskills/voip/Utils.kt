package com.joshtalks.joshskills.voip

import android.app.Application

// TODO: Must Refactor
class Utils {
    companion object {
        var context : Application? = null
        fun initUtils(application: Application ) {
            this.context = application
        }
    }
}