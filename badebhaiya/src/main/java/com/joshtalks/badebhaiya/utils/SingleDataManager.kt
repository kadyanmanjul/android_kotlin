package com.joshtalks.badebhaiya.utils

//import com.joshtalks.badebhaiya.core.models.PendingPilotAction
import com.joshtalks.badebhaiya.core.models.PendingPilotEvent
import com.joshtalks.badebhaiya.core.models.PendingPilotEventData

object SingleDataManager {

    @Volatile
    var pendingPilotAction: PendingPilotEvent? = null

    @Volatile
    var pendingPilotEventData: PendingPilotEventData? = null

}
