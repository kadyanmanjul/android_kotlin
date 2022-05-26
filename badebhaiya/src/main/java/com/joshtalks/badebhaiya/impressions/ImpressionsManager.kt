package com.joshtalks.badebhaiya.impressions

import javax.inject.Inject

/**
    This class is responsible to send events.
*/

data class Impression(
    val event: String,
    val eventProperties: Map<Any, Any>,
)

class ImpressionsManager @Inject constructor(

) {

    fun sendEvent(impressionData: Impression){


    }

    fun sendWelcomeEvent(){
        sendEvent(
            Impression(
            Event.WELCOME,
            mapOf(
                "deeplink" to true
            )
        ))
    }

}

