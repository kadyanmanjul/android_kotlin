package com.joshtalks.joshskills.voip

import android.app.Application
import com.coloros.ocs.base.common.Feature
import com.joshtalks.joshskills.base.model.ApiHeader
import com.joshtalks.joshskills.voip.log.JoshLog

// TODO: Must Refactor
val voipLog = JoshLog.getInstanceIfEnable(com.joshtalks.joshskills.voip.log.Feature.VOIP)

class Utils {
    companion object {
        var context : Application? = null
       var apiHeader : ApiHeader? = null
        var uuid : String? = null
        fun initUtils(application: Application ) {
            this.context = application
        }
    }
}