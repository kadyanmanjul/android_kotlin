package com.joshtalks.joshskills.core.analytics
import com.joshtalks.joshskills.core.AppObjectController
import com.mixpanel.android.mpmetrics.MixpanelAPI
import org.json.JSONObject

class MixPanelTracker {
    val mixpanel: MixpanelAPI by lazy {
        MixpanelAPI.getInstance(
            AppObjectController.joshApplication,
            "4c574e3a5e6b933a0e55c88239f6e994"
        )
    }

    fun publishEvent(eventName:String,properties:JSONObject){
        mixpanel.track(eventName,properties)
        mixpanel.flush()
    }
}