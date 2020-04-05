package com.joshtalks.joshskills.repository.service

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.NotificationObject
import com.joshtalks.joshskills.repository.server.engage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val VIDEO_TIME_LEAP = 6

object EngagementNetworkHelper {
    fun engageVideoApi(videoEngage: VideoEngage) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (videoEngage.watchTime == 0L) {
                    return@launch
                }
                videoEngage.gID = PrefManager.getStringValue(USER_UNIQUE_ID)
                videoEngage.mentorId = Mentor.getInstance().getId()
                AppObjectController.chatNetworkService.engageVideoApiV2(videoEngage)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

    @JvmStatic
    @Synchronized
    fun engageAudioApi(audioId: String, mediaEngageList: List<Graph>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var timeListen: Long = 0
                for (graph in mediaEngageList) {
                    timeListen += (graph.endTime - graph.startTime)
                }
                if (timeListen < 0) {
                    return@launch
                }
                val audioEngage = AudioEngage(mediaEngageList, audioId, timeListen)
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
                if (notificationId.isNullOrEmpty()) {
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

/* val graphList = IntArray(videoDuration / VIDEO_TIME_LEAP)
                //val graphList: ArrayList<Int> =arrayListOf((videoDuration / VIDEO_TIME_LEAP))
                for (graph in videoEngage.graph) {
                    var startUpdateListIndex = 0
                    if (graph.startTime != 0L) {
                        val diff = (graph.startTime.rem(VIDEO_TIME_LEAP)).toInt()
                        startUpdateListIndex = (graph.startTime / VIDEO_TIME_LEAP).toInt() + if (diff == 0) 0 else 1
                    }
                    val endUpdateListIndex = (graph.endTime / VIDEO_TIME_LEAP).toInt() - 1
                    for (i in startUpdateListIndex..endUpdateListIndex) {
                        graphList[i] = graphList[i] + 1
                    }
                }*/