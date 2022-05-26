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

    fun sendDeepWelcomeEvent(){
        sendEvent(
            Impression(
            Event.WELCOME,
            mapOf(
                "deeplink" to true
            )
        ))
    }

    fun sendWelcomeEvent(){
        sendEvent(
            Impression(
                Event.WELCOME,
                mapOf(
                    "deeplink" to false
                )
            ))
    }


    fun sendDeepLaunchEvent(){
        sendEvent(
            Impression(
                Event.LAUNCH,
                mapOf(
                   "deeplink" to true
                )
            )
        )
    }

    fun sendLaunchEvent(){
        sendEvent(
            Impression(
                Event.LAUNCH,
                mapOf(
                    "deeplink" to false
                )
            )
        )
    }
    fun sendTrueUsedEvent(){
        sendEvent(
            Impression(
                Event.TC_USED,
                mapOf()
            )
        )

    }

}

