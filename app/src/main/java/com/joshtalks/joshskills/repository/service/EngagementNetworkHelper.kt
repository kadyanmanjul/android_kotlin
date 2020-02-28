package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.eventbus.MediaEngageEventBus
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.server.engage.AudioEngage
import com.joshtalks.joshskills.repository.server.engage.ImageEngage
import com.joshtalks.joshskills.repository.server.engage.PdfEngage
import com.joshtalks.joshskills.repository.server.engage.VideoEngage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object EngagementNetworkHelper {


    fun engageVideoApi(videoEngage: VideoEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var timeListen: Long = 0
                for (graph in videoEngage.graph) {
                    timeListen += (graph.endTime - graph.startTime)
                }
                if (timeListen <= 0) {
                    return@launch
                }
                videoEngage.graph = mutableListOf()
                videoEngage.watchTime = timeListen
                AppObjectController.chatNetworkService.engageVideo(videoEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

    @JvmStatic
    @Synchronized
    fun engageAudioApi(mediaEngageEventBus: MediaEngageEventBus) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var timeListen: Long = 0
                for (graph in mediaEngageEventBus.list) {
                    timeListen += (graph.endTime - graph.startTime)
                }
                if (timeListen < 0) {
                    return@launch
                }
                val audioEngage = AudioEngage(emptyList(), mediaEngageEventBus.id, timeListen)
                AppObjectController.chatNetworkService.engageAudio(audioEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun engagePdfApi(pdfEngage: PdfEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.chatNetworkService.engagePdf(pdfEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun engageImageApi(imageEngage: ImageEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppObjectController.chatNetworkService.engageImage(imageEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun receivedNotification(notificationObject: NotificationObject) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppAnalytics.create(AnalyticsEvent.NOTIFICATION_RECEIVED.NAME)
                    .addParam("title", notificationObject.contentTitle)
                    .addParam("id", notificationObject.id)
                    .push()
                val data = mapOf("is_delivered" to "true")
                notificationObject.id?.let {
                    AppObjectController.chatNetworkService.engageNotificationAsync(it, data)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    fun clickNotification(notificationId: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (notificationId.isNullOrEmpty()){
                    return@launch
                }
                AppAnalytics.create(AnalyticsEvent.NOTIFICATION_CLICKED.NAME).push()
                val data = mapOf("is_clicked" to "true")
                AppObjectController.chatNetworkService.engageNotificationAsync(
                    notificationId,
                    data
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }


    }
}